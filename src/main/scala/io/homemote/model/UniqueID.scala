package io.homemote.model

import scodec.bits.ByteVector

import scala.util.Try

object UniqueID {
  object Match { def unapply(uid: String) = Try(Some(UniqueID(uid))).getOrElse(None) }
  val Pattern = List.fill(8)("\\p{Alnum}{2}").mkString("(", ":", ")").r
  def apply(bytes: ByteVector): UniqueID = {
    assert(bytes.size == 8, "uid should contain exactly 8 bytes")
    UniqueID(bytes.toHex.grouped(2).mkString(":"))
  }
}

case class UniqueID(id: String) {
  assert(UniqueID.Pattern.findFirstMatchIn(id).isDefined, "not a uid string")
  val bytes = ByteVector.fromValidHex(id.sliding(2, 3).mkString)
}
