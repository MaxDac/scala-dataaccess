package eng.db.dataaccess.Oracle

import java.sql._

import eng.db.dataaccess.base.DataAccessComponent

import scala.collection.mutable.ListBuffer

/**
  * The Oracle data access component
  */
class OracleDataAccessComponent extends DataAccessComponent {

    private val _connection = OracleConnectionManager.getConnectionFromPool
    override protected def getCurrentConnection: Connection = this._connection

    def execute[T](delegate: Connection => CallableStatement, outputFetchDelegate: Option[CallableStatement => T]): Either[T, Unit] = {
        val connection = this.getCurrentConnection
        val statement = delegate(connection)

        try {
            statement.execute()

            outputFetchDelegate match {
                case Some(dlg) => Left(dlg(statement))
                case _ => Right(Unit)
            }
        }
        finally {
            //println("Closing")
            statement.close()
        }
    }

    def select[T](delegate: Connection => PreparedStatement, reader: ResultSet => T): Seq[T] = {
        val connection = this.getCurrentConnection
        val statement = delegate(connection)
        val queryResult = statement.executeQuery()

        try {
            queryResult.setFetchSize(100)
            val result = ListBuffer[T]()

            while (queryResult.next()) {
                result += reader(queryResult)
            }

            result
        }
        finally {
            //println("Closing")
            statement.close()
            queryResult.close()
        }
    }

    override def fetch[T](delegate: Connection => (Either[Int, String], CallableStatement), reader: ResultSet => T): Seq[T] = {
        val connection = this.getCurrentConnection
        val (outCursorParameter, statement) = delegate(connection)
        statement.execute()

        val queryResult = (outCursorParameter match {
            case Left(cursorParameterIndex) => statement.getObject(cursorParameterIndex)
            case Right(cursorParameterName) => statement.getObject(cursorParameterName)
        }).asInstanceOf[ResultSet]

        try {
            queryResult.setFetchSize(100)
            val result = ListBuffer[T]()

            while (queryResult.next()) {
                result += reader(queryResult)
            }

            result
        }
        finally {
            //println("Closing")
            statement.close()
            queryResult.close()
        }
    }

    override def dispose(): Unit = OracleConnectionManager.close(this._connection)
}
