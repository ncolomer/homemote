package io.homemote.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import spray.json.{JsString, _}

import scala.util.Try

trait JsonSerde extends SprayJsonSupport with DefaultJsonProtocol with NullOptions {

  implicit object DateTimeJsonFormat extends RootJsonFormat[DateTime] {
    val DateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ".r
    val Formatter = DateTimeFormat.forPattern(DateTimePattern.regex)
    def valid(s: String): Boolean = Try(Formatter.parseMillis(s)).isSuccess
    override def write(obj: DateTime): JsValue =
      JsString(Formatter.print(obj))
    override def read(json: JsValue): DateTime = json match {
      case JsString(s) if valid(s) => Formatter.parseDateTime(s)
      case _ => deserializationError("DateTime expected")
    }
  }

  implicit object UniqueIDJsonFormat extends RootJsonFormat[UniqueID] {
    override def write(obj: UniqueID): JsValue = JsString(obj.id)
    override def read(json: JsValue): UniqueID = json match {
      case JsString(UniqueID.Pattern(s)) => UniqueID(s)
      case _ => deserializationError("UniqueID expected")
    }
  }

  implicit object NetworkIDJsonFormat extends RootJsonFormat[NetworkID] {
    override def write(obj: NetworkID): JsValue = JsNumber(obj.id)
    override def read(json: JsValue): NetworkID = json match {
      case JsNumber(s) => NetworkID(s.toInt)
      case _ => deserializationError("NetworkID expected")
    }
  }

  implicit val Printer = CompactPrinter

  implicit val FirmwareFormat = jsonFormat2(Firmware)
  implicit val BatteryFormat = jsonFormat2(Battery)
  implicit val NodeJsonFormat = jsonFormat7(Node.apply)
  implicit val MeasureJsonFormat = jsonFormat4(Measure.apply)
  implicit val GroupJsonFormat = jsonFormat3(Group.apply)

}
