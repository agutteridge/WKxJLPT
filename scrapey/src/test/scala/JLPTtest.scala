import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.Element
import org.scalatest._

class JLPTtest extends FlatSpec with Matchers {
  "ConcatMeanings" should "return the expected output" in {
    val browser = JsoupBrowser()
    val doc = browser.parseFile("/Users/alicegutteridge/Dev/WKxJLPT/scrapey/src/test/resources/JishoExample.html")
    // TODO this should be in test as well
    val allWords: List[Element] = doc >> elementList("div .concept_light")
    val jlptInstance = new JLPTscrape
    val result = jlptInstance.concatMeanings(allWords(0))

    result should equal ("1. Emperor of Japan\n2. Tennou")
  }
}
