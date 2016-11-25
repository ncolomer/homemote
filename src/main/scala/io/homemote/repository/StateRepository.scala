package io.homemote.repository

import io.homemote.model.{Group, JsonSerde}
import spray.json._

import scala.concurrent.Future

abstract class StateRepository extends ESRepository with JsonSerde {

  val Index: String = "state"

}
