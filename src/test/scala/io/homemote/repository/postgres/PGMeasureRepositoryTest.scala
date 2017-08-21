package io.homemote.repository.postgres

import java.time.Instant

import anorm._
import io.homemote.model.{Measure, UniqueID}

class PGMeasureRepositoryTest extends PGRepositoryTest {

  it should "create measure table" in { db =>
    // When
    new PGMeasureRepository(db)
    // Then
    db.withConnection(conn => conn.prepareStatement("SELECT 1 FROM \"measure\"").execute() should be(true))
  }

  it should "insert measure" in { db =>
    // Given
    val now = Instant.now
    val measure = Measure(UniqueID("6a:00:02:5e:00:11:00:10"), now, "measure1", 0.24)
    val repository = new PGMeasureRepository(db)
    // When
    val actual = repository.insert(measure).futureValue
    // Then
    actual should equal(measure)
    db.withConnection { implicit conn =>
      SQL("""SELECT * FROM measure WHERE id = 1""").executeQuery().resultSet
        .foreach { rs =>
          rs.next() should be(true)
          rs.getString("origin") should equal("6a:00:02:5e:00:11:00:10")
          rs.getTimestamp("timestamp").getTime should equal(now.toEpochMilli)
          rs.getString("name") should equal("measure1")
          rs.getDouble("value") should equal(0.24)
        }
    }
  }

}
