import dataaccess.tests.dtos.Payment
import eng.db.dataaccess.base.DataAccessComponent.EquivalentTable
import eng.db.dataaccess.base.{DataAccessComponent, DecimalField, Query, StringField}
import org.scalatest.AsyncFlatSpec

import scala.concurrent.ExecutionContextExecutor

private object Fornitura {
    val idForn: StringField[Fornitura] = StringField[Fornitura]("FORN.FORN_FORNITURA_ID", "IdFORN", (f, v) => f.Id = v)
    val codForn: StringField[Fornitura] = StringField[Fornitura]("FORN.FORN_FORNITURA_COD", "CodFORN", (f, v) => f.Code = v)

    val table = "CONF_FORNITURE FORN"
}
private case class Fornitura(
                                var Id: String = "",
                                var Code: String = ""
                            )

private object Pagamento {
    val idPag: StringField[Pagamento] = StringField[Pagamento]("PAG.PAG_PAGAMENTO_ID", "IdPAG", (f, v) => f.Id = v)
    val amountPag: DecimalField[Pagamento] = DecimalField[Pagamento]("PAG.PAG_IMPORTO", "ImportoPAG", (f, v) => f.Amount = v)
    val fornituraCode: StringField[Pagamento] = StringField[Pagamento]("FORN.FORN_FORNITURA_COD", "CodFORN", (f, v) => f.fornituraCode = v)

    val table = "CRD_PAGAMENTI PAG"
    val tableFornituraRelation = "CRD_PAGAMENTI_REL_FORNITURE PAUT"
}
private case class Pagamento (
                                var Id: String = "",
                                var fornituraCode: String = "",
                                var Amount: BigDecimal = 0
                            )

class QuerySpec extends AsyncFlatSpec {

    private implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

    val numberOfItems = 12

    "The query" should s"return $numberOfItems items" in {

        val query = Query(() => Fornitura()) select (
            Fornitura.idForn,
            Fornitura.codForn
        ) from Fornitura.table where s"ROWNUM <= $numberOfItems"

        query.perform().map(list => assert(list.length === this.numberOfItems))

    }

    "The query" should s"return 9 items" in {

        val query = Query(() => Pagamento()) select (
            Pagamento.fornituraCode,
            Pagamento.idPag,
            Pagamento.amountPag
        ) from (
            Fornitura.table,
            Pagamento.tableFornituraRelation,
            Pagamento.table,
        ) where (
            "FORN.FORN_FORNITURA_COD = '262937069001'",
            "FORN.FORN_FORNITURA_ID = PAUT.FORN_FORNITURA_ID",
            "PAUT.PAG_PAGAMENTO_ID = PAG.PAG_PAGAMENTO_ID",
            "PAUT.EDW_PARTITION_DOC = PAG.EDW_PARTITION_DOC"
        )

        query.perform().map(list => {
            for (item <- list) {
                println(item)
            }

            assert(list.length === 9)
        })
    }

    "The query" should s"return 9 items inside a scope" in {

        val query = DataAccessComponent scope { scope =>
            Query(() => Pagamento()) select (
                Pagamento.fornituraCode,
                Pagamento.idPag,
                Pagamento.amountPag
            ) from (
                Fornitura.table,
                Pagamento.tableFornituraRelation,
                Pagamento.table,
            ) where (
                "FORN.FORN_FORNITURA_COD = '262937069001'",
                "FORN.FORN_FORNITURA_ID = PAUT.FORN_FORNITURA_ID",
                "PAUT.PAG_PAGAMENTO_ID = PAG.PAG_PAGAMENTO_ID",
                "PAUT.EDW_PARTITION_DOC = PAG.EDW_PARTITION_DOC"
            ) performWithin scope
        }

        query.map(list => {
            for (item <- list) {
                println(item)
            }

            assert(list.length === 9)
        })
    }

    "The literal query" should s"return 9 items inside a scope" in {
        val query = DataAccessComponent scope { scope =>
            Query() selectLiteral (
                "FORN.FORN_FORNITURA_COD CodFORN",
                "PAG.PAG_PAGAMENTO_ID IdPAG",
                "PAG.PAG_IMPORTO ImportoPAG"
            ) withParser { set =>
                new Pagamento(
                    Id = set.getString("IdPAG"),
                    fornituraCode = set.getString("CodFORN"),
                    Amount = set.getBigDecimal("ImportoPAG")
                )
            } from (
                Fornitura.table,
                Pagamento.tableFornituraRelation,
                Pagamento.table,
            ) where (
                "FORN.FORN_FORNITURA_COD = '262937069001'",
                "FORN.FORN_FORNITURA_ID = PAUT.FORN_FORNITURA_ID",
                "PAUT.PAG_PAGAMENTO_ID = PAG.PAG_PAGAMENTO_ID",
                "PAUT.EDW_PARTITION_DOC = PAG.EDW_PARTITION_DOC"
            ) performWithin scope
        }

        query.map(list => {
            for (item <- list) {
                println(item)
            }

            assert(list.length === 9)
        })
    }

    "The function" should s"return $numberOfItems items" in {

        val result = DataAccessComponent.scope { dac =>
            dac.queryEquivalentTable(EquivalentTable(
                "CRD_SERVICES_TESTS_PCK.TEST_PIPELINED_FUNCTION",
                numberOfItems
            ), queryResult => Payment(
                queryResult.getString(1),
                queryResult.getString(1),
                queryResult.getBigDecimal(2)
            ))
        }

        result.map(list => assert(list.length === 12))

    }
}
