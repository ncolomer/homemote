package io.homemote.repository

import io.homemote.model.{Group, JsonSerde}
import spray.json._

import scala.concurrent.Future

abstract class GroupRepository extends ESRepository with JsonSerde {

  val Index: String = "group"

  def get(name: String): Future[Option[Group]] =
    es.prepareGet(Index, Type, name).execute().toFuture
      .map(r => if (r.isExists) Some(r.getSourceAsString.parseJson.convertTo[Group]) else None)

  def insert(group: Group): Future[Group] =
    es.prepareIndex(Index, Type).setSource(group.toJson)
      .execute().toFuture.map(_ => group)

}
