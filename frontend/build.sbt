 name := """basicframework"""

version := "1.6.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += "commons-io" % "commons-io" % "2.4"
libraryDependencies += "org.projectlombok" % "lombok" % "1.16.10"
libraryDependencies += "com.google.code.gson" % "gson" % "2.2.2"
libraryDependencies += "com.squareup.okhttp3" % "okhttp" % "4.12.0"

// Test Database
libraryDependencies += "javax.inject" % "javax.inject" % "1"

libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.12.772"

//parse http parameters
libraryDependencies += "org.apache.httpcomponents" % "httpcore"  % "4.4.1"
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.2"


libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.17.0"
libraryDependencies += "commons-codec" % "commons-codec" % "1.10"
libraryDependencies += "org.jsoup" % "jsoup" % "1.11.3"
libraryDependencies += "com.itextpdf" % "itext7-core" % "7.2.5"

// Common
libraryDependencies += "org.apache.commons" % "commons-text" % "1.1"

// Java parser
libraryDependencies += "com.github.javaparser" % "javaparser-core" % "3.6.5"
libraryDependencies += "com.github.javaparser" % "javaparser-symbol-solver-core" % "3.6.5"

// Make verbose tests
testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v"))

javaOptions ++= Seq("-Xmx2048M", "-Xms512M", "-XX:MaxPermSize=2048M")

sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

PlayKeys.devSettings := Seq("play.akka.dev-mode.akka.http.parsing.max-uri-length" -> "20480")

PlayKeys.devSettings += "play.server.http.port" -> "9036"
