import java.sql.{Connection, DriverManager}
import scala.concurrent.Future
import javax.inject._
import com.typesafe.config.ConfigFactory
import play.api.inject.ApplicationLifecycle
import scala.collection.mutable.ListBuffer

trait TPostgresConnection {
  val driver: String
  val url: String
  val username: String
  val password: String
  def get(): Connection
}

@Singleton
class PostgresConnection @Inject() (lifecycle: ApplicationLifecycle) extends TPostgresConnection {
  val driver = ConfigFactory.load().getString("db.jlpt.driver")
  val url = ConfigFactory.load().getString("db.jlpt.url")
  val username = ConfigFactory.load().getString("db.jlpt.username")
  val password = ConfigFactory.load().getString("db.jlpt.password")

  def get(): Connection = {
    var conn: Connection = null

    // Closes connection when the application stops
    lifecycle.addStopHook { () =>
      Future.successful(conn.close())
    }

    try {
      Class.forName(driver)
      conn = DriverManager.getConnection(url, username, password)
    } catch {
      case cnf: ClassNotFoundException =>
        println("Driver not loaded properly.")
        cnf.printStackTrace()
      case default: Throwable => default.printStackTrace()
    }

    // Return the connection or throw an Exception
    if (conn != null) {
      conn
    } else {
      throw new Exception("Error in setUpConnection")
    }
  }
}

class UsesPostgresJDBC {
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

