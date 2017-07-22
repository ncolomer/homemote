package io.homemote.model

import java.time.Instant

case class Firmware(name: String, version: String)

case class Battery(voltage: Double, timestamp: Instant = Instant.now)

object Node {
  type Id = Either[NetworkID, UniqueID]
  object Id {
    def apply(ref: Any): Id = ref match {
      case int: Int => Left(NetworkID(int))
      case str: String => Right(UniqueID(str))
      case nid: NetworkID => Left(nid)
      case uid: UniqueID => Right(uid)
      case _ => throw new IllegalArgumentException(s"$ref is not a Node id")
    }
  }
  def apply(uid: UniqueID, nid: NetworkID, firmware: String, version: String): Node = {
    val now = Instant.now
    Node(uid, nid, now, now, Firmware(firmware, version))
  }
}

/**
  * {{{
  * {
  *   "uniqueId": "d7:65:7c:68:e3:3c:1d:24",
  *   "networkId": 69,
  *   "firstSeen": "2016-11-04T15:22:36.000Z",
  *   "lastSeen": "2016-11-04T15:22:36.000Z",
  *   "firmware": {
  *     "name": "water-node",
  *     "version": "1.0.0"
  *   }
  *   "battery": {
  *     "voltage": 3.14,
  *     "lastUpdate": "2016-11-04T15:22:36Z"
  *   },
  *   "tags": ["shutter", "first floor", "living room", "bay window", "left"] // always contains the firmware name
  * }
  * }}}
  */
case class Node(uniqueId: UniqueID,
                networkId: NetworkID,
                firstSeen: DateTime,
                lastSeen: DateTime,
                firmware: Firmware,
                battery: Option[Battery] = None,
                tags: Set[String] = Set.empty) {
  override def toString: String = s"[${networkId.id}/${uniqueId.id} ${firmware.name} ${firmware.version}]"
}

