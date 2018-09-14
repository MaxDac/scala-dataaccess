package dataaccess.tests.dtos

import java.sql.Date

case class Payment(
                      var id: String = "",
                      var code: String = "",
                      var amount: BigDecimal = 0,
                      var dueDate: Date = new Date(0)
                  )