import scalaj.http._
import spray.json._
import DefaultJsonProtocol._
import com.typesafe.config.ConfigFactory

//case class WkWord(fullWord: String, furigana: String, meaning: String, level: Integer)
//
//object WkJsonProtocol extends DefaultJsonProtocol {
//  implicit val wkFormat = jsonFormat4(WkWord)
//}

object WKscrape extends App {
  val apiKey = ConfigFactory.load().getString("values.waniKaniAPIKey")

  // iterate through levels 1-60
    val level = 1
  //  for (level <- Range(1, 61)){
    val url = s"https://www.wanikani.com/api/user/$apiKey/vocabulary/$level"
    val response: HttpResponse[String] = Http(url).asString
    val json = response.body
    val jsonAst = json.parseJson // or JsonParser(source)
  println(jsonAst)

  // TODO: result is one big blog of JSON, how to parse??

//    val astList = jsonAst.convertTo[List[String]]
//    for (word <- astList) {
//      word.convertTo
//    }
//  }
}
