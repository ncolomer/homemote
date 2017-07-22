package io.homemote.repository

import java.time.Instant

import io.homemote.model.{JsonSerde, Measure, UniqueID}
import spray.json._

import scala.concurrent.Future

abstract class MeasureRepository extends ESRepository with JsonSerde {

  override val Index: String = "measure"
  override val Settings: String = """{"number_of_shards":5,"number_of_replicas":0}"""

  def insert(uid: UniqueID, name: String, value: Double): Future[Measure] = {
    val measure = Measure(uid, Instant.now, name, value)
    es.prepareIndex(Index, Type).setSource(measure.toJson)
      .execute().toFuture.map(_ => measure)
  }

}
