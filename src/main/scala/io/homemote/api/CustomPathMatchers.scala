package io.homemote.api

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.PathMatcher.{Matched, Matching, Unmatched}
import akka.http.scaladsl.server.PathMatcher1
import io.homemote.model.{NetworkID, Node, UniqueID}

object CustomPathMatchers {

  /** Expects a node id (either network or unique id) */
  val NodeId = new PathMatcher1[Node.Id] {
    def apply(path: Path): Matching[Tuple1[Node.Id]] = path match {
      case Path.Segment(segment, tail) => segment match {
        case NetworkID.Match(nid) => Matched(tail, Tuple1(Left(nid)))
        case UniqueID.Match(uid) => Matched(tail, Tuple1(Right(uid)))
        case _ => Unmatched
      }
      case _ => Unmatched
    }
  }

}
