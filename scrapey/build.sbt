name := "scrapey"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "net.ruippeixotog" %% "scala-scraper" % "1.1.0",
  "org.scalactic" %% "scalactic" % "3.0.0",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.mockito" % "mockito-all" % "1.8.4",
  "io.spray" %%  "spray-json" % "1.3.2",
  "org.scalaj" %% "scalaj-http" % "2.3.0"
)
