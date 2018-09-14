import java.sql.Date
import java.util.UUID

import dataaccess.tests.dtos.Payment
import eng.db.dataaccess.Oracle.OracleDbCommandDialect
import eng.db.dataaccess.base.Enums._
import eng.db.dataaccess.base.{DbCommand, _}
import org.scalatest.AsyncFlatSpec

import scala.concurrent.{ExecutionContextExecutor, Future}

//noinspection ScalaDeprecation
class TransactionSpecs extends AsyncFlatSpec {

    private implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
    private implicit val dbDialect: DbCommandDialect = OracleDbCommandDialect()

    "The procedure" should "not commit the transaction" in {
        import DbCommand._

        val paymentCode = "0934023929"
        val command = DbCommand("CRD_SERVICES_TESTS_PCK.SIMPLE_TEST_FOR_TRANSACTION",
            (DbTypeString, "P_ID", paymentCode),
            (DbTypeString, "P_CODE", paymentCode),
            (DbTypeDecimal, "P_AMOUNT", BigDecimal(105.5)),
            (DbTypeTimestamp, "P_DUE_DATE", new Date(2018, 7, 21)))

        val query = Query(() => Payment()) select (
            StringField[Payment]("Id", "Id", (p, id) => p.id = id),
            StringField[Payment]("Code", "Code", (p, code) => p.code = code),
            DecimalField[Payment]("Amount", "Amount", (p, amount) => p.amount = amount)
        ) from "{stream-name}.PAYMENTS" where s"Code = '$paymentCode'"

        (for {
            _ <- DataAccessComponent.transactionScope { dac =>
                dac.execute(command)
            }
            queryResult <- query.perform()
        } yield queryResult) map { result =>
            assert(result.length === 0)
        }
    }

    "The procedure" should "commit the transaction" in {
        import DbCommand._

        val paymentCode = UUID.randomUUID().toString.substring(0, 15)
        val command = DbCommand("CRD_SERVICES_TESTS_PCK.SIMPLE_TEST_FOR_TRANSACTION",
            (DbTypeString, "P_ID", paymentCode),
            (DbTypeString, "P_CODE", paymentCode),
            (DbTypeDecimal, "P_AMOUNT", BigDecimal(105.5)),
            (DbTypeTimestamp, "P_DUE_DATE", new Date(2018, 7, 21)))

        val query = Query(() => Payment()) select (
            StringField[Payment]("Id", "Id", (p, id) => p.id = id),
            StringField[Payment]("Code", "Code", (p, code) => p.code = code),
            DecimalField[Payment]("Amount", "Amount", (p, amount) => p.amount = amount)
        ) from "{stream-name}.PAYMENTS" where s"Code = '$paymentCode'"

        (for {
            _ <- DataAccessComponent.transactionScope { dac =>
                Future {
                    dac.execute(command)
                    dac.commit()
                }
            }
            queryResult <- query.perform()
        } yield queryResult) map { result =>
            assert(result.length === 0)
        }
    }
}
