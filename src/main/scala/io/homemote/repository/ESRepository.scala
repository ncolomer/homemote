package io.homemote.repository

import java.util.concurrent.Executors

import org.elasticsearch.action.{ActionListener, ListenableActionFuture}
import org.elasticsearch.client.Client
import org.elasticsearch.common.xcontent.XContentType

import scala.concurrent.{ExecutionContext, Future, Promise}

trait ESRepository {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newWorkStealingPool())

  implicit class ListenableActionFuture_Implicit[T](future: ListenableActionFuture[T]) {
    def toFuture(implicit ec: ExecutionContext): Future[T] = {
      val promise = Promise[T]()
      future.addListener(new ActionListener[T] {
        override def onResponse(response: T): Unit = promise.success(response)
        override def onFailure(e: Exception): Unit = promise.failure(e)
      })
      promise.future
    }
  }

  val DefaultSettings: String = """{"number_of_shards":1,"number_of_replicas":0}"""

  val DefaultMapping: String = """{"properties":{}}"""

  def es: Client

  /** Ensure index is created */
  def init(index: String, `type`: String, settings: String = DefaultSettings, mapping: String = DefaultMapping): Unit =
    if (!es.admin.indices.prepareExists(index).get.isExists)
      es.admin.indices.prepareCreate(`type`)
        .setSettings(settings, XContentType.JSON)
        .addMapping(`type`, s"""{"${`type`}":$mapping}""", XContentType.JSON)
        .get()

}
