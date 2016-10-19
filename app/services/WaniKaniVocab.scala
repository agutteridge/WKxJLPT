import java.sql.Connection
import scalaj.http._
import spray.json._
import com.typesafe.config.ConfigFactory

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
        |full_word        varchar(20)   PRIMARY KEY,
        |furigana         varchar(80)   NOT NULL,
        |meaning          varchar(250)  NOT NULL,
        |wanikani_level   int           NOT NULL);""".stripMargin)
    createStatement.close()
  }

  // Insert row into db
  def insertRow(conn: Connection, jrow: JLPTrow): Unit = {
    val fullWord = jrow.fullWord
    val furigana = jrow.furigana
    val jlpt = jrow.jlptLevel
    val meanings = jrow.meanings
    val url = jrow.jishoURL

    val statement = conn.createStatement()
    val sqlString = s"""INSERT INTO vocab VALUES
                        |('$fullWord',
                        |'$furigana',
                        |'$jlpt',
                        |'$meanings',
                        |'$url')""".stripMargin

    statement.executeUpdate(sqlString)
  }


  def run: Unit = {
    val apiKey = ConfigFactory.load().getString("values.waniKaniAPIKey")

    // iterate through levels 1-60
    for (level <- Range(1, 61)) {
      val url = s"https://www.wanikani.com/api/user/$apiKey/vocabulary/$level"
      val response: HttpResponse[String] = Http(url).asString
      val json = response.body
      val jsonAst = json.parseJson
      val result = jsonAst.convertTo[WkAll]
      // TODO: add to database
//      result.requested_information(0).toString()
    }
  }
}
