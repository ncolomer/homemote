package io.homemote.repository.postgres

import java.time.{Instant, LocalDateTime}

import anorm.{BatchSql, NamedParameter, SQL}
import io.homemote.model._

class PGNodeRepositoryTest extends PGRepositoryTest {

  it should "create node table" in { db =>
    // When
    new PGNodeRepository(db)
    // Then
    db.withConnection(conn => conn.prepareStatement("SELECT 1 FROM \"node\"").execute() should be(true))
  }

  it should "insert node" in { db =>
    // Given
    val now = Instant.now
    val node = Node(UniqueID("98:01:a7:b3:7f:45:02:5e"), NetworkID(10), now, now, Firmware("firmware", "1.0"), Some(Battery(5.05, now)), Set("tag1", "tag2"))
    // When
    val actual = new PGNodeRepository(db).upsert(node).futureValue
    // Then
    actual should equal(node)
    db.withConnection { implicit conn =>
      SQL("""SELECT * FROM "node" WHERE unique_id = {unique_id}""")
        .onParams("98:01:a7:b3:7f:45:02:5e").executeQuery().resultSet
        .foreach { rs =>
          rs.next() should be(true)
          rs.getString("unique_id") should equal("98:01:a7:b3:7f:45:02:5e")
          rs.getInt("network_id") should equal(10)
          rs.getTimestamp("first_seen").getTime should equal(now.toEpochMilli)
          rs.getTimestamp("last_seen").getTime should equal(now.toEpochMilli)
          rs.getString("firmware_name") should equal("firmware")
          rs.getString("firmware_version") should equal("1.0")
          rs.getDouble("battery_voltage") should equal(5.05)
          rs.getTimestamp("battery_timestamp").getTime should equal(now.toEpochMilli)
          rs.getArray("tags").getArray().asInstanceOf[Array[String]].toSet should equal(Set("tag1", "tag2"))
        }
    }
  }

  it should "update node" in { db =>
    // Given
    val repository = new PGNodeRepository(db)
    db.withConnection { implicit conn =>
      val datetime = LocalDateTime.parse("2017-08-20T18:23:01")
      SQL("""INSERT INTO "node" VALUES({unique_id},{network_id},{first_seen},{last_seen},{firmware_name},{firmware_version},{battery_voltage},{battery_timestamp},{tags})""")
        .onParams("98:01:a7:b3:7f:45:02:5e", 10, datetime, datetime, "firmware", "1.0", 5.05, datetime, Array[String]("tag1", "tag2"))
        .execute()
    }
    val now = Instant.now
    val node = Node(UniqueID("98:01:a7:b3:7f:45:02:5e"), NetworkID(11), now, now, Firmware("firmware2", "version2"), None, Set("tag3"))
    // When
    repository.upsert(node).futureValue
    // Then
    db.withConnection { implicit conn =>
      SQL("""SELECT * FROM "node" WHERE unique_id = {unique_id}""")
        .onParams("98:01:a7:b3:7f:45:02:5e").executeQuery().resultSet
        .foreach { rs =>
          rs.next() should be(true)
          rs.getString("unique_id") should equal("98:01:a7:b3:7f:45:02:5e")
          rs.getInt("network_id") should equal(11)
          rs.getTimestamp("first_seen").getTime should equal(now.toEpochMilli)
          rs.getTimestamp("last_seen").getTime should equal(now.toEpochMilli)
          rs.getString("firmware_name") should equal("firmware2")
          rs.getString("firmware_version") should equal("version2")
          rs.getDouble("battery_voltage") should equal(null.asInstanceOf[Double])
          rs.getTimestamp("battery_timestamp") should equal(null)
          rs.getArray("tags").getArray().asInstanceOf[Array[String]].toSet should equal(Set("tag3"))
        }
    }
  }

