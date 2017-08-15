package io.homemote.actor

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.{Directives, RequestContext, Route, RouteResult}
import akka.stream.ActorMaterializer
import io.homemote.actor.HttpApi.InstallRoute

import scala.concurrent.Future

object HttpApi {
  case class InstallRoute(route: Route)
}

class HttpApi extends Actor with ActorLogging with Directives {

  val api = new AtomicReference[Route](reject)

  implicit val system: ActorSystem = context.system
  implicit val materializer = ActorMaterializer()
  val binding: Future[ServerBinding] = Http().bindAndHandle(
    (req: RequestContext) => api.get().apply(req), "::0", 8080)

  override def receive: Receive = {
    case InstallRoute(route) =>
      api.updateAndGet(new UnaryOperator[Route] {
        override def apply(api: Route): Route = route ~ api
      })
  }

  override def postStop(): Unit = binding.flatMap(_.unbind)(context.dispatcher)

}
