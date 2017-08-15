package io.homemote.extension

import akka.http.scaladsl.model.StatusCodes.{GatewayTimeout, OK}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import io.homemote.api.CustomPathMatchers._
import io.homemote.extension.ShutterNode._
import io.homemote.model.Node
import io.homemote.serial.Protocol.{IMessage, OMessage, _}
import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs._

import scala.util.Success

object ShutterNode {
  object Command extends Enumeration { val off, up, down, sunshine, percent = Value }
  object Message { val codec: Codec[Message] = (("command" | enumerated(uint8L, Command)) :: ("percent" | int8L)).as[Message] }
  case class Message(command: Command.Value, percent: Int = -1)
}

class ShutterNode(companion: ExtensionCompanion) extends Extension with LazyLogging {

  override def firmware: String = "shutter-node"

  override def handleRF: (Node, IMessage) => Unit = (node, msg) => {
    val decoded = Message.codec.decodeValue(BitVector(msg.data)).require
    msg.ack() // Immediately ack!
    logger.info(s"$node received manual command ${decoded.command}")
  }

  override def handleHTTP: Route =
    (post & path(NodeId / s"(${Command.values.mkString("|")})".r)) { case (id, cmd) =>
      def toMessage(node: Node) = OMessage(node.networkId.id, requestAck = true,
        Message.codec.encode(Message(Command.withName(cmd))).require.toByteVector)
      onComplete(companion.getNode(id).map(toMessage).flatMap(companion.emit)) {
        case Success(Some(_: Ack)) => complete(OK -> s"Sent command $cmd to node $id")
        case _ => complete(GatewayTimeout -> s"Command $cmd to node $id timed out")
      }
    }

}
