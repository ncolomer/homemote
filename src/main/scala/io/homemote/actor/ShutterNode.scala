package io.homemote.actor

import akka.actor.{Actor, ActorLogging}
import io.homemote.actor.ShutterNode._
import io.homemote.model.NodeMessage
import scodec.bits.BitVector
import scodec.codecs._

object ShutterNode {
  object Command extends Enumeration { val Off, Up, Down, Sunshine, Percent = Value }
  object Message { val codec = (("command" | enumerated(uint8L, Command)) :: ("vbat" | uint8L)).as[Message] }
  case class Message(command: Command.Value, percent: Int)
}

class ShutterNode extends Actor with ActorLogging {

  context.system.eventStream.subscribe(self, classOf[NodeMessage])

  override def receive: Receive = {
    case NodeMessage(node, msg) if node.firmware.name == "shutter-node" =>
      val decoded = Message.codec.decodeValue(BitVector(msg.data)).require
      msg.ack() // Immediately ack!
      log.info("node {} received manual command {}", node.networkId, decoded.command)
  }

}