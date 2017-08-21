package io.homemote.extension

import akka.actor.ActorRef
import io.homemote.model.Node
import io.homemote.repository.NodeRepository
import io.homemote.serial.GatewayDriver
import io.homemote.serial.Protocol.{Ack, OMessage}
import scaldi.{Injectable, Injector}

import scala.concurrent.Future


class ExtensionCompanion(implicit inj: Injector) extends Injectable {

  /* Fetch a Node entity from its id */
  def getNode(id: Node.Id): Future[Option[Node]] = inject[NodeRepository].get(id)

  /* Emit a message and return a `Future` that completes once the message is sent (containing ack result if `requestAck` was `true`) */
  def emit(msg: OMessage): Future[Option[Ack]] = {
    inject[ActorRef]('router).tell(GatewayDriver.EmitMessage(msg), null)
    msg.future
  }

  def measure[T <: Number](node: Node, key: String, value: T): Unit = ???

  def setState[T](node: Node, key: String, value: T): Unit = ???

  def getState[T](node: Node, key: String): T = ???

}
