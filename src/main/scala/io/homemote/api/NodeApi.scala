package io.homemote.api

import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.server.{Directive, Directives, Route}
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

  def route(repo: NodeRepository): Route =
    GetNodes(onSuccess(repo.all())(list => complete(list))) ~
    GetNode(id => onSuccess(repo.get(id)) {
      case Some(node) => complete(node)
      case None => complete(NotFound -> s"Node $id was not found")
    }) ~
    GetNodeTags(id => onSuccess(repo.get(id)) {
      case Some(node) => complete(node.tags)
      case None => complete(NotFound -> s"Node $id was not found")
    }) ~
    PostNodeTag { (id, tag) =>
      onSuccess(repo.updateWith(id, node => node.copy(tags = node.tags + tag))) {
        case Some(node) => complete(node.tags)
        case None => complete(NotFound -> s"Node $id was not found")
      }
    } ~
    DeleteNodeTag { (id, tag) =>
      onSuccess(repo.updateWith(id, node => node.copy(tags = node.tags - tag))) {
        case Some(node) => complete(node.tags)
        case None => complete(NotFound -> s"Node $id was not found")
      }
    }

}
