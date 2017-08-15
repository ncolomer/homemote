package io.homemote.repository

import io.homemote.model.{Group, JsonSerde}
import org.elasticsearch.client.transport.TransportClient
import spray.json._

import scala.concurrent.Future

class GroupRepository(val es: TransportClient) extends ESRepository with JsonSerde {

  init("group", "group")

  def get(name: String): Future[Option[Group]] =
    es.prepareGet("group", "group", name).execute().toFuture
      .map(r => if (r.isExists) Some(r.getSourceAsString.parseJson.convertTo[Group]) else None)

  def insert(group: Group): Future[Group] =
    es.prepareIndex("group", "group").setSource(group.toJson)
      .execute().toFuture.map(_ => group)

}
