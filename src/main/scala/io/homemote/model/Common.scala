package io.homemote.model

import scala.util.matching.Regex

object Common {

  /** Network ID */
  type Nid = Int
  /** Unique ID */
  type Uid = String
  /** (polymorphic) Node ID type */
  type NodeId = Either[Nid, Uid]

  val NidPattern: Regex = """(\d+)""".r.anchored
  val UidPattern = """(\d{2}:\d{2}:\d{2}:\d{2}:\d{2}:\d{2}:\d{2}:\d{2})""".r.anchored

}
