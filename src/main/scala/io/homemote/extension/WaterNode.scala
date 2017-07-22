package io.homemote.extension
import io.homemote.model.Node
import org.slf4j.{Logger, LoggerFactory}
import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs._

object WaterNode {
  object Message { val codec: Codec[Message] = (("pulses" | uint8L) :: ("vbat" | optional(bool(8), floatL))).as[Message] }
  case class Message(pulses: Int, vbat: Option[Float])
}

class WaterNode extends Extension { self: Companion =>
  import WaterNode._
  val log: Logger = LoggerFactory.getLogger(classOf[WaterNode])
  override def firmware: String = "water-node"
  override def handleRF: (Node, _root_.io.homemote.serial.Protocol.IMessage) => Unit = (node, msg) => {
    val decoded = Message.codec.decodeValue(BitVector(msg.data)).require
    msg.ack() // Immediately ack!
    log.info(s"$node just emitted ${decoded.pulses} pulses" +
      s"${decoded.vbat.map(v => s" (${v}V)").getOrElse("")}")
  }
}
