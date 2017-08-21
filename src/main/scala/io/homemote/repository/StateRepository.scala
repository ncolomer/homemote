package io.homemote.repository

import io.homemote.model.{State, UniqueID}

import scala.concurrent.Future

trait StateRepository {

  def setState(id: UniqueID, key: String, value: String): Future[State]

  def getState(id: UniqueID, key: String): Future[Option[State]]

}
