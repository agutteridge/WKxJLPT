package services

import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Element

class Row {
  def escape(str: String): String = {
    str.replace("\'", "\'\'")
  }
}

class JLPTrow(wordElement: Element) extends Row {
  val fullWord: String = wordElement >> text(".text")
  val furigana: String = wordElement >> text(".furigana")
  val jlptLevel: Integer = getJLPTlevel
  val meanings: String = escape(concatMeanings)
  val jishoURL: String = wordElement >> element(".light-details_link") >> attr("href")("a")

  // Extracts JLPT level from text in HTML document
  private[this] def getJLPTlevel: Integer = {
    val fullString: String = wordElement >> text(".concept_light-status")
    val jlptPattern = """.*JLPT N(\d).*""".r
    val jlptLevel: Integer = fullString match {
      case jlptPattern(level) => level.toInt
      case _ => 0
    }
    jlptLevel
  }

  // Formats meanings so that they are numbered, and separated by newlines
  private[this] def concatMeanings: String = {
    val meanings: List[Element] = wordElement >> elementList(".meaning-meaning")
    val meaningsList: List[String] = meanings.map(_ >> text(".meaning-meaning"))
      .distinct // Meanings should be unique
    val numberList = Range(1, meaningsList.length).toList // Creates list of numbers
        .map(x => x.toString + ". ")
        .zip(meaningsList.map(m => m + "\n")) // Zips numbers with meanings
        .flatMap(t => List(t._1, t._2)) // Flattens list of tuples to single-level list

    if (numberList.length > 1) { // cannot reduce 1-element list
      numberList.reduce(_+_).trim()
    } else {
      numberList.toString.trim()
    }
  }

  override def toString(): String = {
    s"{$fullWord\n $furigana\nJLPT-N$jlptLevel\n$meanings\n$jishoURL\n}"
  }

  println("full_word from Jisho.org: " + fullWord)
  require(fullWord.length > 0 || furigana.length > 0, "No word provided")
  require(1 <= jlptLevel && jlptLevel <= 5, "JLPT level outside of range")
  require(meanings.length() > 0, "No meaning provided")
  require(jishoURL.length() > 15, "Incomplete URL provided") // longer than "jisho.org/word/"
}

class WKrow(wkWord: WkWord) extends Row {
  val fullWord: String = wkWord.character
  val furigana: String = wkWord.kana
  val meaning: String = escape(wkWord.meaning)
  val wkLevel: Int = wkWord.level

  println("full_word from WaniKani: " + fullWord)
  require(fullWord.length > 0 || furigana.length > 0, "No word provided")
  require(1 <= wkLevel && wkLevel <= 60, "WaniKani level outside of range")
  require(meaning.length() > 0, "No meaning provided")
}