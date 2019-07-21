import java.time.LocalDateTime.now
import java.time.format.{DateTimeFormatter => DTF}
import scala.sys.process._

import com.typesafe.sbt.packager.archetypes.systemloader.ServerLoader.Systemd
import DebianConstants._

val ScalaVersion = "2.12.8"
val AkkaVersion = "2.5.23"
val AkkaHttpVersion = "10.1.9"
val ScaldiVersion = "0.5.8"
val PlayVersion = "2.7.3"

lazy val homemote = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  enablePlugins(JavaServerAppPackaging, DebianPlugin, SystemdPlugin, JDebPackaging, ClasspathJarPlugin).
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
    serverLoading in Debian := Some(Systemd),
    debianPackageDependencies in Debian ++= Seq("java8-runtime", "librxtx-java"),
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

      "org.scodec" %% "scodec-core" % "1.11.4",

      "com.typesafe.play" %% "play-jdbc" % PlayVersion,
      "com.typesafe.play" %% "play-jdbc-evolutions" % PlayVersion,
      "org.playframework.anorm" %% "anorm" % "2.6.4",
      "org.postgresql" % "postgresql" % "42.2.6",

      "org.scaldi" %% "scaldi" % ScaldiVersion,
      "org.scaldi" %% "scaldi-akka" % ScaldiVersion,

      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",

      // Tests
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % Test,

      "org.mockito" % "mockito-core" % "2.28.2" % Test,
      "org.scalatest" %% "scalatest" % "3.0.8" % Test
    ),

    // Other
    parallelExecution in Test := false
  )
