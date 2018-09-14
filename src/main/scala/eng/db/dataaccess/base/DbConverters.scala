package eng.db.dataaccess.base

trait DbConverters {
    implicit def scalaBigDecimal2DbDecimal(value: BigDecimal): java.math.BigDecimal = value.bigDecimal
}
