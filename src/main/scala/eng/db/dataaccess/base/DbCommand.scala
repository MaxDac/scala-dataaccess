package eng.db.dataaccess.base

import java.sql._

import com.typesafe.config.ConfigFactory
import eng.db.dataaccess.Oracle.OracleDbCommandDialect
import eng.db.dataaccess.base.DbCommand.{FetchReaderType, FetchReaderTypeByParameterIndex, FetchReaderTypeByParameterName}
import eng.db.dataaccess.base.Enums._
import eng.db.dataaccess.utils.StringBuilderPlus

case class DbCommandParameter(
                            pType: DbType,
                            name: String,
                            value: Any,
                            direction: DbDirection = Input,
                            isOutputArray: Boolean = false
                           )

object DbCommandDialect {
    private val configuration = ConfigFactory.parseResources("defaults.conf").resolve()
    private val databaseType: String = configuration.getString("conf.dataAccess.type")

    def apply(): DbCommandDialect = {
        this.databaseType match {
            case "Oracle" => new OracleDbCommandDialect()
        }
    }
}
trait DbCommandDialect {
    def appendParameter(statement: CallableStatement, parameter: DbCommandParameter)(implicit connection: Connection)
}

object DbCommand {
    implicit def tuple32CommandParameter(x: (DbType, String, Any)): DbCommandParameter = DbCommandParameter.apply(x._1, x._2, x._3)
    implicit def tuple3X2CommandParameter(x: (DbType, String, DbDirection)): DbCommandParameter = DbCommandParameter.apply(x._1, x._2, scala.None, x._3)
    implicit def tuple42CommandParameter(x: (DbType, String, Any, DbDirection)): DbCommandParameter = DbCommandParameter.apply(x._1, x._2, x._3, x._4)
    implicit def tuple4X2CommandParameter(x: (DbType, String, DbDirection, Boolean)): DbCommandParameter = DbCommandParameter.apply(x._1, x._2, scala.None, x._3, x._4)
    implicit def tuple52CommandParameter(x: (DbType, String, Any, DbDirection, Boolean)): DbCommandParameter = DbCommandParameter.apply(x._1, x._2, x._3, x._4, x._5)

    sealed trait FetchReaderType
    object FetchReaderTypeByParameterName extends FetchReaderType
    object FetchReaderTypeByParameterIndex extends FetchReaderType

    def apply(procedureName: String, fetchReaderType: FetchReaderType, parameters: DbCommandParameter*)(implicit dialect: DbCommandDialect): DbCommand =
        new DbCommand(procedureName, fetchReaderType, parameters:_*)(dialect)

    def apply(procedureName: String, parameters: DbCommandParameter*)(implicit dialect: DbCommandDialect): DbCommand =
        new DbCommand(procedureName, FetchReaderTypeByParameterName, parameters:_*)(dialect)
}
class DbCommand(val procedureName: String, val fetchReaderType: FetchReaderType, val parameters: DbCommandParameter*)(implicit val dialect: DbCommandDialect) {

    /**
      * Get the procedure execution statement creation delegate.
      * @return The execution creation delegate.
      */
    def getExecuteDelegate: Connection => CallableStatement = { connection =>
        val textStatement = new StringBuilderPlus()
        for (_ <- 0 until this.parameters.length) textStatement.append("?")
        val statement = connection.prepareCall(s"{ call $procedureName(${textStatement.toString}) }")

        for (par <- this.parameters) {
            this.dialect.appendParameter(statement, par)(connection)
        }

        statement
    }

    /**
      * Provides the fetch delegate for the procedure creation.
      * The delegate returns either an Int, with the absolute index of the cursor parameter, or a String with the cursor parameter name.
      * @return The fetch delegate.
      */
    def getFetchDelegate: Connection => (Either[Int, String], CallableStatement) = { connection =>
        val textStatement = new StringBuilderPlus()
        for (_ <- 0 until this.parameters.length) textStatement.append("?")
        val statement = connection.prepareCall(s"{ call $procedureName(${textStatement.toString}) }")

        for (par <- this.parameters) {
            this.dialect.appendParameter(statement, par)(connection)
        }

        val cursorParameter = this.parameters.filter(p => p.isOutputArray)
        this.fetchReaderType match {
            case FetchReaderTypeByParameterIndex => (Left(this.parameters.indexOf(cursorParameter.head)), statement)
            case FetchReaderTypeByParameterName => (Right(cursorParameter.head.name), statement)
        }

    }
}

class ResultCommand(parametersListDelegate: CallableStatement => Unit) {

}
