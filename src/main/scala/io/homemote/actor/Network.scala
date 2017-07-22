package io.homemote.actor

import java.time.Instant

import akka.actor.{Actor, ActorLogging}
import io.homemote.actor.Network._
import io.homemote.model._
import io.homemote.repository.NodeRepository
import io.homemote.serial.GatewayDriver
import io.homemote.serial.Protocol.IMessage
import scodec._
import scodec.bits.{ByteVector, _}
import scodec.codecs._

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor

object Network {

  case class NodeMessage(node: Node, msg: IMessage)

  case class IDRequest(uid: ByteVector, firmware: String, version: String) {
    val uniqueId = UniqueID(uid)
    override def toString = s"IDRequest(uid: ${uniqueId.id}, firmware: $firmware, version: $version)"
  }

  object IDRequest {
    val codec: Codec[IDRequest] = (("uid" | bytes(8)) :: ("firmware" | cstring) :: ("version" | cstring)).as[IDRequest]
  }

}

class Network(Nodes: NodeRepository) extends Actor with ActorLogging {

  implicit val ec: ExecutionContextExecutor = context.dispatcher

  val GatewayId: Int = context.system.settings.config.getInt("gateway.node-id")
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
        old =>
          val node = old.copy(lastSeen = Instant.now, firmware = Firmware(req.firmware, req.version))
          msg.ack(Array(node.networkId.id.toByte))
          log.debug("Node {} joined network", node)
          node
      } recover {
        case _ =>
          val node = Node(req.uniqueId, NetworkID(free.dequeue), req.firmware, req.version)
          msg.ack(Array(node.networkId.id.toByte))
          log.info("New node {} discovered", node)
          node
      } map Nodes.upsert recover { case _ => msg.noAck() }

    case GatewayDriver.MessageReceived(msg) =>
      Nodes.get(NetworkID(msg.senderId)) map {
        node => // Forward message
          Nodes.upsert(node.copy(lastSeen = Instant.now))
          context.parent ! NodeMessage(node, msg)
      } recover {
        case _ => // FIXME reset the node!
          log.warning("Node is unknown for message {}", msg)
          msg.noAck()
      }

  }

}
