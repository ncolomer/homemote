package io.homemote.repository

import io.homemote.model.{JsonSerde, NetworkID, Node, UniqueID}
import org.elasticsearch.action.DocWriteResponse.Result
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.indices.TermsLookup
import org.elasticsearch.search.SearchHit
import spray.json._

import scala.concurrent.Future
import scala.language.implicitConversions

abstract class NodeRepository extends ESRepository with JsonSerde {

  implicit def get2Node(get: GetResponse): Node = get.getSourceAsString.parseJson.convertTo[Node]
  implicit def hit2Node(hit: SearchHit): Node = hit.getSourceAsString.parseJson.convertTo[Node]
  implicit def search2Nodes(res: SearchResponse): Array[Node] = res.getHits.getHits.map(a => a: Node)

  val Index = "node"
  val MaxNodes = 255

  /** Fetch a node from its id */
  def get(id: Node.Id): Future[Node] = id match {
    case Left(nid: NetworkID) => get(nid)
    case Right(uid: UniqueID) => get(uid)
  }

  def get(nid: NetworkID): Future[Node] =
    es.prepareSearch(Index).setTypes(Type)
      .setFetchSource(true).setSize(1)
      .setQuery(QueryBuilders.termQuery("networkId", nid.id))
      .execute.toFuture
      .map(_.getHits.getHits.headOption)
      .collect { case Some(hit) => hit: Node }

  def get(uid: UniqueID): Future[Node] =
    es.prepareGet(Index, Type, uid.id)
      .setFetchSource(true)
      .execute.toFuture
      .collect { case res if res.isExists => res: Node }

  /** Fetch all known nodes */
  def all(from: Int = 0, size: Int = MaxNodes): Future[Array[Node]] =
    es.prepareSearch(Index).setTypes(Type)
      .setFrom(from).setSize(size)
      .execute.toFuture
      .map(res => res: Array[Node])

  /** Fetch all nodes in group */
  def inGroup(name: String): Future[Array[Node]] =
    es.prepareSearch(Index).setTypes(Type)
      .setSize(MaxNodes)
      .setQuery(QueryBuilders.termsLookupQuery("tags", new TermsLookup("group", "group", name, "tags")))
      .execute.toFuture
      .map(res => res: Array[Node])

  /** Insert or update a node based on its id */
  def upsert(node: Node): Future[Node] = {
    val json: String = node.toJson
    es.prepareUpdate(Index, Type, node.uniqueId.id)
      .setDoc(json, XContentType.JSON)
      .setDocAsUpsert(true)
      .execute.toFuture
      .map(_.getResult)
      .filter(Set(Result.CREATED, Result.UPDATED).contains)
      .map(_ => node)
  }

  /** Apply a mutation to a node based on its id */
  def updateWith(id: Node.Id, mutation: Node => Node): Future[Node] =
    get(id).flatMap(node => upsert(mutation(node)))

  /** Fetch all known network ids */
  def getUsedNid: Future[Array[Int]] =
    es.prepareSearch(Index).setTypes(Type)
      .addDocValueField("networkId")
      .setFetchSource(false)
      .setSize(MaxNodes)
      .execute.toFuture
      .map(_.getHits.getHits.map(_.getField("networkId").getValue[Long].toInt))

}
