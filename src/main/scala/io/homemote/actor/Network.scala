package io.homemote.actor

import akka.actor.{Actor, ActorLogging}
import io.homemote.actor.Network._
import io.homemote.model.{NetworkID, Node, NodeMessage, UniqueID}
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
    val uniqueId = UniqueID(uid)
    override def toString = s"IDRequest(uid: ${uniqueId.id}, firmware: $firmware, version: $version)"
  }

  object IDRequest {
    val codec = (("uid" | bytes(8)) :: ("firmware" | cstring) :: ("version" | cstring)).as[IDRequest]
  }

}

class Network(Nodes: NodeRepository) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  val GatewayId = context.system.settings.config.getInt("gateway.node-id")
  def broadcast(msg: IMessage): Boolean = msg.senderId == 0 && msg.targetId == 255

  Nodes.getUsedNid.onSuccess { case used =>
    val nids = (1 to 254).toSet -- used - GatewayId
    val queue = mutable.Queue(nids.toSeq: _*)
    context.become(listen(queue))
  }

  override def receive: Receive = Actor.emptyBehavior
  def listen(free: mutable.Queue[Int]): Receive = {
    case GatewayDriver.MessageReceived(msg) if broadcast(msg) =>
      val req = IDRequest.codec.decodeValue(BitVector(msg.data)).require
      Nodes.get(req.uniqueId) map {
        case Some(node) =>
          msg.ack(Array(node.networkId.id.toByte))
          log.debug("Node {} joined network", node)
          node.copy(lastSeen = DateTime.now)
        case None =>
          val node = Node.firstSeen(req.uniqueId, NetworkID(free.dequeue), req.firmware, req.version)
          msg.ack(Array(node.networkId.id.toByte))
          log.info("New node {} discovered", node)
          node
      } map Nodes.upsert recover { case _ => msg.noAck() }

    case GatewayDriver.MessageReceived(msg) =>
      Nodes.get(NetworkID(msg.senderId)).onSuccess {
        case Some(node) => // Forward message
          Nodes.upsert(node.copy(lastSeen = DateTime.now))
          context.parent ! NodeMessage(node, msg)
        case None => // FIXME reset the node!
          log.warning("Node is unknown for message {}", msg)
      }

    // TODO Handle battery update (touch update + battery update)
    // TODO Handle heartbeat (touch update)
    // TODO Handle set/get state values (return in ACK)
    // TODO Handle clock request

  }

}