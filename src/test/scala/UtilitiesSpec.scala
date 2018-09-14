import eng.db.dataaccess.utils.StringBuilderPlus
import org.scalatest.FlatSpec

class UtilitiesSpec extends FlatSpec {

    "StringBuilderPlus" should "build string with separator correctly" in {
        val builder = new StringBuilderPlus(", ")
        val result = builder.append("First")
            .append("Second")
            .append("Third")
            .toString

        assert(result === "First, Second, Third")
    }

}
