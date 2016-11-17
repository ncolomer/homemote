package io.homemote.utils

/**
  * from(int):to(int):type(int):size(int):payload(bytes[])
  * ack
  * mesg
  * prog
  * ping
  * rset
  * conf
  */

object BinaryHelpers {
  implicit class ByteArray_BinaryImplicits(bytes: Array[Byte]) {
    def toHexString: String = bytes.map("%02X" format _).mkString
  }
  implicit class String_BinaryImplicits(string: String) {
    /** Remove all non-hex case-insensitive chars */
    def toByteArray: Array[Byte] = string.replaceAll("(^0x|[^0-9A-Fa-f])", "")
      .grouped(2).toArray.map(Integer.parseInt(_, 16).toByte)
  }
}
