package io.homemote.repository.postgres

import anorm._
import io.homemote.model.Measure
import io.homemote.repository.MeasureRepository
import play.api.db.Database

import scala.concurrent.{Future, blocking}

class PGMeasureRepository(db: Database) extends MeasureRepository with PGRepository {

  db.withConnection { implicit connection =>
    SQL(
      """CREATE TABLE IF NOT EXISTS "measure" (
        |  id BIGSERIAL PRIMARY KEY,
        |  origin VARCHAR NOT NULL,
        |  timestamp TIMESTAMP NOT NULL,
        |  name VARCHAR NOT NULL,
        |  value DECIMAL NOT NULL
        |)""".stripMargin).execute()
  }

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
