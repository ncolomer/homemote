package io.homemote.api

import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.server.{Directive, Directives, ExceptionHandler, Route}
import io.homemote.api.CustomPathMatchers._
import io.homemote.model.{JsonSerde, Node}
import io.homemote.repository.NodeRepository

import scala.language.implicitConversions

object NodeApi extends Directives with JsonSerde {

  val GetNodes: Directive[Unit] = get & path("nodes")
  val GetNode: Directive[Tuple1[Node.Id]] = get & path("nodes" / NodeId)
  val GetNodeTags: Directive[Tuple1[Node.Id]] = get & path("nodes" / NodeId / "tags")
  val PostNodeTag: Directive[(Node.Id, String)] = post & path("nodes" / NodeId / "tags" / Segment)
  val DeleteNodeTag: Directive[(Node.Id, String)] = delete & path("nodes" / NodeId / "tags" / Segment)

  val NotFoundHandler = ExceptionHandler {
    case _: NoSuchElementException => complete(NotFound -> s"Node was not found")
  }

  def route(repo: NodeRepository): Route =
    handleExceptions(NotFoundHandler) {
      GetNodes { onSuccess(repo.all())(list => complete(list)) } ~
      GetNode { id => onSuccess(repo.get(id))(node => complete(node)) } ~
      GetNodeTags { id => onSuccess(repo.get(id))(node => complete(node.tags)) } ~
      PostNodeTag { (id, tag) =>
        val future = repo.updateWith(id, node => node.copy(tags = node.tags + tag))
        onSuccess(future)(node => complete(node.tags)) } ~
      DeleteNodeTag { (id, tag) =>
        val future = repo.updateWith(id, node => node.copy(tags = node.tags - tag))
        onSuccess(future)(node => complete(node.tags)) }
    }

}