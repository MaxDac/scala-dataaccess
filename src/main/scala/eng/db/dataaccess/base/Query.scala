package eng.db.dataaccess.base

import java.sql.ResultSet
import java.util.Date

import eng.db.dataaccess.base.Query.{LiteralQueryWithParser, QueryWithParsers}
import eng.db.dataaccess.utils.StringBuilderPlus

import scala.concurrent.{ExecutionContext, Future}

sealed trait SelectReader[T] {
    val column: String
    val alias: String
    val reader: (T, ResultSet) => Unit
}

case class StringField[T](column: String, alias: String, getter: (T, String) => Unit) extends SelectReader[T] {
    val reader: (T, ResultSet) => Unit = (result: T, set: ResultSet) => {
        getter(result, set.getString(this.alias) match {
            case value if value != null => value
            case _ => ""
        })
    }
}

case class DecimalField[T](column: String, alias: String, getter: (T, BigDecimal) => Unit) extends SelectReader[T] {
    val reader: (T, ResultSet) => Unit = (result: T, set: ResultSet) => {
        getter(result, set.getBigDecimal(this.alias) match {
            case value if value != null => value
            case _ => 0
        })
    }
}

case class DateField[T](column: String, alias: String, getter: (T, Date) => Unit) extends SelectReader[T] {
    val reader: (T, ResultSet) => Unit = (result: T, set: ResultSet) => {
        getter(result, set.getDate(this.alias) match {
            case value if value != null => value
            case _ => new Date()
        })
    }
}

case class IntegerField[T](column: String, alias: String, getter: (T, Int) => Unit) extends SelectReader[T] {
    val reader: (T, ResultSet) => Unit = (result: T, set: ResultSet) => {
        getter(result, set.getInt(this.alias))
    }
}

case class FloatField[T](column: String, alias: String, getter: (T, Float) => Float) extends SelectReader[T] {
    val reader: (T, ResultSet) => Unit = (result: T, set: ResultSet) => {
        getter(result, set.getFloat(this.alias) match {
            case value if value != null => value
            case _ => 0
        })
    }
}

case class DoubleField[T](column: String, alias: String, getter: (T, Double) => Double) extends SelectReader[T] {
    val reader: (T, ResultSet) => Unit = (result: T, set: ResultSet) => {
        getter(result, set.getDouble(this.alias) match {
            case value if value != null => value
            case _ => 0
        })
    }
}

case class LongField[T](column: String, alias: String, getter: (T, Long) => Long) extends SelectReader[T] {
    val reader: (T, ResultSet) => Unit = (result: T, set: ResultSet) => {
        getter(result, set.getLong(this.alias) match {
            case value if value != null => value
            case _ => 0
        })
    }
}

object Query {

    trait BaseQuery[T] {
        def from(fromClause: String*): BaseQuery[T]
        def where(whereClause: String*): BaseQuery[T]

        def perform()(implicit ec: ExecutionContext): Future[Seq[T]]
        def performWithin(scope: DataAccessComponent): Seq[T]
    }

    trait QueryWithParsers[T] extends BaseQuery[T] {
        def select(selectParameters: SelectReader[T]*): BaseQuery[T]
    }

    trait LiteralQueryWithParser[T] extends BaseQuery[T] {
        def selectLiteral(fields: String*): LiteralQueryWithParser[T]
        def withParser(queryParser: QueryReader[T]): BaseQuery[T]
    }

    type QueryReader[T] = (ResultSet) => T
    def apply[T](generator: () => T): QueryWithParsers[T] = new Query[T](Some(generator))
    def apply[T](): LiteralQueryWithParser[T] = new Query[T](None)
}
private class Query[T](val generator: Option[() => T]) extends QueryWithParsers[T] with LiteralQueryWithParser[T] {
    import eng.db.dataaccess.base.Query._

    private var selectClause: Option[Seq[SelectReader[T]]] = None
    private var literalSelectClause: Option[Seq[String]] = None
    private var parser: Option[QueryReader[T]] = None
    private var fromClause: String = ""
    private var whereClause: String = ""

    def select(selectParameters: SelectReader[T]*): BaseQuery[T] = {
        this.selectClause = Some(selectParameters)
        this
    }

    def selectLiteral(fields: String*): LiteralQueryWithParser[T] = {
        this.literalSelectClause = Some(fields)
        this
    }

    def from(fromClause: String*): BaseQuery[T] = {
        val builder = new StringBuilderPlus("\n      ,")
        for (item <- fromClause) {
            builder.append(item)
        }
        this.fromClause = builder.toString
        this
    }

    def where(whereClause: String*): BaseQuery[T] = {
        val builder = new StringBuilderPlus("\n   AND ")
        for (clause <- whereClause) {
            builder.append(clause)
        }
        this.whereClause = builder.toString
        this
    }

    def withParser(queryParser: QueryReader[T]): BaseQuery[T] = {
        this.parser = Some(queryParser)
        this
    }

    private def getLiteralQuery: String = {
        val selectClauseString = new StringBuilderPlus("\n      ,")

        (this.selectClause, this.literalSelectClause) match {
            case (Some(list), None) => for (selectItem <- list) selectClauseString.append(s"${selectItem.column} ${selectItem.alias}")
            case (None, Some(list)) =>
                if (this.parser == None) throw new IllegalArgumentException("No parser for the query")
                for (selectItem <- list) {
                    selectClauseString.append(s"${selectItem}")
                }
            case _ => throw new IllegalArgumentException("Select list not populated.")
        }

        val parsedQuery = s"SELECT ${selectClauseString.toString}\n  FROM $fromClause\n WHERE $whereClause"
        //println(parsedQuery)
        parsedQuery
    }

    private def getParser: QueryReader[T] = {
        (this.selectClause, this.parser) match {
            case (Some(list), None) => (rSet: ResultSet) => {
                val returnValue = this.generator.get.apply()
                list.map(_.reader).foreach(r => r(returnValue, rSet))
                returnValue
            }
            case (None, Some(p)) => p
            case _ => throw new IllegalArgumentException("Select list not populated.")
        }
    }

    def perform()(implicit ec: ExecutionContext): Future[Seq[T]] = {
        val queryData = (getLiteralQuery, getParser)
        DataAccessComponent.scope { scopeDac =>
            scopeDac.select(connection => connection.prepareStatement(queryData._1), queryData._2)
        }
    }

    def performWithin(scope: DataAccessComponent): Seq[T] = {
        val queryData = (getLiteralQuery, getParser)
        scope.select(connection => connection.prepareStatement(queryData._1), queryData._2)
    }
}
