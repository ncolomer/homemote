package io.homemote.repository

import io.homemote.model.{NetworkID, Node, UniqueID}

import scala.concurrent.Future

trait NodeRepository {

  def getUniqueID(nid: NetworkID): Future[Option[UniqueID]]

  def get(id: Node.Id): Future[Option[Node]]

  def get(nid: NetworkID): Future[Option[Node]]

  def get(uid: UniqueID): Future[Option[Node]]

  /** Fetch all known nodes */
  def all(from: Int = 0, size: Int = 255): Future[List[Node]]

  /** Fetch nodes having tags */
  def withTags(tags: String*): Future[List[Node]]

  /** Insert or update a node based on its id */
  def upsert(node: Node): Future[Node]

  /** Apply a mutation to a node based on its id */
  def updateWith(id: Node.Id, transform: Node => Node): Future[Option[Node]]

  /** Fetch all known network ids */
  def getUsedNid: Future[List[Int]]

}
