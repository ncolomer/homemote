package io.homemote.repository

import org.elasticsearch.action.{ActionListener, ListenableActionFuture}
import org.elasticsearch.client.Client
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future, Promise}

trait ESRepository {

  val log: Logger = LoggerFactory.getLogger(getClass)

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  implicit class ListenableActionFuture_Implicit[T](future: ListenableActionFuture[T]) {
    def toFuture: Future[T] = {
      val promise = Promise[T]()
      future.addListener(new ActionListener[T] {
        override def onResponse(response: T): Unit = {
          log.trace("Elasticsearch response: {}", response)
          promise.success(response)
        }
        override def onFailure(e: Exception): Unit = {
          log.error("Elasticsearch request failure", e)
          promise.failure(e)
        }
      })
      promise.future
    }
  }

  /** Ensure index is created */
  def init(): Unit = if (!es.admin.indices.prepareExists(Index).get.isExists)
    es.admin.indices.prepareCreate(Index)
      .setSettings(Settings, XContentType.JSON)
      .addMapping(Type, s"""{"$Type":$Mapping}""", XContentType.JSON)
      .get

  /** Index name */
  def Index: String

  /** Index's type name */
  def Type: String = Index

  /** Index settings */
  def Settings: String = """{"number_of_shards":1,"number_of_replicas":0}"""

  /** Index's type mapping */
  def Mapping: String = """{"properties":{}}"""

  def es: Client

}
