package io.homemote.repository

import io.homemote.model.{Measure, UniqueID}

import scala.concurrent.Future

trait MeasureRepository {

  def insert(measure: Measure): Future[Measure]

}
