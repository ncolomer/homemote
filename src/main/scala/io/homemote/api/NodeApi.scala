package io.homemote.api

import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}
import akka.http.scaladsl.server.{Directives, PathMatcher1, Route}
import io.homemote.model.Common._
import io.homemote.model.JsonSerde
import io.homemote.repository.NodeRepository

import scala.util.{Failure, Success}

trait NodeApi extends Directives with JsonSerde {

  val Nodes: NodeRepository

  val NodeId = new PathMatcher1[Either[Nid, Uid]] {
    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) => segment match {
        case NidPattern(nid) => Matched(tail, Tuple1(Left(nid.toInt)))
        case UidPattern(uid) => Matched(tail, Tuple1(Right(uid)))
        case _ => Unmatched
      } case _ => Unmatched}}

  val GetNodes      = get & path("nodes")
  val GetNode       = get & path("node" / NodeId)
  val GetNodeTags   = get & path("node" / NodeId / "tags")
  val PostNodeTag   = post & path("node" / NodeId / "tag" / Segment)
  val DeleteNodeTag = delete & path("node" / NodeId / "tag" / Segment)

  val nodeApi: Route =
    GetNodes { onComplete(Nodes.all()) {
      case Success(list) => complete(list)
      case Failure(t) => failWith(t)
    }} ~
    GetNode { id => onComplete(Nodes.get(id)) {
      case Success(Some(node)) => complete(node)
      case Success(None) => complete(NotFound -> s"Node ${id.fold(_.toString, _.toString)} was not found")
      case Failure(t) => failWith(t)
    }} ~
    GetNodeTags { id => onComplete(Nodes.get(id)) {
      case Success(Some(node)) => complete(node.tags)
      case Success(None) => complete(NotFound -> s"Node ${id.fold(_.toString, _.toString)} was not found")
      case Failure(t) => failWith(t)
    }} ~
    PostNodeTag { (id, tag) => onComplete(Nodes.updateWith(id, node => node.copy(tags = node.tags + tag))) {
      case Success(node) => complete(node.tags)
      case Failure(t) => failWith(t)
    }} ~
    DeleteNodeTag { (id, tag) => onComplete(Nodes.updateWith(id, node => node.copy(tags = node.tags - tag))) {
      case Success(node) => complete(node.tags)
      case Failure(t) => failWith(t)
    }}

}