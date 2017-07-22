package io.homemote.extension

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes.NotImplemented
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.homemote.model.Node
import io.homemote.serial.GatewayDriver
import io.homemote.serial.Protocol.{Ack, IMessage, OMessage}

import scala.concurrent.Future


trait Extension {
  /* implemented by user */
  def firmware: String
  def handleRF: (Node, IMessage) => Unit
  /* optionnally implemented by user */
  def handleHTTP: Route = complete(NotImplemented -> s"$firmware extension API is not implemented")
}

trait Companion { self: ContactPoint =>
  /* Fetch a Node entity from its id */
  def getNode(id: Node.Id): Future[Node]
  /* Emit a message and return a `Future` that completes once the message is sent (containing ack result if `requestAck` was `true`) */
  def emit(msg: OMessage): Future[Option[Ack]] = {
    actor.tell(GatewayDriver.EmitMessage(msg), null)
    msg.future
  }
  def measure[T <: Number](node: Node, key: String, value: T): Unit = ???
  def setState[T](node: Node, key: String, value: T): Unit = ???
  def getState[T](node: Node, key: String): T = ???
}

trait ContactPoint {
  def actor: ActorRef
}