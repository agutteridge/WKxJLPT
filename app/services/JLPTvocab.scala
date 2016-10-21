package services

import java.sql.Connection

/* Inherits setUpConnection and listTables */
class JLPTvocab extends UsesPostgresJDBC {

  // TODO: define schema in another file! Use Evolutions! Use Slick!
  // Creates vocab table
  def createTable(conn: Connection): Unit = {
    dropIfExists(conn, "vocab")

    val createStatement = conn.createStatement()
    createStatement.executeUpdate(
      """CREATE TABLE vocab (
        |full_word        varchar(20)    PRIMARY KEY,
        |jisho_furigana   varchar(80)    NOT NULL,
        |jlpt_level       int            NOT NULL,
        |jisho_meanings   varchar(1000)  NOT NULL,
        |jisho_url        varchar(80)    NOT NULL);""".stripMargin)
    createStatement.close()
  }

  // Insert row into db
  def insertRow(conn: Connection, jrow: JLPTrow): Unit = {
    val fullWord = jrow.fullWord
    val furigana = jrow.furigana
    val jlpt = jrow.jlptLevel
    val meanings = jrow.meanings
    val url = jrow.jishoURL

    val statement = conn.createStatement()
    val sqlString = s"""INSERT INTO vocab VALUES
                        |('$fullWord',
                        |'$furigana',
                        |'$jlpt',
                        |'$meanings',
                        |'$url')""".stripMargin

    try {
      statement.executeUpdate(sqlString)
      statement.closeOnCompletion()
    } catch {
//      case unknown: ArrayIndexOutOfBoundsException =>
//        val noMeaning = s"""INSERT INTO vocab VALUES
//                            |('$fullWord',
//                            |'$furigana',
//                            |'$jlpt',
//                            |'',
//                            |'$url')""".stripMargin
//        conn.createStatement().executeUpdate(noMeaning)
//        statement.closeOnCompletion()
      case default: Exception => println(jrow.toString); throw default
    }
  }
}