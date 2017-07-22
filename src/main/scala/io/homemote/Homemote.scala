package io.homemote

import akka.actor.{ActorSystem, Props}
import io.homemote.actor.Root
import org.slf4j.LoggerFactory

object Homemote extends App {

  LoggerFactory.getLogger(this.getClass).info(
    s"""Starting up...
        |
        |  Artifact name:  ${BuildInfo.name}
        |  Artifact build: ${BuildInfo.version}
        |  Scala version:  ${BuildInfo.scalaVersion}
        |  Java runtime:   ${classOf[Runtime].getPackage.getImplementationVersion}
        |""".stripMargin)

  val system = ActorSystem("homemote")
  system.actorOf(Props[Root])
  sys.addShutdownHook(system.terminate)

}
