package io.homemote.repository.postgres

import java.time.Instant

import anorm.Macro.ColumnNaming
import anorm._
import io.homemote.model.{State, UniqueID}
import io.homemote.repository.StateRepository
import play.api.db.Database

import scala.concurrent.{Future, blocking}

class PGStateRepository(db: Database) extends StateRepository with PGRepository {

  db.withConnection { implicit connection =>
    SQL(
      """CREATE TABLE IF NOT EXISTS "state" (
        |  origin VARCHAR NOT NULL,
        |  updated TIMESTAMP NOT NULL,
        |  key VARCHAR NOT NULL,
        |  value VARCHAR NOT NULL,
        |  PRIMARY KEY (origin, key)
        |)""".stripMargin).execute()
  }

  override def setState(node: UniqueID, key: String, value: String): Future[State] =
    Future(db.withConnection { implicit connection =>
      val now = Instant.now
      val state = State(node, now, key, value)
      blocking(SQL(
        """INSERT INTO "state" (origin, updated, key, value) VALUES ({origin}, {updated}, {key}, {value})
          |ON CONFLICT (origin, key) DO UPDATE SET value = EXCLUDED.value, updated = EXCLUDED.updated
        """.stripMargin)
        .on(
          'origin -> state.origin.id,
          'updated -> state.updated,
          'key -> state.key,
          'value -> state.value)
        .execute())
      state
    })

  override def getState(node: UniqueID, key: String): Future[Option[State]] =
    Future(db.withConnection { implicit connection =>
      implicit val uniqueIDParser: Column[UniqueID] = parser[UniqueID] { case id: String => UniqueID(id) }
      blocking(SQL"""SELECT * FROM "state" WHERE origin = ${node.id} AND key = $key"""
          .as(Macro.namedParser[State](ColumnNaming.SnakeCase).singleOpt))
    })

}
