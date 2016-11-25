package io.homemote.api

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatcher._
import akka.http.scaladsl.server.Route
import io.homemote.BuildInfo

trait ServiceApi {

  val serviceApi: Route =
    (get & path("ping") & complete("pong")) ~
    (get & path("version")) {
      implicit val m = Marshaller.withFixedContentType(`application/json`) { json: String =>
        HttpEntity(`application/json`, json)
      }
      complete(BuildInfo.toJson)
    }

}
