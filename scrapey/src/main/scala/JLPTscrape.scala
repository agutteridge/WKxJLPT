import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
//import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.Element
import java.sql.DriverManager
import java.sql.Connection

import org.postgresql.util.PSQLException

class JLPTscrape {

  // connect to the database named "jlpt" on the localhost
  def setUpConnection(): Connection = {
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
      conn
    } else {
      throw new Exception("Error in setUpConnection")
    }
  }

  def createTable(conn: Connection): Unit = {
    val statement = conn.createStatement()

    // if vocab table exists, drop table
    val listOfTables = statement.executeQuery(
      """SELECT table_name
        |FROM information_schema.tables
        |WHERE table_schema='public'
        |AND table_type='BASE TABLE';""".stripMargin)
    while (listOfTables.next()) {
      if (listOfTables.getString("table_name") == "vocab") {
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

  def getJLPTlevel(wordElement: Element): Integer = {
    val fullString: String = wordElement >> text(".concept_light-status")
    val jlptPattern = """.*JLPT N(\d).*""".r
    val jlptLevel: Integer = fullString match {
      case jlptPattern(level) => level.toInt
      case _ => 0
    }
    jlptLevel
  }

  def concatMeanings(wordElement: Element): String = {
    val meanings: List[Element] = wordElement >> elementList(".meaning-meaning")
    val meaningsList: List[String] = meanings.map(_ >> text(".meaning-meaning"))
      .distinct // Meanings should be unique
    val numberList = Range(1, meaningsList.length).toList // Creates list of numbers
      .map(x => x.toString + ". ")
      .zip(meaningsList.map(m => m + "\n")) // Zips numbers with meanings
      .flatMap(t => List(t._1, t._2)) // Flattens list of tuples to single-level list
      .reduce((x, y) => x + y) // Concatenates all elements oflist
    numberList.toString.trim() // Get rid of trailing newline
  }

  def createJLPTrow(wordElement: Element): JLPTrow = {
    val kanji: String = wordElement >> text(".text")
    val furigana: String = wordElement >> text(".furigana")
    val jlptLevel: Integer = getJLPTlevel(wordElement)
    val meanings: String = concatMeanings(wordElement)
    val jishoURL: String = wordElement >> element(".light-details_link") >> attr("href")("a")

    new JLPTrow(kanji, furigana, jlptLevel, meanings, jishoURL)
  }

  // Insert row into db
  //TODO: put into DB
  def insertRow(conn: Connection, j: JLPTrow): Unit = {
    val statement = conn.createStatement()
    val resultSet = statement.executeQuery("SELECT * FROM vocab")
    while (resultSet.next()) {
      val word = resultSet.getString("word")
      println(word)
    }
  }

  def run: Unit = {
    val browser = JsoupBrowser()
    var conn: Connection = null
    // searching for level 1 returns words from all levels including JLPT-N1 (most advanced level)
    val url: String = s"http://jisho.org/search/%20%23jlpt-n1"
    // TODO: iterate through pages until no results on page
    try {
      // Set up connection and create vocab table
      conn = setUpConnection()
//        createTable(conn)

      val doc = browser.get(url)
      val allWords: List[Element] = doc >> elementList("div .concept_light")

      // An empty list indicates there are no more results
      for (w <- allWords) {
        val row = createJLPTrow(w)
        val success = insertRow(conn, row)
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
