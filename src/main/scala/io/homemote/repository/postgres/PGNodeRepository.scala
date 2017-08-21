package io.homemote.repository.postgres

import java.time.Instant

import anorm.SqlParser.{get => getTyped, _}
import anorm._
import io.homemote.model._
import io.homemote.repository.NodeRepository
import play.api.db.Database

import scala.concurrent.{Future, blocking}

class PGNodeRepository(db: Database) extends NodeRepository with PGRepository {

  db.withConnection { implicit connection =>
    SQL(
      """CREATE TABLE IF NOT EXISTS "node" (
        |  unique_id VARCHAR PRIMARY KEY,
        |  network_id SMALLINT NOT NULL UNIQUE,
        |  first_seen TIMESTAMP NOT NULL,
        |  last_seen TIMESTAMP NOT NULL,
        |  firmware_name VARCHAR NOT NULL,
        |  firmware_version VARCHAR NOT NULL,
        |  battery_voltage DECIMAL,
        |  battery_timestamp TIMESTAMP,
        |  tags VARCHAR[] NOT NULL
        |)""".stripMargin).execute()
  }

  private val nodeParser = (str("unique_id") ~ int("network_id") ~ getTyped[Instant]("first_seen") ~ getTyped[Instant]("last_seen") ~ str("firmware_name") ~ str("firmware_version") ~ double("battery_voltage").? ~ getTyped[Instant]("battery_timestamp").? ~ array[String]("tags")) map {
    case uid ~ nid ~ firstSeen ~ lastSeen ~ firmwareName ~ firmwareVersion ~ batteryVoltage ~ batteryTimestamp ~ tags =>
      Node(UniqueID(uid), NetworkID(nid), firstSeen, lastSeen, Firmware(firmwareName, firmwareVersion), for {voltage <- batteryVoltage; tmp <- batteryTimestamp} yield Battery(voltage, tmp), tags.toSet)
  }

  override def getUniqueID(nid: NetworkID): Future[Option[UniqueID]] =
    Future(db.withConnection { implicit connection =>
      blocking(SQL"""SELECT unique_id FROM "node" WHERE network_id = ${nid.id}"""
        .as(scalar[String].singleOpt).map(UniqueID.apply))
    })

  override def get(id: Node.Id): Future[Option[Node]] = id match {
    case Left(nid: NetworkID) => get(nid)
    case Right(uid: UniqueID) => get(uid)
  }

  override def get(nid: NetworkID): Future[Option[Node]] =
    getUniqueID(nid).flatMap(_.map(get).getOrElse(Future.successful(None)))

  override def get(uid: UniqueID): Future[Option[Node]] =
    Future(db.withConnection { implicit connection =>
      blocking(SQL"""SELECT * FROM "node" WHERE unique_id = ${uid.id}""".as(nodeParser.singleOpt))
    })

  /** Fetch all known nodes */
  override def all(from: Int = 0, size: Int = 255): Future[List[Node]] =
    Future(db.withConnection { implicit connection =>
      blocking(SQL"""SELECT * FROM "node" ORDER BY network_id ASC LIMIT $size OFFSET $from""".as(nodeParser.*))
    })

  override def withTags(tags: String*): Future[List[Node]] =
    Future(db.withConnection { implicit connection =>
      blocking(SQL("""SELECT * FROM "node" WHERE {condition}""")
        .on('condition -> SeqParameter(seq = tags, sep = " AND ", pre = "", post = " = ANY (tags)"))
        .as(nodeParser.*))
    })

  /** Insert or update a node based on its id */
  override def upsert(node: Node): Future[Node] =
    Future(db.withConnection { implicit connection =>
      blocking(SQL(
        """INSERT INTO "node" (unique_id, network_id, first_seen, last_seen, firmware_name, firmware_version, battery_voltage, battery_timestamp, tags)
          |VALUES ({unique_id}, {network_id}, {first_seen}, {last_seen}, {firmware_name}, {firmware_version}, {battery_voltage}, {battery_timestamp}, {tags})
          |ON CONFLICT (unique_id) DO UPDATE SET
          |  unique_id = EXCLUDED.unique_id,
          |  network_id = EXCLUDED.network_id,
          |  first_seen = EXCLUDED.first_seen,
          |  last_seen = EXCLUDED.last_seen,
          |  firmware_name = EXCLUDED.firmware_name,
          |  firmware_version = EXCLUDED.firmware_version,
          |  battery_voltage = EXCLUDED.battery_voltage,
          |  battery_timestamp = EXCLUDED.battery_timestamp,
          |  tags = EXCLUDED.tags""".stripMargin)
        .on(
          'unique_id -> node.uniqueId.id,
          'network_id -> node.networkId.id,
          'first_seen -> node.firstSeen,
          'last_seen -> node.lastSeen,
          'firmware_name -> node.firmware.name,
          'firmware_version -> node.firmware.version,
          'battery_voltage -> node.battery.map(_.voltage),
          'battery_timestamp -> node.battery.map(_.timestamp),
          'tags -> node.tags.toArray[String])
        .execute())
      node
    })

  /** Apply a mutation to a node based on its id */
  def updateWith(id: Node.Id, transform: Node => Node): Future[Option[Node]] =
    get(id).flatMap {
      case Some(node) => upsert(transform(node)).map(Some.apply)
      case None => Future.successful(None)
    }

  /** Fetch all known network ids */
  override def getUsedNid: Future[List[Int]] =
    Future(db.withConnection { implicit connection =>
      blocking(SQL("""SELECT network_id FROM "node"""").as(scalar[Int].*))
    })

}
