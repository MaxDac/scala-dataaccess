import java.sql._

import eng.db.dataaccess.Oracle.OracleDbCommandDialect
import eng.db.dataaccess.base.Enums._
import eng.db.dataaccess.base.{DbCommand, _}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}
// import eng.db.dataaccess.utils.Stopwatch
import org.scalatest._

//noinspection NameBooleanParameters
class HelperMethodsSpec extends AsyncFlatSpec {

    private implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
    private implicit val dbDialect: DbCommandDialect = OracleDbCommandDialect()

    "The procedure" should "perform the call correctly" in {
        import DbCommand._

        val command = DbCommand("CRD_SERVICES_TESTS_PCK.SIMPLE_TEST",
            (DbTypeString, "FIRST_PARAMETER", "Test1"),
            (DbTypeInt, "SECOND_PARAMETER", 2),
            (DbTypeString, "THIRD_PARAMETER", "Test3", InputOutput))

        DataAccessComponent.scope(dac => dac.execute(command)) transformWith {
            case Success(_) => assert(true)
            case Failure(error) => assert(false, error.toString)
        }
    }

    "The procedure" should "perform the call correctly with return out parameter" in {
        import DbCommand._

        val command = DbCommand("CRD_SERVICES_TESTS_PCK.SIMPLE_TEST",
            (DbTypeString, "FIRST_PARAMETER", "Test1"),
            (DbTypeInt, "SECOND_PARAMETER", 2),
            (DbTypeString, "THIRD_PARAMETER", "Test3", InputOutput))

        DataAccessComponent.scope { dac =>
            dac.execute(command, (statement: CallableStatement) => statement.getString("THIRD_PARAMETER"))
        } transformWith {
            case Success(result) => assert(result === "Test")
            case Failure(ex) => assert(false, ex.toString)
        }
    }

    "The procedure" should "show the error of the test procedure" in {
        import DbCommand._

        val command = DbCommand("CRD_SERVICES_TESTS_PCK.SIMPLE_TEST_WITH_ERROR",
            (DbTypeString, "FIRST_PARAMETER", "Test1"),
            (DbTypeInt, "SECOND_PARAMETER", 2),
            (DbTypeString, "THIRD_PARAMETER", "Test3", InputOutput))

        DataAccessComponent.scope { dac =>
            dac.execute(command, (statement: CallableStatement) => statement.getString("THIRD_PARAMETER"))
        } transformWith {
            case Success(_) => assert(false, "The procedure call should have returned an error.")
            case Failure(error) => assert(error.toString.contains("Generic error"))
        }
    }

    case class TestItem(
                       code: String,
                       amount: BigDecimal
                       )

    "The procedure" should "return the cursor with the data" in {
        import DbCommand._

        val command = DbCommand("CRD_SERVICES_TESTS_PCK.SIMPLE_TEST_WITH_CURSOR",
            (DbTypeString, "FIRST_PARAMETER", "Test1"),
            (DbTypeInt, "SECOND_PARAMETER", 2),
            (DbTypeString, "THIRD_PARAMETER", "Test3", InputOutput),
            (DbTypeCursor, "FOURTH_CURSOR", Output, true))

        DataAccessComponent.scope { dac =>
            dac.fetch(command, (rs: ResultSet) => TestItem(
                rs.getString("Codigo"),
                rs.getBigDecimal("Importe")
            ))
        } map { res =>
            assert(res.length === 1)
            assert(res.head.code === "013932402")
            assert(res.head.amount === 104.50)
        }
    }
}
