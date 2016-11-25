package io.homemote.model

import scala.util.Try

object NetworkID {
  object Match { def unapply(nid: String) = Try(Some(NetworkID(nid.toInt))).getOrElse(None) }
}

case class NetworkID(id: Int) {
  assert(0 <= id && id <= 255, "nid invalid")
}


