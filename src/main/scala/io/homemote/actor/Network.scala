package io.homemote.actor

import akka.actor.{Actor, ActorLogging}
import io.homemote.actor.Network._
import io.homemote.model.{Node, NodeMessage}
import io.homemote.repository.NodeRepository
import io.homemote.serial.GatewayDriver
import io.homemote.serial.Protocol.IMessage
import org.joda.time.DateTime
import scodec._
import scodec.bits.{ByteVector, _}
import scodec.codecs._

import scala.collection.mutable

object Network {

  case class IDRequest(uid: ByteVector, firmware: String, version: String) {
    def uniqueId = uid.toHex.grouped(2).mkString(":")
    override def toString = s"IDRequest(uid: $uniqueId, firmware: $firmware, version: $version)"
  }

  object IDRequest {
    val codec = (("uid" | bytes(8)) :: ("firmware" | cstring) :: ("version" | cstring)).as[IDRequest]
  }

}

class Network(Nodes: NodeRepository) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  def isIDRequest(msg: IMessage): Boolean = msg.senderId == 0 && msg.targetId == 255

  val GatewayId = context.system.settings.config.getInt("gateway.node-id")

  Nodes.getUsedNid.onSuccess({ case used =>
    val nids = (1 to 254).toSet -- used - GatewayId
    val queue = mutable.Queue(nids.toSeq: _*)
    context.become(listen(queue))
  })

  override def receive: Receive = Actor.emptyBehavior
  def listen(free: mutable.Queue[Int]): Receive = {
    case GatewayDriver.MessageReceived(msg) if isIDRequest(msg) =>
      val req = IDRequest.codec.decodeValue(BitVector(msg.data)).require
      Nodes.get(Right(req.uniqueId)).flatMap {
        case Some(node) =>
          log.debug("Node {} requested NID: returning {}", node.uniqueId, node.networkId)
          Nodes.upsert(node.copy(lastSeen = DateTime.now))
        case None =>
          val networkId = free.dequeue
          log.debug("Node {} (new) requested NID: returning {}", req.uniqueId, networkId)
          Nodes.upsert(Node.firstSeen(req.uniqueId, networkId, req.firmware, req.version))
      }.onSuccess { case node => msg.ack(Array(node.networkId.toByte)) }
    case GatewayDriver.MessageReceived(msg) =>
      Nodes.get(Left(msg.senderId)).onSuccess {
        case Some(node) =>
          Nodes.upsert(node.copy(lastSeen = DateTime.now))
          context.parent ! NodeMessage(node, msg)
        case None => log.warning("Node is unknown for message {}", msg) // TODO reset the node!
      }
  }

}