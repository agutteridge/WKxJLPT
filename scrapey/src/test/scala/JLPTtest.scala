import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.Element
import org.scalatest._

import scala.collection.mutable.Stack

class JLPTtest extends FlatSpec with Matchers {
  "GetFurigana" should "return the expected output てんのう" in {
    val browser = JsoupBrowser()
    val doc = browser.parseFile("/Users/alicegutteridge/Dev/WKxJLPT/scrapey/src/test/resources/JishoExample.html")
    val allWords: List[Element] = doc >> elementList("div .concept_light-representation")

    val jlptInstance = new JLPTscrape
    val result = ???

    result should equal ("てんのう")
  }
}
