import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Element
import java.sql.Connection
import org.postgresql.util.PSQLException

/* Inherits setUpConnection and listTables */
class JLPTvocab extends usesPostgresJDBC {

  // Creates vocab table
  def createTable(conn: Connection): Unit = {
    val selectStatement = conn.createStatement()
    val listOfTables = selectStatement.executeQuery(
      """SELECT table_name
        |FROM information_schema.tables
        |WHERE table_schema='public'
        |AND table_type='BASE TABLE';""".stripMargin)

    // if vocab table exists, drop table
    while (listOfTables.next()) {
      if (listOfTables.getString("table_name") == "vocab") {
        val dropStatement = conn.createStatement()
        println("Dropping vocab table...")
        dropStatement.executeUpdate("DROP TABLE vocab")
        dropStatement.close()
      }
    }
    selectStatement.close()

    val createStatement = conn.createStatement()
    createStatement.executeUpdate(
      """CREATE TABLE vocab (
        |full_word      varchar(10)   PRIMARY KEY,
        |furigana   varchar(80)   NOT NULL,
        |jlpt       int           NOT NULL,
        |meanings   varchar(250)  NOT NULL,
        |jisho_url  varchar(80)   NOT NULL);""".stripMargin)
    createStatement.close()
  }

  // Extracts JLPT level from text in HTML document
  def getJLPTlevel(wordElement: Element): Integer = {
    val fullString: String = wordElement >> text(".concept_light-status")
    val jlptPattern = """.*JLPT N(\d).*""".r
    val jlptLevel: Integer = fullString match {
      case jlptPattern(level) => level.toInt
      case _ => 0
    }
    jlptLevel
  }

  // Formats meanings so that they are numbered, and separated by newlines
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

  // Instantiates JLPTrow object
  def createJLPTrow(wordElement: Element): JLPTrow = {
    val fullWord: String = wordElement >> text(".text")
    val furigana: String = wordElement >> text(".furigana")
    val jlptLevel: Integer = getJLPTlevel(wordElement)
    val meanings: String = concatMeanings(wordElement)
    val jishoURL: String = wordElement >> element(".light-details_link") >> attr("href")("a")

    new JLPTrow(fullWord, furigana, jlptLevel, meanings, jishoURL)
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
}