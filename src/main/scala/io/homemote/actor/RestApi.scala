package io.homemote.actor

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes.ServiceUnavailable
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import io.homemote.actor.RestApi.PushRoute
import io.homemote.api._

import scala.concurrent.{ExecutionContextExecutor, Future}

object RestApi {
  case class PushRoute(route: Route)
}

class RestApi extends Actor with ActorLogging with Directives {

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  implicit val system: ActorSystem = context.system

  var apiRoute: Route = complete(ServiceUnavailable -> "Not available")

  val handler: Route =
    pathPrefix("api" / "v1") {
      (pathEnd & get & complete("Homemote API v1")) ~ apiRoute
    } ~ ServiceApi.route

  implicit val materializer = ActorMaterializer()
  val binding: Future[ServerBinding] = Http().bindAndHandle(handler, "::0", 8080)

  override def receive: Receive = { case PushRoute(route) => apiRoute = route ~ apiRoute }

  override def postStop(): Unit = binding.flatMap(_.unbind)

}
