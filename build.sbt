name := "play-scala"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.postgresql" % "postgresql" % "9.4-1200-jdbc41",
  "net.ruippeixotog" %% "scala-scraper" % "1.1.0",
  "io.spray" %%  "spray-json" % "1.3.2",
  "org.scalaj" %% "scalaj-http" % "2.3.0"
)

