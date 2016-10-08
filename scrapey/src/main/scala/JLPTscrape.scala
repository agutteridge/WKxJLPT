import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.Element

object JLPTscrape extends App {
  val browser = JsoupBrowser()

  // Iterate through levels N1 - N5
  for(level <- 1 to 5){
      val url: String = s"http://jisho.org/search/%20%23jlpt-n$level"
      //TODO: iterate through pages until no results on page
      try {
        val doc = browser.get(url)
        val allDivs: List[Element] = doc >> elementList("div .concept_light-representation")
        val kanjiDivs: List[Option[Element]] = allDivs.map(_ >?> element(".text"))
        for(k <- kanjiDivs){
          k match {
            case Some(s) => println(s.text)
            case None => // do nothing
          }
        }
      } catch {
        case notFound: org.jsoup.HttpStatusException => println(notFound)
      }
  }
}