package io.homemote.repository.postgres

import java.time.Instant

import anorm._
import io.homemote.model.{State, UniqueID}

class PGStateRepositoryTest extends PGRepositoryTest {

  it should "create state table" in { db =>
    // When
    new PGStateRepository(db)
    // Then
    db.withConnection(conn => conn.prepareStatement("SELECT 1 FROM \"state\"").execute() should be(true))
  }

  it should "insert state" in { db =>
    // Given
    val now = Instant.now
    // When
    val actual = new PGStateRepository(db).setState(UniqueID("98:01:a7:b3:7f:45:02:5e"), "key", "value").futureValue
    // Then
    actual should matchPattern { case State(UniqueID("98:01:a7:b3:7f:45:02:5e"), _, "key", "value") => }
    db.withConnection { implicit conn =>
      SQL("""SELECT * FROM "state" WHERE origin = {origin} AND key = {key}""")
        .onParams("98:01:a7:b3:7f:45:02:5e", "key").executeQuery().resultSet
        .foreach { rs =>
          rs.next() should be(true)
          rs.getString("origin") should equal("98:01:a7:b3:7f:45:02:5e")
          rs.getTimestamp("updated").getTime should be > now.toEpochMilli
          rs.getString("key") should equal("key")
          rs.getString("value") should equal("value")
        }
    }
  }

  it should "update state" in { db =>
    // Given
    val now = Instant.now
    val repository = new PGStateRepository(db)
    db.withConnection { implicit conn =>
      SQL("""INSERT INTO "state" VALUES({origin},{timestamp},{key},{value})""")
        .onParams("98:01:a7:b3:7f:45:02:5e", now, "key", "value1")
        .execute()
    }
    // When
    val actual = repository.setState(UniqueID("98:01:a7:b3:7f:45:02:5e"), "key", "value2").futureValue
    // Then
    actual should matchPattern { case State(UniqueID("98:01:a7:b3:7f:45:02:5e"), _, "key", "value2") => }
    actual.updated should be > now
  }

  it should "get state" in { db =>
    // Given
    val now = Instant.now
    val repository = new PGStateRepository(db)
    db.withConnection { implicit conn =>
      SQL("""INSERT INTO "state" VALUES({origin},{timestamp},{key},{value})""")
        .onParams("98:01:a7:b3:7f:45:02:5e", now, "key", "value")
        .execute()
    }
    // When
    val actual = repository.getState(UniqueID("98:01:a7:b3:7f:45:02:5e"), "key").futureValue
    // Then
    actual should equal(Some(State(UniqueID("98:01:a7:b3:7f:45:02:5e"), now, "key", "value")))
  }

  it should "not find state" in { db =>
    // When
    val actual = new PGStateRepository(db).getState(UniqueID("98:01:a7:b3:7f:45:02:5e"), "key").futureValue
    // Then
    actual should equal(None)
  }

}
