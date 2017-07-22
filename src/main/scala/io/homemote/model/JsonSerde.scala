package io.homemote.model

import java.time.Instant

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{JsString, _}

import scala.util.Try

trait JsonSerde extends SprayJsonSupport with DefaultJsonProtocol with NullOptions {

  implicit object DateTimeJsonFormat extends RootJsonFormat[Instant] {
    def valid(s: String): Boolean = Try(Instant.parse(s)).isSuccess
    override def write(obj: Instant): JsValue = JsString(obj.toString)
    override def read(json: JsValue): Instant = json match {
      case JsString(s) if valid(s) => Instant.parse(s)
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

  implicit val FirmwareFormat: RootJsonFormat[Firmware] = jsonFormat2(Firmware)
  implicit val BatteryFormat: RootJsonFormat[Battery] = jsonFormat2(Battery)
  implicit val NodeJsonFormat: RootJsonFormat[Node] = jsonFormat7(Node.apply)
  implicit val MeasureJsonFormat: RootJsonFormat[Measure] = jsonFormat4(Measure.apply)
  implicit val GroupJsonFormat: RootJsonFormat[Group] = jsonFormat3(Group.apply)

}
