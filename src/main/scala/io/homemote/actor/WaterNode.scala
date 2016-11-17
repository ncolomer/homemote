package io.homemote.actor

import akka.actor.{Actor, ActorLogging}
import io.homemote.actor.WaterNode.Message
import io.homemote.model.NodeMessage
import scodec.bits.BitVector
import scodec.codecs._

object WaterNode {
  object Message { val codec = (("pulses" | uint8L) :: ("vbat" | optional(bool(8), floatL))).as[Message] }
  case class Message(pulses: Int, vbat: Option[Float])
}

class WaterNode extends Actor with ActorLogging {

  context.system.eventStream.subscribe(self, classOf[NodeMessage])

  override def receive: Receive = {
    case NodeMessage(node, msg) if node.firmware.name == "water-node" =>
      val decoded = Message.codec.decodeValue(BitVector(msg.data)).require
      msg.ack() // Immediately ack!
      log.info("node {} emitted {} pulses", node.networkId, decoded.pulses)
      decoded.vbat.foreach(vbat => log.info("node {}'s vbat is {}", node.networkId, vbat))
  }

}
