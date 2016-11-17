package io.homemote.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import spray.json._

trait JsonSerde extends SprayJsonSupport with DefaultJsonProtocol with NullOptions {

  implicit object JsonDateTimeFormat extends RootJsonFormat[DateTime] {
    val Pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ".r.anchored
    val Formatter = DateTimeFormat.forPattern(Pattern.regex)
    override def read(json: JsValue): DateTime = json match {
      case JsString(s @ Pattern()) => Formatter.parseDateTime(s)
      case _ => deserializationError("DateTime expected")
    }
    override def write(obj: DateTime): JsValue =
      JsString(Formatter.print(obj))
  }

  implicit val Printer = CompactPrinter

  implicit val FirmwareFormat = jsonFormat2(Firmware)
  implicit val BatteryFormat = jsonFormat2(Battery)
  implicit val NodeJsonFormat = jsonFormat7(Node.apply)

}
