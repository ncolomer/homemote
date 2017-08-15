import java.time.LocalDateTime.now
import java.time.format.{DateTimeFormatter => DTF}

import com.typesafe.sbt.packager.archetypes.ServerLoader
import DebianConstants._

val ScalaVersion = "2.12.2"
val AkkaVersion = "2.4.19"
val AkkaHttpVersion = "10.0.9"
val ScaldiVersion = "0.5.8"
val ElasticsearchVersion = "5.5.0"

val log4j = Seq(ExclusionRule("org.slf4j", "slf4j-log4j12"), ExclusionRule("log4j", "*"))
val junit = Seq(ExclusionRule("junit", "*"))

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  enablePlugins(RevolverPlugin).
  enablePlugins(JavaServerAppPackaging, DebianPlugin, JDebPackaging, ClasspathJarPlugin).
  settings(
    // General
    organization  := "io.homemote",
    name := "homemote",
    version := scala.util.Try("git rev-parse --short HEAD".!!.trim).getOrElse("unknown"),

    // Artifact generation
    maintainer := "Homemote <maintainer@homemote.io>",
    packageSummary := "Homemote Debian Package",
    packageDescription := "This is the Homemote debian installer",
    mainClass in Compile := Some("io.homemote.Homemote"),
    version in Debian := s"${DTF.ofPattern("yyyyMMddHHmmss").format(now)}+${version.value}",
    daemonUser in Debian := "homemote",
    daemonGroup in Debian := "homemote",
    serverLoading in Debian := ServerLoader.Systemd,
    debianPackageDependencies in Debian ++= Seq("java8-runtime", "librxtx-java", "elasticsearch (>= 5.0.0)"),
    //javaOptions in Universal += "-Djava.library.path=/usr/lib/jni/",
    scriptClasspath += "/usr/share/java/RXTXcomm.jar",
    bashScriptExtraDefines ++= Seq(
      """addJava "-Xmx256m"""",
      """addJava "-Xms256m"""",
      """addJava "-XX:+HeapDumpOnOutOfMemoryError"""",
      """addJava "-XX:OnOutOfMemoryError='kill -9 %p'"""",
      """addJava "-Dconfig.file=/etc/homemote/application.conf"""",
      """addJava "-Dlogback.configurationFile=/etc/homemote/logback.xml""""
    ),
    maintainerScripts in Debian := maintainerScriptsAppend((maintainerScripts in Debian).value)(
      Postinst -> s"adduser ${(daemonUser in Debian).value} dialout"
    ),
    mappings in Universal := (mappings in Universal).value.filter {
      // Remove lib/ directory from uberjar (aka sbt unmanaged dependencies)
      case(jar, _) => jar.getParentFile != unmanagedBase.value
    } ++ Seq(
      file("src/templates/application.conf") -> "conf/application.conf",
      file("src/templates/logback.xml") -> "conf/logback.xml"
    ),

    // Build
    scalaVersion := ScalaVersion,
    scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
    javacOptions := Seq("-source", "1.8", "-target", "1.8", "-Xlint"),

    // Build info
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := organization.value,
    buildInfoOptions += BuildInfoOption.ToJson,

    // Dependencies
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,

      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,

      "org.scodec" %% "scodec-core" % "1.10.3",

      "org.elasticsearch.client" % "transport" % ElasticsearchVersion,
      "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.8.2",

      "org.scaldi" %% "scaldi" % ScaldiVersion,
      "org.scaldi" %% "scaldi-akka" % ScaldiVersion,
      "org.clapper" %% "classutil" % "1.1.2",

      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",

      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % "test",
      "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % "test",

      "org.mockito" % "mockito-core" % "1.10.19" % "test",
      "org.scalatest" %% "scalatest" % "3.0.3" % "test"
    )

  )
