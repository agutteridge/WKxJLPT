import scalaj.http._
import spray.json._
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

case class WkAll(requested_information: List[WkWord])

case class WkWord(character: String, kana: String, meaning: String, level: Int)

object WkJsonProtocol extends DefaultJsonProtocol {
  implicit val wordFormat = jsonFormat4(WkWord)
  implicit val wkFormat = jsonFormat1(WkAll)
}

import WkJsonProtocol._

class WaniKaniVocabulary {
  val datetime = DateTime.now.toString

  def run: Unit = {
    val apiKey = ConfigFactory.load().getString("values.waniKaniAPIKey")

    // iterate through levels 1-60
    for (level <- Range(1, 61)) {
      val url = s"https://www.wanikani.com/api/user/$apiKey/vocabulary/$level"
      val response: HttpResponse[String] = Http(url).asString
      val json = response.body
      val jsonAst = json.parseJson
      val result = jsonAst.convertTo[WkAll]
      // TODO: add to database
      println(result.requested_information(0))
    }
  }
}
