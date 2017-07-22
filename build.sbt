import java.time.LocalDateTime.now
import java.time.format.{DateTimeFormatter => DTF}

import com.typesafe.sbt.packager.archetypes.ServerLoader
import DebianConstants._

val ScalaVersion = "2.11.11"
val AkkaVersion = "2.4.19"
val AkkaHttpVersion = "10.0.9"
val ElasticsearchVersion = "5.0.1"

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
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,

    libraryDependencies += "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,

    libraryDependencies += "org.scodec" %% "scodec-core" % "1.10.3",

    libraryDependencies += "org.elasticsearch.client" % "transport" % ElasticsearchVersion,
    libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.7",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.7",

    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",

    libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % "test",
    libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % "test",

    libraryDependencies += "org.mockito" % "mockito-core" % "1.10.19" % "test",
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  )
