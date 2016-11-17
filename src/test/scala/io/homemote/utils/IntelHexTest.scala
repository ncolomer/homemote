package io.homemote.utils

//import io.homemote.utils.IntelHex.{ParseException, Record}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

import scala.io.Source

class IntelHexTest extends FlatSpec {

  behavior of "IntelHex"

// :10010000 214601360121470136007EFE09D21901 40
// :10011000 2146017E17C20001FF5F160021480119 28
// :10012000 194E79234623965778239EDA3F01B2CA A7
// :10013000 3F0156702B5E712B722B732146013421 C7
// :00000001                                  FF

//  it should "parseBytes" in {
//    // Given
//    val sample =
//      """:10010000214601360121470136007EFE09D2190140
//        |:100110002146017E17C20001FF5F16002148011928
//        |:10012000194E79234623965778239EDA3F01B2CAA7
//        |:100130003F0156702B5E712B722B732146013421C7
//        |:00000001FF
//      """.stripMargin
//    // When
//    val actual = IntelHex.parseBytes(Source.fromString(sample))
//    // Then
//    actual should be a 'Success
//    //actual.get should equal
//    val arr = Array(
//      0x21, 0x46, 0x01, 0x36, 0x01, 0x21, 0x47, 0x01, 0x36, 0x00, 0x7E, 0xFE, 0x09, 0xD2, 0x19, 0x01,
//      0x21, 0x46, 0x01, 0x7E, 0x17, 0xC2, 0x00, 0x01, 0xFF, 0x5F, 0x16, 0x00, 0x21, 0x48, 0x01, 0x19,
//      0x19, 0x4E, 0x79, 0x23, 0x46, 0x23, 0x96, 0x57, 0x78, 0x23, 0x9E, 0xDA, 0x3F, 0x01, 0xB2, 0xCA,
//      0x3F, 0x01, 0x56, 0x70, 0x2B, 0x5E, 0x71, 0x2B, 0x72, 0x2B, 0x73, 0x21, 0x46, 0x01, 0x34, 0x21
//    )
//  }
//
//  it should "parseRecords" in {
//    // Given
//    val sample =
//      """:10010000214601360121470136007EFE09D2190140
//        |:100110002146017E17C20001FF5F16002148011928
//        |:10012000194E79234623965778239EDA3F01B2CAA7
//        |:100130003F0156702B5E712B722B732146013421C7
//        |:00000001FF
//      """.stripMargin
//    // When
//    val actual = IntelHex.parseRecords(Source.fromString(sample))
//    // Then
//    actual should be a 'Success
//    //actual.get should equal
//    val arr = Array(
//      Record(16,  0, 0, Array(0x21, 0x46, 0x01, 0x36, 0x01, 0x21, 0x47, 0x01, 0x36, 0x00, 0x7E, 0xFE, 0x09, 0xD2, 0x19, 0x01).map(_.toByte), 0x40),
//      Record(16, 16, 0, Array(0x21, 0x46, 0x01, 0x7E, 0x17, 0xC2, 0x00, 0x01, 0xFF, 0x5F, 0x16, 0x00, 0x21, 0x48, 0x01, 0x19).map(_.toByte), 0x28),
//      Record(16, 32, 0, Array(0x19, 0x4E, 0x79, 0x23, 0x46, 0x23, 0x96, 0x57, 0x78, 0x23, 0x9E, 0xDA, 0x3F, 0x01, 0xB2, 0xCA).map(_.toByte), 0xA7),
//      Record(16, 48, 0, Array(0x3F, 0x01, 0x56, 0x70, 0x2B, 0x5E, 0x71, 0x2B, 0x72, 0x2B, 0x73, 0x21, 0x46, 0x01, 0x34, 0x21).map(_.toByte), 0xC7),
//      Record( 0,  0, 1, Array.empty[Byte], 0xFF)
//    )
//  }
//
//  it should "throw ParseException with wrong format" in {
//    // Given
//    val sample = ":10010000 214601360121470136007EFE09D21901 40"
//    // When
//    val actual = IntelHex.parseBytes(Source.fromString(sample))
//    // Then
//    actual should be a 'Failure
//    val exception = intercept[ParseException] { actual.get }
//    exception.getMessage should include("wrong format")
//  }
//
//  it should "throw ParseException with unrecognized record type" in {
//    // Given
//    val sample = ":10010006214601360121470136007EFE09D2190140"
//    // When
//    val actual = IntelHex.parseBytes(Source.fromString(sample))
//    // Then
//    actual should be a 'Failure
//    val exception = intercept[ParseException] { actual.get }
//    exception.getMessage should include("unrecognized record type")
//  }
//
//  it should "throw ParseException with incorrect data size (overflow)" in {
//    // Given
//    val sample = ":10010000214601360121470136007EFE09D219010140"
//    // When
//    val actual = IntelHex.parseBytes(Source.fromString(sample))
//    // Then
//    actual should be a 'Failure
//    val exception = intercept[ParseException] { actual.get }
//    exception.getMessage should include("incorrect data size")
//  }
//
//  it should "throw ParseException with incorrect data size (underflow)" in {
//    // Given
//    val sample = ":10010000214601360121470136007EFE09D21940"
//    // When
//    val actual = IntelHex.parseBytes(Source.fromString(sample))
//    // Then
//    actual should be a 'Failure
//    val exception = intercept[ParseException] { actual.get }
//    exception.getMessage should include("incorrect data size")
//  }
//
//  it should "throw ParseException with incorrect checksum" in {
//    // Given
//    val sample = ":10010000214601360121470136007EFE09D2190141"
//    // When
//    val actual = IntelHex.parseBytes(Source.fromString(sample))
//    // Then
//    actual should be a 'Failure
//    val exception = intercept[ParseException] { actual.get }
//    exception.getMessage should include("incorrect checksum")
//  }

}
