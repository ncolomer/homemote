package io.homemote.repository.postgres

import anorm.Macro.ColumnNaming
import anorm.{Macro, _}
import io.homemote.model.Group
import io.homemote.repository.GroupRepository
import play.api.db.Database

import scala.concurrent.{Future, blocking}

class PGGroupRepository(db: Database) extends GroupRepository with PGRepository {

  override def get(name: String): Future[Option[Group]] =
    Future(db.withConnection { implicit connection =>
      implicit val setStringParser: Column[Set[String]] = parser[Set[String]] {
        case arr: java.sql.Array => arr.getArray().asInstanceOf[Array[String]].toSet }
      blocking(SQL"""SELECT * FROM "group" WHERE name = $name"""
        .as(Macro.namedParser[Group](ColumnNaming.SnakeCase).singleOpt))
    })

  override def insert(group: Group): Future[Group] =
    Future(db.withConnection { implicit connection =>
      blocking(SQL("""INSERT INTO "group" (name, tags, groups) VALUES ({name}, {tags}, {groups})
            ON CONFLICT (name) DO UPDATE SET tags = EXCLUDED.tags, groups = EXCLUDED.groups""")
        .on(
          'name -> group.name,
          'tags -> group.tags.toArray[String],
          'groups -> group.groups.toArray[String])
        .execute())
      group
    })

}
