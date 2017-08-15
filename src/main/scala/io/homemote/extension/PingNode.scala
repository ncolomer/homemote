package io.homemote.extension

import com.typesafe.scalalogging.LazyLogging
import io.homemote.extension.PingNode._
import io.homemote.model.Node
import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs._

object PingNode {
  object Message { val codec: Codec[Message] = (("success" | uint32L) :: ("attempt" | uint32L)).as[Message] }
  case class Message(success: Long, attempt: Long)
}

class PingNode(companion: ExtensionCompanion) extends Extension with LazyLogging {

  override def firmware: String = "ping-node"

  override def handleRF: (Node, _root_.io.homemote.serial.Protocol.IMessage) => Unit = (node, msg) => {
    val decoded = Message.codec.decodeValue(BitVector(msg.data)).require
    msg.ack() // Immediately ack!
    logger.info(s"$node succeed ${decoded.success}/${decoded.attempt} (RSSI ${msg.rssi})")
  }

}
