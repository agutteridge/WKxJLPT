import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Element
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._

class JLPTtest extends FlatSpec with Matchers with MockitoSugar {

  val document = mock[JsoupBrowser] //don't need this yet

  // make into fixture or something ?
  def firstWord: Element = {
    val browser = JsoupBrowser()
    val doc = browser.parseFile("/Users/alicegutteridge/Dev/WKxJLPT/scrapey/src/test/resources/JishoExample.html")
    val allWords: List[Element] = doc >> elementList("div .concept_light")
    allWords.head
  }

  "ConcatMeanings" should "return the expected output" in {
    val jlptInstance = new JLPTscrape
    val result = jlptInstance.concatMeanings(firstWord)

    result should equal ("1. Emperor of Japan\n2. Tennou")
  }

  "getJLPTlevel" should "return the expected output" in {
    val browser = JsoupBrowser()
    val doc = browser.parseFile("/Users/alicegutteridge/Dev/WKxJLPT/scrapey/src/test/resources/JishoExample.html")
    val allWords: List[Element] = doc >> elementList("div .concept_light")
    val jlptInstance = new JLPTscrape
    val result = jlptInstance.getJLPTlevel(firstWord)

    result should equal (2)
  }

  "Creating a JLPTrow instance" should "return the expected output" in {
    val browser = JsoupBrowser()
    val doc = browser.parseFile("/Users/alicegutteridge/Dev/WKxJLPT/scrapey/src/test/resources/JishoExample.html")
    val allWords: List[Element] = doc >> elementList("div .concept_light")
    val jlptInstance = new JLPTscrape
    val result = jlptInstance.createJLPTrow(firstWord)

    result.kanji should equal ("天皇")
    result.furigana should equal ("てんのう")
    result.jishoURL should equal ("http://jisho.org/word/%E5%A4%A9%E7%9A%87")
  }

  "SetUpConnection" should "return a functioning connection to PostgreSQL" in {
    val jlptInstance = new JLPTscrape
    val conn = jlptInstance.setUpConnection()
    conn.getCatalog should equal ("jlpt")
  }

  "CreateTable" should "create vocab table in jlpt db" in {
    val jlptInstance = new JLPTscrape
    val conn = jlptInstance.setUpConnection()
    jlptInstance.createTable(conn)
    jlptInstance.listTables(conn) should contain ("vocab")
  }

  "InsertRow" should "insert row into vocab table in jlpt db" in {
    val jlptInstance = new JLPTscrape
    val conn = jlptInstance.setUpConnection()
    jlptInstance.createTable(conn)
    val row = jlptInstance.createJLPTrow(firstWord)
    jlptInstance.insertRow(conn, row)
    jlptInstance.listTables(conn) should contain ("vocab")

    val statement = conn.createStatement()
    val result = statement.executeQuery("SELECT * FROM vocab")
    while (result.next()) {
      result.getString("kanji") should be ("天皇")
      result.getString("furigana") should be ("てんのう")
      result.getString("meanings") should be ("1. Emperor of Japan\n2. Tennou")
      result.getInt("jlpt") should be (2)
      result.getString("jisho_url") should be ("http://jisho.org/word/%E5%A4%A9%E7%9A%87")
    }
  }

  "hasNext in JishoIterator" should "return true if `more` class available in document" in {
    val iterator = new JishoIterator("http://jisho.org/search/seaweed?page=")
    iterator.hasNext should be (true)
  }

  "hasNext in JishoIterator" should "return false if `more` class not present in document" in {
    val iterator = new JishoIterator("http://jisho.org/search/headphones?page=")
    iterator.hasNext should be (false)
  }
}

