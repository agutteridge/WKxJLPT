import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Element
import org.scalatest._
import play.api.inject.guice.GuiceApplicationBuilder
import scala.reflect.ClassTag

/* SBT will not run test classes with args.
   Dependencies must be injected using GuiceApplicationBuilder.
*/
trait Inject {
  lazy val injector = (new GuiceApplicationBuilder).injector()
  def inject[T : ClassTag]: T = injector.instanceOf[T]
}

class JLPTtest extends FlatSpec with Matchers with Inject {
  def firstWord: Element = {
    val browser = JsoupBrowser()
    val doc = browser.parseFile("/Users/alicegutteridge/Dev/WKxJLPT/test/resources/JishoExample.html")
    val allWords: List[Element] = doc >> elementList("div .concept_light")
    allWords.head
  }

  "ConcatMeanings" should "return the expected output" in {
    val jlptRow = new JLPTrow(firstWord)
    jlptRow.meanings should equal ("1. Emperor of Japan\n2. Tennou")
  }

  "getJLPTlevel" should "return the expected output" in {
    val jlptRow = new JLPTrow(firstWord)
    jlptRow.jlptLevel should equal (2)
  }

  "Creating a JLPTrow instance" should "return the expected output" in {
    val jlptRow = new JLPTrow(firstWord)
    jlptRow.fullWord should equal ("天皇")
    jlptRow.furigana should equal ("てんのう")
    jlptRow.jishoURL should equal ("http://jisho.org/word/%E5%A4%A9%E7%9A%87")
  }

  "SetUpConnection" should "return a functioning connection to PostgreSQL" in {
    val conn = inject[PostgresConnection].get()
    conn.getCatalog should equal ("jlpt")
  }

  "CreateTable" should "create vocab table in jlpt db" in {
    val jlptInstance = new JLPTvocab
    val conn = inject[PostgresConnection].get()
    jlptInstance.createTable(conn)
    jlptInstance.listTables(conn) should contain ("vocab")
  }

  "InsertRow" should "insert row into vocab table in jlpt db" in {
    val jlptInstance = new JLPTvocab
    val conn = inject[PostgresConnection].get()
    jlptInstance.createTable(conn)
    val jlptRow = new JLPTrow(firstWord)
    jlptInstance.insertRow(conn, jlptRow)
    val statement = conn.createStatement()
    val result = statement.executeQuery("SELECT * FROM vocab")

    while (result.next()) {
      result.getString("full_word") should be ("天皇")
      result.getString("furigana") should be ("てんのう")
      result.getString("meanings") should be ("1. Emperor of Japan\n2. Tennou")
      result.getInt("jlpt") should be (2)
      result.getString("jisho_url") should be ("http://jisho.org/word/%E5%A4%A9%E7%9A%87")
    }
  }

  // The following two tests rely on results from the Jisho website
  "hasNext in JishoIterator" should "return true if `more` class available in document" in {
    val iterator = new JishoIterator("http://jisho.org/search/seaweed?page=")

    iterator.hasNext should be (true)
  }

  "hasNext in JishoIterator" should "return false if `more` class not present in document" in {
    val iterator = new JishoIterator("http://jisho.org/search/headphones?page=")

    iterator.hasNext should be (false)
  }

//  "WaniKaniVocab.run" should "return a String representation of the word two" in {
//    val waniKaniInstance = new WaniKaniVocab(inject[PostgresConnection])
//    val result = waniKaniInstance.run
//
//    result should be ("WkWord(二,に,two,1)")
//  }
}

