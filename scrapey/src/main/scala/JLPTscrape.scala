import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.Element
import java.sql.DriverManager
import java.sql.Connection

import org.postgresql.util.PSQLException

class JLPTscrape {

  def setUpConnection(): Connection = {
    // connect to the database named "jlpt" on the localhost
    val driver = "org.postgresql.Driver"
    val url = "jdbc:postgresql://localhost/jlpt"
    val username = "alicegutteridge"
    val password = "root"

    var conn: Connection = null

    try {
      Class.forName(driver)
      conn = DriverManager.getConnection(url, username, password)
    } catch {
      case cnf: ClassNotFoundException =>
        println("Driver not loaded properly.")
        cnf.printStackTrace()
      case default: Throwable => default.printStackTrace()
    }

    if (conn != null) {
      return conn
    } else {
      throw new Exception("Error in setUpConnection")
    }
  }

  def createTable(conn: Connection) = {
    val statement = conn.createStatement()

    // if vocab table exists, drop table
    val listOfTables = statement.executeQuery(
      """SELECT table_name
        |FROM information_schema.tables
        |WHERE table_schema='public'
        |AND table_type='BASE TABLE';""".stripMargin)
    while (listOfTables.next()) {
      if(listOfTables.getString("table_name") == "vocab") {
        statement.executeQuery("DROP TABLE vocab")
      }
    }

    // create vocab table
    statement.executeQuery(
      """CREATE TABLE vocab (
        |kanji      varchar(10)   PRIMARY KEY,
        |furigana   varchar(80)   NOT NULL,
        |jlpt       int           NOT NULL,
        |meanings   varchar(250)  NOT NULL,
        |jisho_url  varchar(80)   NOT NULL
        |);""".stripMargin)
  }

  def run {
    val browser = JsoupBrowser()
    var conn: Connection = null

    // Iterate through levels N1 - N5
    for (level <- 1 to 5) {
      val url: String = s"http://jisho.org/search/%20%23jlpt-n$level"
      // TODO: iterate through pages until no results on page
      try {
        // Set up connection and create vocab table
        conn = setUpConnection()
        createTable(conn)

        val doc = browser.get(url)
        // TODO: rename var to represent which elements are in this div
        val allDivs: List[Element] = doc >> elementList("div .concept_light-representation")
        /* TODO: Extract more elements useful for db. These are:
           - JLPT level
           - Reading
           - Meaning(s)
           - Link to jisho page
        */

        // An empty list indicates there are no more results
        if (allDivs.nonEmpty) {
          val kanjiDivs: List[Option[Element]] = allDivs.map(_ >?> element(".text"))
          for (k <- kanjiDivs) {
            k match {
              case Some(s) => println(s.text)
              case None => // do nothing
            }

            //TODO: put into DB
            // Insert row into db
            val statement = conn.createStatement()
            val resultSet = statement.executeQuery("SELECT * FROM vocab")
            while (resultSet.next()) {
              val word = resultSet.getString("word")
              println(word)
            }
          }
        }
      } catch {
        case notFound: org.jsoup.HttpStatusException => println(notFound)
        case psql: PSQLException =>
          println("Problem with connection.")
          psql.printStackTrace()
        case default: Throwable => default.printStackTrace()

      } finally {
        conn.close()
      }
    }
  }

object JLPTscrape extends App {
  val jlptInstance = new JLPTscrape
  jlptInstance.run
}