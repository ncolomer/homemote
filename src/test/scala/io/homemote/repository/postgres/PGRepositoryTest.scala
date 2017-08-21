package io.homemote.repository.postgres

import anorm.SqlParser.scalar
import anorm._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, Outcome, fixture}
import play.api.db.{Database, Databases}

trait PGRepositoryTest extends fixture.FlatSpec with ScalaFutures with Matchers {

  override type FixtureParam = Database

  override protected def withFixture(test: OneArgTest): Outcome = {
    Databases.withDatabase(
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql://localhost:5432/homemote",
      config = Map("username" -> "user", "password" -> "pass")) { db =>
      val outcome = test(db)
      db.withConnection { implicit conn =>
        SQL("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'").as(scalar[String].*)
          .foreach(table => SQL(s"""DROP TABLE IF EXISTS "$table"""").execute())
      }
      outcome
    }
  }

}
