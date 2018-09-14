package eng.db.dataaccess.Oracle

import java.sql._

import eng.db.dataaccess.base.Enums._
import eng.db.dataaccess.base.{DbCommandDialect, DbCommandParameter, DbConverters}
import oracle.jdbc.OracleTypes

object OracleDbCommandDialect {
    def apply(): DbCommandDialect = new OracleDbCommandDialect()
}
class OracleDbCommandDialect extends DbCommandDialect with DbConverters {
    override def appendParameter(statement: CallableStatement, parameter: DbCommandParameter)(implicit connection: Connection): Unit = {
        def setClob(clob: String = parameter.value.asInstanceOf[String]): Unit = {
            if (clob.length < 32000) {
                statement.setString(parameter.name, parameter.value.asInstanceOf[String])
            }
            else {
                val massiveClob = connection.createClob()
                massiveClob.setString(1, clob)
                statement.setClob(parameter.name, massiveClob)
            }
        }

        if (parameter.direction == Input || parameter.direction == InputOutput) {
            (parameter.pType: @unchecked) match {
                case DbTypeString => statement.setString(parameter.name, parameter.value.asInstanceOf[String])
                case DbTypeInt => statement.setInt(parameter.name, parameter.value.asInstanceOf[Int])
                case DbTypeDecimal => statement.setBigDecimal(parameter.name, parameter.value.asInstanceOf[BigDecimal])
                case DbTypeTimestamp => statement.setDate(parameter.name, parameter.value.asInstanceOf[Date])
                case DbTypeClob => setClob()
            }
        }

        if (parameter.direction == Output || parameter.direction == InputOutput) {
            parameter.pType match {
                case DbTypeString => statement.registerOutParameter(parameter.name, OracleTypes.VARCHAR)
                case DbTypeInt | DbTypeDecimal => statement.registerOutParameter(parameter.name, OracleTypes.NUMBER)
                case DbTypeTimestamp => statement.registerOutParameter(parameter.name, OracleTypes.TIMESTAMP)
                case DbTypeClob => statement.registerOutParameter(parameter.name, OracleTypes.CLOB)
                case DbTypeCursor => statement.registerOutParameter(parameter.name, OracleTypes.CURSOR)
            }
        }
    }
}
