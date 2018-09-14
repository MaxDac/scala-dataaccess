package eng.db.dataaccess.base

object Enums {

    sealed trait LogicalOperator
    case object Equals extends LogicalOperator
    case object Lesser extends LogicalOperator
    case object LesserEquals extends LogicalOperator
    case object Greater extends LogicalOperator
    case object GreaterEquals extends LogicalOperator
    case object Between extends LogicalOperator
    case object In extends LogicalOperator

    sealed trait DbType
    case object DbTypeString extends DbType
    case object DbTypeInt extends DbType
    case object DbTypeDecimal extends DbType
    case object DbTypeClob extends DbType
    case object DbTypeTimestamp extends DbType
    case object DbTypeCursor extends DbType

    sealed trait DbDirection
    case object Input extends DbDirection
    case object Output extends DbDirection
    case object InputOutput extends DbDirection

}