package io.homemote.actor

import java.net.{InetAddress, NetworkInterface}

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import io.homemote.api._
import io.homemote.repository.{GroupRepository, NodeRepository}


class HttpApi(val Node: NodeRepository,
              val Group: GroupRepository) extends Actor
  with ServiceApi
  with NodeApi {

  val route: Route = nodeApi ~ serviceApi
  implicit val system = context.system
  implicit val materializer = ActorMaterializer()

  val binding = Http().bindAndHandle(route, "::0", 8080)
  override def receive: Receive = Actor.emptyBehavior

  implicit val executionContext = context.dispatcher
  override def postStop() = binding.flatMap(_.unbind)

}
