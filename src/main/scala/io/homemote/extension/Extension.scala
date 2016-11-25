package io.homemote.extension

import akka.http.scaladsl.server.Route
import io.homemote.model.Node
import io.homemote.serial.Protocol.{IMessage, OMessage}
import org.slf4j.{Logger, LoggerFactory}

trait Extension {

  val log = LoggerFactory.getLogger(getClass)

  def emit(msg: OMessage): Unit = ???

  def measure[T <: Number](node: Node, key: String, value: T): Unit = ???

  def setState[T](node: Node, key: String, value: T): Unit = ???
  def getState[T](node: Node, key: String): T = ???

  def firmware: String

  def handleHTTP: List[Node] => Route
  def handleRF: (IMessage, Node) => Unit

}
