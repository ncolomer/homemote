package io.homemote.extension

import io.homemote.model.Node
import org.slf4j.{Logger, LoggerFactory}
import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs._

object PingNode {
  object Message { val codec: Codec[Message] = (("success" | uint32L) :: ("attempt" | uint32L)).as[Message] }
  case class Message(success: Long, attempt: Long)
}

class PingNode extends Extension { self: Companion =>
  import PingNode._
  val log: Logger = LoggerFactory.getLogger(classOf[PingNode])
  override def firmware: String = "ping-node"
  override def handleRF: (Node, _root_.io.homemote.serial.Protocol.IMessage) => Unit = (node, msg) => {
    val decoded = Message.codec.decodeValue(BitVector(msg.data)).require
    msg.ack() // Immediately ack!
    log.info(s"$node succeed ${decoded.success}/${decoded.attempt} (RSSI ${msg.rssi})")
  }
}
