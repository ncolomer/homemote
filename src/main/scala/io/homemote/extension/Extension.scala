package io.homemote.extension

import java.util.concurrent.Executors

import akka.http.scaladsl.model.StatusCodes.NotImplemented
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.homemote.model.Node
import io.homemote.serial.Protocol.IMessage

import scala.concurrent.ExecutionContext


trait Extension {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newWorkStealingPool())

  /* implemented by user */
  def firmware: String

  /* optionally implemented by user */
  def handleRF: (Node, IMessage) => Unit = (_, _) => ()

  /* optionally implemented by user */
  def handleHTTP: Route = complete(NotImplemented -> s"$firmware extension API is not implemented")

}

