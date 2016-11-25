package io.homemote.repository

import io.homemote.model.{JsonSerde, NetworkID, Node, UniqueID}
import org.elasticsearch.action.DocWriteResponse.Result
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.indices.TermsLookup
import spray.json._

import scala.concurrent.Future

abstract class NodeRepository extends ESRepository with JsonSerde {

  val Index = "node"
  val MaxNodes = 255

  /** Fetch a node from its id */
  def get(id: Either[NetworkID, UniqueID]): Future[Option[Node]] = id match {
    case Left(nid: NetworkID) => get(nid)
    case Right(uid: UniqueID) => get(uid)
  }

  def get(nid: NetworkID): Future[Option[Node]] =
    es.prepareSearch(Index).setTypes(Type)
      .setFetchSource(true).setSize(1)
      .setQuery(QueryBuilders.termQuery("networkId", nid.id))
      .execute.toFuture
      .map(_.getHits.hits.headOption.map(_.sourceAsString.parseJson.convertTo[Node]))

  def get(uid: UniqueID): Future[Option[Node]] =
    es.prepareGet(Index, Type, uid.id)
      .setFetchSource(true)
      .execute.toFuture
      .map(r => if (r.isExists) Some(r.getSourceAsString.parseJson.convertTo[Node]) else None)

  /** Fetch all known nodes */
  def all(from: Int = 0, size: Int = MaxNodes): Future[Array[Node]] =
    es.prepareSearch(Index).setTypes(Type)
      .setFrom(from).setSize(size)
      .execute.toFuture
      .map(_.getHits.hits.map(_.getSourceAsString.parseJson.convertTo[Node]))

  /** Fetch all nodes in group */
  def inGroup(name: String): Future[Array[Node]] =
    es.prepareSearch(Index).setTypes(Type)
      .setSize(MaxNodes)
      .setQuery(QueryBuilders.termsLookupQuery("tags", new TermsLookup("group", "group", name, "tags")))
      .execute.toFuture
      .map(_.getHits.hits.map(_.getSourceAsString.parseJson.convertTo[Node]))

  /** Insert or update a node based on its id */
  def upsert(node: Node): Future[Node] = {
    val json: String = node.toJson
    es.prepareUpdate(Index, Type, node.uniqueId.id)
      .setDoc(json)
      .setDocAsUpsert(true)
      .execute.toFuture
      .map(_.getResult)
      .filter(Set(Result.CREATED, Result.UPDATED).contains)
      .map(_ => node)
  }

  /** Apply a mutation to a node based on its id */
  def updateWith(id: Either[NetworkID, UniqueID], mutation: Node => Node): Future[Node] =
    get(id).flatMap {
      case Some(node) => upsert(mutation(node))
      case None => throw new Exception(s"Node $id was not found")
    }

  /** Fetch all known network ids */
  def getUsedNid: Future[Array[Int]] =
    es.prepareSearch(Index).setTypes(Type)
      .addDocValueField("networkId")
      .setFetchSource(false)
      .setSize(MaxNodes)
      .execute.toFuture
      .map(_.getHits.hits.map(_.field("networkId").getValue[Long].toInt))

}
