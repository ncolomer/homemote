package io.homemote.extension

import akka.http.scaladsl.model.StatusCodes.{OK, GatewayTimeout}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.homemote.model.Node
import io.homemote.serial.Protocol.{IMessage, OMessage}
import org.slf4j.{Logger, LoggerFactory}
import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs._
import ShutterNode._
import io.homemote.api.CustomPathMatchers._
import io.homemote.serial.Protocol._

import scala.util.Success

object ShutterNode {
  object Command extends Enumeration { val off, up, down, sunshine, percent = Value }
  object Message { val codec: Codec[Message] = (("command" | enumerated(uint8L, Command)) :: ("percent" | int8L)).as[Message] }
  case class Message(command: Command.Value, percent: Int = -1)
}

class ShutterNode extends Extension { self: Companion =>

  import scala.concurrent.ExecutionContext.Implicits.global

  val log: Logger = LoggerFactory.getLogger(classOf[ShutterNode])

  override def firmware: String = "shutter-node"

  override def handleRF: (Node, IMessage) => Unit = (node, msg) => {
    val decoded = Message.codec.decodeValue(BitVector(msg.data)).require
    msg.ack() // Immediately ack!
    log.info(s"$node received manual command ${decoded.command}")
  }

  override def handleHTTP: Route =
    (post & path(NodeId / s"(${Command.values.mkString("|")})".r)) { case (id, cmd) =>
      def toMessage(node: Node) = OMessage(node.networkId.id, requestAck = true,
        Message.codec.encode(Message(Command.withName(cmd))).require.toByteVector)
      onComplete(getNode(id).map(toMessage).flatMap(emit)) {
        case Success(Some(_: Ack)) => complete(OK -> s"Sent command $cmd to node $id")
        case _ => complete(GatewayTimeout -> s"Command $cmd to node $id timed out")
      }
    }

}
