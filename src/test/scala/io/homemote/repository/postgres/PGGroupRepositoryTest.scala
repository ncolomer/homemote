package io.homemote.repository.postgres

import anorm.SqlParser.bool
import anorm._
import io.homemote.model.Group

class PGGroupRepositoryTest extends PostgresTest {

  it should "create group table" in { db =>
    // When
    new PGGroupRepository(db)
    // Then
    db.withConnection(implicit conn => SQL("""SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'group')""")
      .as(bool(1).single) should be(true))
  }

  it should "insert group" in { db =>
    // Given
    val group = Group("group1", Set("tag1", "tag2"), Set("group2", "group3"))
    // When
    val actual = new PGGroupRepository(db).insert(group).futureValue
    // Then
    actual should equal(group)
    db.withConnection { implicit conn =>
      SQL("""SELECT * FROM "group" WHERE name = 'group1'""").executeQuery().resultSet
        .foreach { rs =>
          rs.next() should be(true)
          rs.getString("name") should equal("group1")
          rs.getArray("tags").getArray().asInstanceOf[Array[String]].toSet should equal(Set("tag1", "tag2"))
          rs.getArray("groups").getArray().asInstanceOf[Array[String]].toSet should equal(Set("group2", "group3"))
        }
    }
  }

  it should "update group" in { db =>
    // Given
    val conn = db.getConnection()
    val repository = new PGGroupRepository(db)
    db.withConnection { implicit conn =>
      SQL("""INSERT INTO "group" VALUES({name},{tags},{groups})""")
        .on('name -> "group1", 'tags -> Array("tag1", "tag2"), 'groups -> Array("group2", "group3"))
        .execute()
    }
    val group = Group("group1", Set("tag3"), Set("group4"))
    // When
    val actual = repository.insert(group).futureValue
    // Then
    actual should equal(group)
  }

  it should "get group" in { db =>
    // Given
    val repository = new PGGroupRepository(db)
    db.withConnection { implicit conn =>
      SQL("""INSERT INTO "group" VALUES({name},{tags},{groups})""")
        .on('name -> "group1", 'tags -> Array("tag1"), 'groups -> Array("group2"))
        .execute()
    }
    // When
    val actual = repository.get("group1").futureValue
    // Then
    actual should equal(Some(Group("group1", Set("tag1"), Set("group2"))))
  }

  it should "not find group" in { db =>
    // When
    val actual = new PGGroupRepository(db).get("group1").futureValue
    // Then
    actual should equal(None)
  }

}
