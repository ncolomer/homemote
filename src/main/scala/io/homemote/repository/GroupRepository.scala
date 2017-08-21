package io.homemote.repository
import io.homemote.model.Group

import scala.concurrent.Future

trait GroupRepository {

  def get(name: String): Future[Option[Group]]

  def insert(group: Group): Future[Group]

}