  it should "update node with mutation" in { db =>
    // Given
    val repository = new PGNodeRepository(db)
    db.withConnection { implicit conn =>
      val datetime = LocalDateTime.parse("2017-08-20T18:23:01")
      SQL("""INSERT INTO "node" VALUES({unique_id},{network_id},{first_seen},{last_seen},{firmware_name},{firmware_version},{battery_voltage},{battery_timestamp},{tags})""")
        .onParams("98:01:a7:b3:7f:45:02:5e", 10, datetime, datetime, "firmware", "1.0", 5.05, datetime, Array[String]("tag1", "tag2"))
        .execute()
    }
    val now = Instant.now
    val node = Node(UniqueID("98:01:a7:b3:7f:45:02:5e"), NetworkID(11), now, now, Firmware("firmware2", "version2"), None, Set("tag3"))
    // When
    repository.updateWith(Right(node.uniqueId), _.copy(
      networkId = NetworkID(11),
      firstSeen = now,
      lastSeen = now,
      firmware = Firmware("firmware2", "version2"),
      battery = None,
      tags = Set("tag3")
    )).futureValue
    // Then
    db.withConnection { implicit conn =>
      SQL("""SELECT * FROM "node" WHERE unique_id = {unique_id}""")
        .onParams("98:01:a7:b3:7f:45:02:5e").executeQuery().resultSet
        .foreach { rs =>
          rs.next() should be(true)
          rs.getString("unique_id") should equal("98:01:a7:b3:7f:45:02:5e")
          rs.getInt("network_id") should equal(11)
          rs.getTimestamp("first_seen").getTime should equal(now.toEpochMilli)
          rs.getTimestamp("last_seen").getTime should equal(now.toEpochMilli)
          rs.getString("firmware_name") should equal("firmware2")
          rs.getString("firmware_version") should equal("version2")
          rs.getDouble("battery_voltage") should equal(null.asInstanceOf[Double])
          rs.getTimestamp("battery_timestamp") should equal(null)
          rs.getArray("tags").getArray().asInstanceOf[Array[String]].toSet should equal(Set("tag3"))
        }
    }
  }

  it should "get node from its unique id" in { db =>
    // Given
    val now = Instant.now
    val repository = new PGNodeRepository(db)
    db.withConnection { implicit conn =>
      SQL("""INSERT INTO "node" VALUES({unique_id},{network_id},{first_seen},{last_seen},{firmware_name},{firmware_version},{battery_voltage},{battery_timestamp},{tags})""")
        .onParams("98:01:a7:b3:7f:45:02:5e", 10, now, now, "firmware", "1.0", 5.05, now, Array[String]("tag1", "tag2"))
        .execute()
    }
    // When
    val actual = repository.get(UniqueID("98:01:a7:b3:7f:45:02:5e")).futureValue
    // Then
    actual should equal(Some(Node(UniqueID("98:01:a7:b3:7f:45:02:5e"), NetworkID(10), now, now, Firmware("firmware", "1.0"), Some(Battery(5.05, now)), Set("tag1", "tag2"))))
  }

  it should "get node from its network id" in { db =>
    // Given
    val now = Instant.now
    val repository = new PGNodeRepository(db)
    db.withConnection { implicit conn =>
      SQL("""INSERT INTO "node" VALUES({unique_id},{network_id},{first_seen},{last_seen},{firmware_name},{firmware_version},{battery_voltage},{battery_timestamp},{tags})""")
        .onParams("98:01:a7:b3:7f:45:02:5e", 10, now, now, "firmware", "1.0", 5.05, now, Array[String]("tag1", "tag2"))
        .execute()
    }
    // When
    val actual = repository.get(NetworkID(10)).futureValue
    // Then
    actual should equal(Some(Node(UniqueID("98:01:a7:b3:7f:45:02:5e"), NetworkID(10), now, now, Firmware("firmware", "1.0"), Some(Battery(5.05, now)), Set("tag1", "tag2"))))
  }

  it should "not find node" in { db =>
    // When
    val actual = new PGNodeRepository(db).get(UniqueID("98:01:a7:b3:7f:45:02:5e")).futureValue
    // Then
    actual should equal(None)
  }

  it should "get node unique id from its network id" in { db =>
    // Given
    val now = Instant.now
    val repository = new PGNodeRepository(db)
    db.withConnection { implicit conn =>
      SQL("""INSERT INTO "node" VALUES({unique_id},{network_id},{first_seen},{last_seen},{firmware_name},{firmware_version},{battery_voltage},{battery_timestamp},{tags})""")
        .onParams("98:01:a7:b3:7f:45:02:5e", 10, now, now, "firmware", "1.0", 5.05, now, Array[String]("tag1", "tag2"))
        .execute()
    }
    // When
    val actual = repository.getUniqueID(NetworkID(10)).futureValue
    // Then
    actual should equal(Some(UniqueID("98:01:a7:b3:7f:45:02:5e")))
  }

