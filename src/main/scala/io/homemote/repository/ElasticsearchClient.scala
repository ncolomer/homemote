package io.homemote.repository

import org.elasticsearch.action.{ActionListener, ListenableActionFuture}
import org.elasticsearch.client.Client

import scala.concurrent.{Promise, Future}

trait ElasticsearchClient {

  implicit class ListenableActionFuture_Implicit[T](future: ListenableActionFuture[T]) {
    def toFuture: Future[T] = {
      val promise = Promise[T]()
      future.addListener(new ActionListener[T] {
        override def onFailure(e: Exception): Unit = promise.failure(e)
        override def onResponse(response: T): Unit = promise.success(response)
      })
      promise.future
    }
  }

  def es: Client

}
