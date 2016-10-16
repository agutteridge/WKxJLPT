import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Element
import java.sql.DriverManager
import java.sql.Connection
import scala.collection.mutable.ListBuffer
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

  def listTables(conn: Connection): List[String] = {
    val statement = conn.createStatement()

    val listOfTables = statement.executeQuery(
      """SELECT table_name
        |FROM information_schema.tables
        |WHERE table_schema='public'
        |AND table_type='BASE TABLE';""".stripMargin)

    val resultList = new ListBuffer[String]()

    while (listOfTables.next()) {
      resultList += listOfTables.getString("table_name")
    }

    statement.close()
    resultList.toList
  }

  def createTable(conn: Connection): Unit = {
    // if vocab table exists, drop table
    val selectStatement = conn.createStatement()
    val listOfTables = selectStatement.executeQuery(
      """SELECT table_name
        |FROM information_schema.tables
        |WHERE table_schema='public'
        |AND table_type='BASE TABLE';""".stripMargin)

    while (listOfTables.next()) {
      if (listOfTables.getString("table_name") == "vocab") {
        val dropStatement = conn.createStatement()
        println("Dropping vocab table...")
        dropStatement.executeUpdate("DROP TABLE vocab")
        dropStatement.close()
      }
    }
    selectStatement.close()

    // create vocab table
    val createStatement = conn.createStatement()
    createStatement.executeUpdate(
      """CREATE TABLE vocab (
        |kanji      varchar(10)   PRIMARY KEY,
        |furigana   varchar(80)   NOT NULL,
        |jlpt       int           NOT NULL,
        |meanings   varchar(250)  NOT NULL,
        |jisho_url  varchar(80)   NOT NULL);""".stripMargin)
    createStatement.close()
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
  def insertRow(conn: Connection, jrow: JLPTrow): Unit = {
    val kanji = jrow.kanji
    val furigana = jrow.furigana
    val jlpt = jrow.jlptLevel
    val meanings = jrow.meanings
    val url = jrow.jishoURL

    val statement = conn.createStatement()
    val sqlString = s"""INSERT INTO vocab VALUES
                        |('$kanji',
                        |'$furigana',
                        |'$jlpt',
                        |'$meanings',
                        |'$url')""".stripMargin

    statement.executeUpdate(sqlString)
  }

  def run: Unit = {
    // searching for level 1 returns words from all levels including JLPT-N1 (most advanced level)
    val url: String = s"http://jisho.org/search/%20%23jlpt-n1?page="
    val jishoIterator = new JishoIterator(url)
    var conn: Connection = null

    try {
      // Set up connection and create vocab table
      conn = setUpConnection()
      createTable(conn)

      while (jishoIterator.hasNext) {
        val allWords: List[Element] = jishoIterator.next >> elementList("div .concept_light")

        for (w <- allWords) {
          val row = createJLPTrow(w)
          insertRow(conn, row)
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
    println("All words added to database")
  }
}

object JLPTscrape extends App {
  val jlptInstance = new JLPTscrape
  jlptInstance.run
}
