package io.homemote.extension
import com.typesafe.scalalogging.LazyLogging
import io.homemote.extension.WaterNode._
import io.homemote.model.Node
import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs._

object WaterNode {
  object Message { val codec: Codec[Message] = (("pulses" | uint8L) :: ("vbat" | optional(bool(8), floatL))).as[Message] }
  case class Message(pulses: Int, vbat: Option[Float])
}

class WaterNode(companion: ExtensionCompanion) extends Extension with LazyLogging {

  override def firmware: String = "water-node"

  override def handleRF: (Node, _root_.io.homemote.serial.Protocol.IMessage) => Unit = (node, msg) => {
    val decoded = Message.codec.decodeValue(BitVector(msg.data)).require
    msg.ack() // Immediately ack!
    logger.info(s"$node just emitted ${decoded.pulses} pulses")
  }

}
