package services

import java.sql.Connection
import javax.inject._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Element
import play.api.inject.guice.GuiceApplicationBuilder

class PopulateVocabularyDatabase @Inject() (postgresConnection: TPostgresConnection) {

  // Jisho: iterate through all pages
  // searching for level 1 returns words from all levels including JLPT-N1 (most advanced level)
  def jisho(conn: Connection): Unit = {
    val jv = new JLPTvocab
    jv.createTable(conn)
    val url: String = s"http://jisho.org/search/%23jlpt-n1?page="
    val jishoIterator = new JishoIterator(url)
    var fullWordSet: Set[String] = () // full_word is primary key

    while (jishoIterator.hasNext) {
      val allWords: List[Element] = jishoIterator.next >> elementList("div .concept_light")

      for (w <- allWords) {
        val row = new JLPTrow(w)
        if (!fullWordSet(row.fullWord)) {
          jv.insertRow(conn, row)
        }
        fullWordSet += row.fullWord
      }
    }
  }

  // WaniKani: iterate through levels 1-60
  def wanikani(conn: Connection): Unit = {
    val wv = new WaniKaniVocab
    wv.createTable(conn)

    for (level <- Range(1, 61)) {
      val wordList: List[WkWord] = wv.getVocab(level)
      for (w <- wordList) {
        wv.insertRow(conn, new WKrow(w))
      }
    }
  }

  // Full outer join to create all_vocab view
  def outerJoin(conn: Connection): Unit = {
    val joinStatement = conn.createStatement()
    joinStatement.executeUpdate(
      """CREATE VIEW all_vocab
        |AS SELECT *
        |FROM jlpt
        |FULL OUTER JOIN ON jlpt.full_word = wanikani.full_word""".stripMargin)
    joinStatement.closeOnCompletion()
  }

  def run(): Unit = {
    var conn: Connection = null

    try {
      conn = postgresConnection.get()
      jisho(conn)
      wanikani(conn)
      outerJoin(conn)
    } catch {
      case notFound: org.jsoup.HttpStatusException => println(notFound)
      case default: Throwable => default.printStackTrace()
    } finally {
      conn.close()
    }
    println("All words added to database")
    // TODO: find max level of kanji in full_word
  }
}

object OPopulate extends App {
  val postgresConn = (new GuiceApplicationBuilder).injector().instanceOf[PostgresConnection]
  val p = new PopulateVocabularyDatabase(postgresConn)
  p.run()
}