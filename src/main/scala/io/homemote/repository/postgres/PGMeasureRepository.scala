package io.homemote.repository.postgres

import anorm._
import io.homemote.model.Measure
import io.homemote.repository.MeasureRepository
import play.api.db.Database

import scala.concurrent.{Future, blocking}

class PGMeasureRepository(db: Database) extends MeasureRepository with PGRepository {

  override def insert(measure: Measure): Future[Measure] =
    Future(db.withConnection { implicit connection =>
      blocking(SQL("""INSERT INTO "measure" (origin, timestamp, name, value) VALUES ({origin}, {timestamp}, {name}, {value})""")
        .on(
          'origin -> measure.origin.id,
          'timestamp -> measure.timestamp,
          'name -> measure.name,
          'value -> measure.value)
        .execute())
      measure
    })

}
