import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.Element
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._

class JLPTtest extends FlatSpec with Matchers with MockitoSugar {

  val document = mock[JsoupBrowser] //don't need this yet

  // TODO: make into fixture or something ?
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
}

