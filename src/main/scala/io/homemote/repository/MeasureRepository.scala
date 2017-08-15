package io.homemote.repository

import java.time.Instant

import io.homemote.model.{JsonSerde, Measure, UniqueID}
import org.elasticsearch.client.transport.TransportClient
import spray.json._

import scala.concurrent.Future

class MeasureRepository(val es: TransportClient) extends ESRepository with JsonSerde {

  init("measure", "measure", settings = """{"number_of_shards":5,"number_of_replicas":0}""")

  def insert(uid: UniqueID, name: String, value: Double): Future[Measure] = {
    val measure = Measure(uid, Instant.now, name, value)
    es.prepareIndex("measure", "measure").setSource(measure.toJson)
      .execute().toFuture.map(_ => measure)
  }

}
