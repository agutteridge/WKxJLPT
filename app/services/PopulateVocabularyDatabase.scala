import java.sql.Connection

import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.scraper.ContentExtractors._
import org.postgresql.util.PSQLException

object PopulateVocabularyDatabase {

  def run: Unit = {
    val jv = new JLPTvocab // TODO use Guice

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
