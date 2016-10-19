import java.sql.Connection

import org.postgresql.util.PSQLException
import javax.inject._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Element


class PopulateVocabularyDatabase @Inject() (postgresConnection: TPostgresConnection) {

  def run: Unit = {
    val jv = new JLPTvocab // TODO use Guice
    val wv = new WaniKaniVocab

    // searching for level 1 returns words from all levels including JLPT-N1 (most advanced level)
    val url: String = s"http://jisho.org/search/%20%23jlpt-n1?page="
    val jishoIterator = new JishoIterator(url)
    var conn: Connection = null

    try {
      // Set up connection and create vocab table
      conn = postgresConnection.get()
      jv.createTable(conn)

      while (jishoIterator.hasNext) {
        val allWords: List[Element] = jishoIterator.next >> elementList("div .concept_light")

        for (w <- allWords) {
          val row = new JLPTrow(w)
          jv.insertRow(conn, row)
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
