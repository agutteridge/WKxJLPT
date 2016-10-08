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

    var connection: Connection = null

    try {
      Class.forName(driver)
      connection = DriverManager.getConnection(url, username, password)
    } catch {
      case cnf: ClassNotFoundException =>
        println("Driver not loaded properly.")
        cnf.printStackTrace()
      case default: Throwable => default.printStackTrace()
    }

    if (connection != null) {
      return connection
    } else {
      throw new Exception("Error in setUpConnection")
    }
  }

  def run {

    val browser = JsoupBrowser()

    // Iterate through levels N1 - N5
    for(level <- 1 to 5){
      val url: String = s"http://jisho.org/search/%20%23jlpt-n$level"
      //TODO: iterate through pages until no results on page
      try {
        val doc = browser.get(url)
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
          //TODO: put into DB
          for(k <- kanjiDivs){
            k match {
              case Some(s) => println(s.text)
              case None => // do nothing
            }
          }
        }
      } catch {
        case notFound: org.jsoup.HttpStatusException => println(notFound)
      }
    }

    var connection: Connection = null

    try {
      connection = setUpConnection()
      val statement = connection.createStatement()
      //TODO: Create table with correct columns
      val resultSet = statement.executeQuery("SELECT * FROM vocab")
      while (resultSet.next()) {
        val word = resultSet.getString("word")
        println(word)
      }
    } catch {
      case psql: PSQLException =>
        println("Problem with connection.")
        psql.printStackTrace()
      case default: Throwable => default.printStackTrace()

    } finally {
      connection.close()
    }

  }
}

object JLPTscrape extends App {
  val jlptInstance = new JLPTscrape
  jlptInstance.run
}