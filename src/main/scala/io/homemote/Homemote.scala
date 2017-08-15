package io.homemote

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

  new AppModule().initNonLazy()

}
