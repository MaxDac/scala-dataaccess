package eng.db.dataaccess.base

import java.sql._

import eng.db.dataaccess.Oracle.OracleDataAccessComponent

import scala.concurrent.{ExecutionContext, Future}

case class DataAccessSettings()

object DataAccessComponent {
    sealed case class EquivalentTable(
                                     tableName: String,
                                     parameters: Any*
                                     )

    private def getInstance(settings: DataAccessSettings): DataAccessComponent = {
        new OracleDataAccessComponent()
    }

    /**
      * Defines a scope in which a number of sql operation could be performed.
      * @param executionDelegate The execution delegate. This delegate contains all the operation that has to be performed.
      * @param settings The connection settings.
      * @param ec The execution context.
      * @tparam T The return type.
      * @return The result of the elaboration.
      */
    def scope[T](executionDelegate: DataAccessComponent => T, settings: DataAccessSettings = DataAccessSettings())(implicit ec: ExecutionContext): Future[T] = Future {
        val dac = getInstance(settings)
        try {
            executionDelegate(dac)
        }
        finally {
            dac.dispose()
        }
    }

    /**
      * This methods executes statements in a transactional scope, allowing the client code to commit when it wants.
      * The commit statement will be up to the client. If the client will not commit the changes, they will automatically rollbacked at the end of the procedure.
      * @param executionDelegate The execution delegate. This delegate contains all the operation that has to be performed.
      * @param settings The connection settings.
      * @param ec The execution context.
      * @tparam T The return type.
      * @return The result of the elaboration.
      */
    def transactionScope[T](executionDelegate: DataAccessComponent => T, settings: DataAccessSettings = DataAccessSettings())(implicit ec: ExecutionContext): Future[T] = Future {
        val dac = getInstance(settings)
        val connection = dac.getCurrentConnection

        try {
            connection.setAutoCommit(false)
            executionDelegate(dac)
        }
        finally {
            connection.rollback()
            connection.setAutoCommit(true)
            dac.dispose()
        }
    }

    implicit class Helpers(val dac: DataAccessComponent) {
        // Helpers methods
        def queryEquivalentTable[T](equivalentTable: EquivalentTable, reader: ResultSet => T): Seq[T] = {
            dac.select[T](connection => {
                val sqlInit = s"SELECT * FROM TABLE(${equivalentTable.tableName}("
                val sqlEnd = "))"

                val sql = new StringBuilder()
                for (parameter <- equivalentTable.parameters) {
                    sql.append(s"'$parameter', ")
                }
                val sqlMiddle = sql.toString.substring(0, sql.length - 2)

                connection.prepareStatement(s"$sqlInit$sqlMiddle$sqlEnd")
            }, reader)
        }
    }
}
trait DataAccessComponent {

    /**
      * Execute a sql statement.
      * @param delegate The parameter generator delegate.
      * @param outputFetchDelegate The output parameter delegate manager.
      * @tparam T The output type.
      * @return The result.
      */
    def execute[T](delegate: Connection => CallableStatement, outputFetchDelegate: Option[CallableStatement => T]): Either[T, Unit]

    /**
      * Execute a select statement provided as input parameter.
      * @param delegate The select statement.
      * @param reader The reader delegate.
      * @tparam T The output type.
      * @return The list of items fetched from the query.
      */
    def select[T](delegate: Connection => PreparedStatement, reader: ResultSet => T): Seq[T]

    /**
      * Fetches the result from a prepared statement.
      * @param delegate The statement generation delegate. It returns either an Int, with the absolute position of the cursor parameter, or a String with the cursor parameter name.
      * @param reader The reader delegate.
      * @tparam T The output type.
      * @return The list of item fetched from the procedure.
      */
    def fetch[T](delegate: Connection => (Either[Int, String], CallableStatement), reader: ResultSet => T): Seq[T]

    /**
      * Closes the connection.
      * This method must be called in the finally clause after every use of the data access component.
      */
    def dispose(): Unit

    def fetch[T](command: DbCommand, reader: ResultSet => T): Seq[T] = this.fetch[T](command.getFetchDelegate, reader)

    def execute[T](delegate: Connection => CallableStatement): Unit = this.execute(delegate, None).right.get
    def execute[T](command: DbCommand): Unit = this.execute(command.getExecuteDelegate, None).right.get
    def execute[T](delegate: Connection => CallableStatement, outputFetchDelegate: CallableStatement => T): T = this.execute(delegate, Some(outputFetchDelegate)).left.get
    def execute[T](command: DbCommand, outputFetchDelegate: CallableStatement => T): T = this.execute(command.getExecuteDelegate, Some(outputFetchDelegate)).left.get

    /**
      * This methods must return the current connection instance used.
      * @return The current connection instance.
      */
    protected def getCurrentConnection: Connection

    /**
      * A wrapper for the connection commit.
      */
    def commit(): Unit = this.getCurrentConnection.commit()
}
