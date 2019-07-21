package io.homemote.repository.postgres

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, Outcome, fixture}
import play.api.db.evolutions.Evolutions
import play.api.db.{Database, Databases}

import scala.util.Try

trait PostgresTest extends fixture.FlatSpec with ScalaFutures with Matchers {

  override type FixtureParam = Database

  override protected def withFixture(test: OneArgTest): Outcome = {
    Databases.withDatabase(
      name = "homemote",
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql://localhost:5432/homemote",
      config = Map("username" -> "user", "password" -> "pass")) { db =>
      Evolutions.applyEvolutions(db)
      val outcome = Try(test(db))
      Evolutions.cleanupEvolutions(db)
      outcome.get
    }
  }

}
