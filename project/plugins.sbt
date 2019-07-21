addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.25")
libraryDependencies += "org.vafer" % "jdeb" % "1.7" artifacts Artifact("jdeb", "jar", "jar")
