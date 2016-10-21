package services

import java.sql.Connection
import scalaj.http._
import com.typesafe.config.ConfigFactory
import spray.json._

case class WkAll(requested_information: List[WkWord])

case class WkWord(character: String, kana: String, meaning: String, level: Int)

object WkJsonProtocol extends DefaultJsonProtocol {
  implicit val wordFormat = jsonFormat4(WkWord)
  implicit val wkFormat = jsonFormat1(WkAll)
}

import WkJsonProtocol._

class WaniKaniVocab extends UsesPostgresJDBC {
  // Creates wanikani table
  def createTable(conn: Connection): Unit = {
    dropIfExists(conn, "wanikani")

    val createStatement = conn.createStatement()
    createStatement.executeUpdate(
      """CREATE TABLE wanikani (
        |full_word    varchar(20)   PRIMARY KEY,
        |wk_furigana  varchar(80)   NOT NULL,
        |wk_meaning   varchar(250)  NOT NULL,
        |wk_level     int           NOT NULL);""".stripMargin)
    createStatement.close()
  }

  // Insert row into db
  def insertRow(conn: Connection, wkRow: WKrow): Unit = {
    val fullWord: String = wkRow.fullWord
    val furigana: String = wkRow.furigana
    val meaning: String = wkRow.meaning
    val level: Int = wkRow.wkLevel

    val statement = conn.createStatement()
    val sqlString = s"""INSERT INTO wanikani VALUES
                        |('$fullWord',
                        |'$furigana',
                        |'$meaning',
                        |'$level')""".stripMargin

    statement.executeUpdate(sqlString)
    statement.closeOnCompletion()
  }

  def getVocab(level: Int): List[WkWord] = {
    val apiKey = ConfigFactory.load().getString("values.waniKaniAPIKey")
    val url = s"https://www.wanikani.com/api/user/$apiKey/vocabulary/$level"
    val response: HttpResponse[String] = Http(url).asString
    val json = response.body
    val jsonAst = json.parseJson
    jsonAst.convertTo[WkAll].requested_information
  }
}
