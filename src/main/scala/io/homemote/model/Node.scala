package io.homemote.model

import org.joda.time.DateTime

case class Firmware(name: String, version: String)

case class Battery(voltage: Double, timestamp: DateTime = DateTime.now)

object Node {
  def firstSeen(uid: String, nid: Int, firmware: String, version: String) = {
    val now = DateTime.now
    Node(uid, nid, now, now, Firmware(firmware, version))
  }
}

case class Node(uniqueId: String,
                networkId: Int,
                firstSeen: DateTime,
                lastSeen: DateTime,
                firmware: Firmware,
                battery: Option[Battery] = None,
                tags: Set[String] = Set.empty)

