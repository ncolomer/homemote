package io.homemote.repository

import io.homemote.model.Common._
import io.homemote.model.{Battery, JsonSerde, Node}
import org.elasticsearch.action.DocWriteResponse.Result
import org.elasticsearch.index.query.QueryBuilders
import org.joda.time.DateTime
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

trait NodeRepository extends Repository with JsonSerde {
  this: ElasticsearchClient =>

  import ExecutionContext.Implicits.global

  val Index = "homemote"
  val Type = "nodes"

  override def init() = {
    es.admin.indices.prepareCreate(Index).get
  }

  /** Fetch a node from its id */
  def get(id: NodeId): Future[Option[Node]] = id match {
    case Left(nid) =>
      es.prepareSearch(Index).setTypes(Type)
        .setFetchSource(true).setSize(1)
        .setQuery(QueryBuilders.termQuery("networkId", nid))
        .execute.toFuture
        .map(_.getHits.hits.headOption.map(_.sourceAsString.parseJson.convertTo[Node]))
    case Right(uid) =>
      es.prepareGet(Index, Type, uid)
        .setFetchSource(true)
        .execute.toFuture
        .map(r => if (r.isExists) Some(r.getSourceAsString.parseJson.convertTo[Node]) else None)
  }

  /** Fetch all known nodes */
  def all(from: Int = 0, size: Int = 255): Future[Array[Node]] =
    es.prepareSearch(Index).setTypes(Type)
      .setFrom(from).setSize(size)
      .execute.toFuture
      .map(_.getHits.hits.map(_.getSourceAsString.parseJson.convertTo[Node]))

  /** Insert or update a node based on its id */
  def upsert(node: Node): Future[Node] =
    es.prepareUpdate(Index, Type, node.uniqueId)
      .setUpsert(node.toJson)
      .execute.toFuture
      .map(_.getResult)
      .filter(Set(Result.CREATED, Result.UPDATED).contains)
      .map(_ => node)

  /** Apply a mutation to a node based on its id */
  def updateWith(id: NodeId, mutation: Node => Node): Future[Node] =
    get(id).flatMap {
      case Some(node) => upsert(mutation(node))
      case None => throw new Exception(s"Node $id was not found")
    }

  /** Fetch all known network ids */
  def getUsedNid: Future[Array[Nid]] =
    es.prepareSearch(Index).setTypes(Type)
      .addStoredField("networkId").setSize(255)
      .execute.toFuture
      .map(_.getHits.hits().map(_.field("networkId").getValue[Int]))

}