  it should "fetch all nodes" in { db =>
    // Given
    val now = Instant.now
    val repository = new PGNodeRepository(db)
    db.withConnection { implicit conn =>
      BatchSql("""INSERT INTO "node" VALUES({unique_id},{network_id},{first_seen},{last_seen},{firmware_name},{firmware_version},{battery_voltage},{battery_timestamp},{tags})""",
        Seq[NamedParameter]('unique_id -> "98:01:a7:b3:7f:45:02:5e", 'network_id -> 10, 'first_seen -> now, 'last_seen -> now, 'firmware_name -> "firmware", 'firmware_version -> "1.0", 'battery_voltage -> 5.05, 'battery_timestamp -> now, 'tags -> Array[String]("tag1", "tag2")),
        Seq[NamedParameter]('unique_id -> "98:01:a7:b3:7f:45:02:5f", 'network_id -> 11, 'first_seen -> now, 'last_seen -> now, 'firmware_name -> "firmware", 'firmware_version -> "1.0", 'battery_voltage -> Option.empty[Double], 'battery_timestamp -> Option.empty[Instant], 'tags -> Array[String]("tag2", "tag3")),
      ).execute()
    }
    // When
    val actual = repository.all().futureValue
    // Then
    actual should contain theSameElementsAs Seq(
      Node(UniqueID("98:01:a7:b3:7f:45:02:5e"), NetworkID(10), now, now, Firmware("firmware", "1.0"), Some(Battery(5.05, now)), Set("tag1", "tag2")),
      Node(UniqueID("98:01:a7:b3:7f:45:02:5f"), NetworkID(11), now, now, Firmware("firmware", "1.0"), None, Set("tag2", "tag3"))
    )
  }

  it should "fetch all nodes with tags" in { db =>
    // Given
    val now = Instant.now
    val repository = new PGNodeRepository(db)
    db.withConnection { implicit conn =>
      BatchSql("""INSERT INTO "node" VALUES({unique_id},{network_id},{first_seen},{last_seen},{firmware_name},{firmware_version},{battery_voltage},{battery_timestamp},{tags})""",
        Seq[NamedParameter]('unique_id -> "98:01:a7:b3:7f:45:02:5e", 'network_id -> 10, 'first_seen -> now, 'last_seen -> now, 'firmware_name -> "firmware", 'firmware_version -> "1.0", 'battery_voltage -> 5.05, 'battery_timestamp -> now, 'tags -> Array[String]("tag1", "tag2")),
        Seq[NamedParameter]('unique_id -> "98:01:a7:b3:7f:45:02:5f", 'network_id -> 11, 'first_seen -> now, 'last_seen -> now, 'firmware_name -> "firmware", 'firmware_version -> "1.0", 'battery_voltage -> Option.empty[Double], 'battery_timestamp -> Option.empty[Instant], 'tags -> Array[String]("tag2", "tag3")),
      ).execute()
    }
    // When
    val actual1 = repository.withTags("tag2").futureValue
    val actual2 = repository.withTags("tag1").futureValue
    val actual3 = repository.withTags("tag0").futureValue
    // Then
    actual1.map(_.uniqueId.id) should contain theSameElementsAs Seq("98:01:a7:b3:7f:45:02:5e", "98:01:a7:b3:7f:45:02:5f")
    actual2.map(_.uniqueId.id) should contain theSameElementsAs Seq("98:01:a7:b3:7f:45:02:5e")
    actual3.map(_.uniqueId.id) should be ('empty)
  }

  it should "fetch all used network ids" in { db =>
    // Given
    val now = Instant.now
    val repository = new PGNodeRepository(db)
    db.withConnection { implicit conn =>
      BatchSql("""INSERT INTO "node" VALUES({unique_id},{network_id},{first_seen},{last_seen},{firmware_name},{firmware_version},{battery_voltage},{battery_timestamp},{tags})""",
        Seq[NamedParameter]('unique_id -> "98:01:a7:b3:7f:45:02:5e", 'network_id -> 10, 'first_seen -> now, 'last_seen -> now, 'firmware_name -> "firmware", 'firmware_version -> "1.0", 'battery_voltage -> 5.05, 'battery_timestamp -> now, 'tags -> Array[String]("tag1", "tag2")),
        Seq[NamedParameter]('unique_id -> "98:01:a7:b3:7f:45:02:5f", 'network_id -> 11, 'first_seen -> now, 'last_seen -> now, 'firmware_name -> "firmware", 'firmware_version -> "1.0", 'battery_voltage -> Option.empty[Double], 'battery_timestamp -> Option.empty[Instant], 'tags -> Array[String]("tag2", "tag3")),
      ).execute()
    }
    // When
    val actual = repository.getUsedNid.futureValue
    // Then
    actual should contain theSameElementsAs Seq(10, 11)
  }

}
