package io.homemote.serial

import java.util.UUID
import java.util.concurrent.TimeoutException

import scodec._
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._

import scala.concurrent.{Future, Promise}

object Protocol extends Protocol

trait Protocol {

  private val codec =
    discriminated[Packet].by(uint8L)
      .typecase(0, Greeting.codec)
      .typecase(1, Config.codec)
      .typecase(2, IMessage.codec)
      .typecase(3, OMessage.codec)
      .typecase(4, Ack.codec)
      .typecase(5, NoAck.codec)

  def decode[T <: Packet](bytes: Array[Byte]): T = codec.decodeValue(BitVector(bytes)).require.asInstanceOf[T]

  implicit class ByteArray_Implicit(bytes: Array[Byte]) {
    def decodePacket[T <: Packet]: T = codec.decodeValue(BitVector(bytes)).require.asInstanceOf[T]
  }

  implicit class Packet_Implicit(packet: Packet) {
    def encode: Array[Byte] = codec.encode(packet).require.toByteArray
  }

  sealed trait Packet

  /** Representation of a greeting frame.
    *
    * @param uidBytes the gateway's unique id
    * @param version the gateway firmware version
    */
  case class Greeting(uidBytes: ByteVector, version: String) extends Packet {
    def uniqueId: String = uidBytes.toHex.grouped(2).mkString(":")
  }
  object Greeting {
    val codec: Codec[Greeting] = (
        ("uidBytes" | bytes(8)) ::
        ("version" | cstring)
      ).as[Greeting]
  }

  /** Representation of a configuration frame.
    *
    * @param networkId  the rf network id
    * @param gatewayId  the gateway network id
    * @param encryptKey the 16-char encrypt key (128bit AES encryption)
    */
  case class Config(networkId: Int, gatewayId: Int, encryptKey: String) extends Packet {
    encryptKey.ensuring(_.length == 16, "encrypt key must be 16 bytes fixed length")
  }
  object Config {
    val codec: Codec[Config] = (
        ("networkId" | uint8L) ::
        ("gatewayId" | uint8L) ::
        ("encryptKey" | cstring)
      ).as[Config]
  }

  trait Message extends Packet {
    /** Unique message identifier */
    val uuid: UUID = UUID.randomUUID()
    /** Message creation timestamp in millis */
    val timestamp: Long = System.currentTimeMillis()
  }

  trait Deliverable[B <: Deliverable[B]] { self: B =>
    private val promise: Promise[Option[Ack]] = Promise()
    /** Complete and acknowledge this message without data*/
    def ack(): Unit = promise.success(Some(Ack.empty))
    /** Complete and acknowledge this message with data */
    def ack(data: Array[Byte]): Unit = promise.success(Some(Ack.withData(data)))
    /** Complete but do not acknowledge this message.
      * May be ''normal'' or ''not expected'' depending on message's `requestAck` value. */
    def noAck(): Unit = promise.failure(new TimeoutException())
    /** Mark message as delivered (complete sur future with no ack). */
    def delivered(): Unit = promise.success(None)
    /** Message delivery future */
    val future: Future[Option[Ack]] = promise.future
  }

  /** Representation of a message that transited over the air from
    * any node to the gateway, using the HopeRF RFM69 transceiver protocol.
    *
    * @param senderId   the sender node id
    * @param targetId   the target node id
    * @param data       any raw payload
    * @param rssi       Received Signal Strength Indicator (in dB)
    * @param requestAck whether the message require an acks
    */
  case class IMessage(senderId: Int, targetId: Int, requestAck: Boolean, rssi: Int, data: ByteVector)
    extends Message with Deliverable[IMessage] {
    /** Send an answer to the node sending this message */
    def answer(data: Array[Byte], requestAck: Boolean = true): OMessage =
      new OMessage(senderId, requestAck, ByteVector(data))
  }
  object IMessage {
    val codec: Codec[IMessage] = (
        ("senderId" | uint8L) ::
        ("targetId" | uint8L) ::
        ignore(7) :: ("requestAck" | bool) ::
        ("rssi" | int16L) ::
        ("data" | variableSizeBytes(uint8L, bytes))
      ).as[IMessage]
  }

  /** Representation of a message that should be sent over the air
    * from the gateway to any (or all) node, using the HopeRF RFM69 transceiver protocol.
    *
    * @param targetId   the target node id
    * @param data       any raw payload
    * @param requestAck whether the message require an acks
    */
  case class OMessage(targetId: Int, requestAck: Boolean, data: ByteVector) extends Message with Deliverable[OMessage]
  object OMessage {
    val codec: Codec[OMessage] = (
        ("targetId" | uint8L) ::
        ignore(7) :: ("requestAck" | bool) ::
        ("data" | variableSizeBytes(uint8L, bytes))
      ).as[OMessage]
  }

  /** This is the object representation of an ack.
    *
    * @param data an optional payload
    */
  case class Ack(data: ByteVector) extends Packet {
    override def toString: String = if (data.isEmpty) "Ack" else s"Ack(data: $data)"
  }
  object Ack {
    def empty: Ack = Ack(ByteVector.empty)
    def withData(bytes: Array[Byte]) = Ack(ByteVector(bytes))
    val codec: Codec[Ack] = ("data" | variableSizeBytes(uint8L, bytes)).as[Ack]
  }

  /** This is the object representation of a noack. */
  case class NoAck() extends Packet
  object NoAck {
    val codec: Codec[NoAck] = provide(NoAck())
  }

}
