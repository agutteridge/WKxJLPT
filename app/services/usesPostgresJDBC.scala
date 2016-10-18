import java.sql.{Connection, DriverManager}

import scala.collection.mutable.ListBuffer

class usesPostgresJDBC {

  // Connect to the database named "jlpt" on the localhost
  def setUpConnection(): Connection = {
    val driver = "org.postgresql.Driver"
    val url = "jdbc:postgresql://localhost/jlpt"
    val username = "alicegutteridge"
    val password = "root"

    var conn: Connection = null

    try {
      Class.forName(driver)
      conn = DriverManager.getConnection(url, username, password)
    } catch {
      case cnf: ClassNotFoundException =>
        println("Driver not loaded properly.")
        cnf.printStackTrace()
      case default: Throwable => default.printStackTrace()
    }

    if (conn != null) {
      conn
    } else {
      throw new Exception("Error in setUpConnection")
    }
  }

  // List all table names in database
  def listTables(conn: Connection): List[String] = {
    val statement = conn.createStatement()

    val listOfTables = statement.executeQuery(
      """SELECT table_name
        |FROM information_schema.tables
        |WHERE table_schema='public'
        |AND table_type='BASE TABLE';""".stripMargin)

    val resultList = new ListBuffer[String]()

    while (listOfTables.next()) {
      resultList += listOfTables.getString("table_name")
    }

    statement.close()
    resultList.toList
  }
}

