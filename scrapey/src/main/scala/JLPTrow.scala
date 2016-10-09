trait Row {
  // Nothing needed yet, maybe string escape?
}

class JLPTrow(kanji: String,
              furigana: String,
              jlptLevel: Integer,
              meanings: String,
              jishoURL: String) extends Row {

  require(kanji.length() > 0, "No kanji provided")
  require(furigana.length() > 0, "No furigana provided")
  require(1 <= jlptLevel && jlptLevel >= 5, "JLPT level outside of range")
  require(meanings.length() > 0, "No meaning provided")
  require(jishoURL.length() > 15, "Incomplete URL provided") // longer than "jisho.org/word/"
}

class WKrow extends Row {
  // TODO
}