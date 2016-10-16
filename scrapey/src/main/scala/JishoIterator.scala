import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Document
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Element

class JishoIterator(val url: String) extends Iterator[Document] {
  var page = 1
  val browser = JsoupBrowser()

  def hasNext: Boolean = {
    val doc = browser.get(url + page.toString)
    val moreElement: Option[Element] = doc >?> element(".more")
    moreElement match {
      case Some(_) => true
      case None => false
    }
  }

  def next(): Document = {
    val doc = browser.get(url.toString)
    page += 1
    doc
  }
}