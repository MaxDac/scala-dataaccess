package eng.db.dataaccess.utils

import scala.collection.mutable.ListBuffer

class StringBuilderPlus(val separator: String = ", ") {
    private val stringBuilder = ListBuffer[String]()

    def append(x: String): StringBuilderPlus = {
        this.stringBuilder += x
        this
    }

    override def toString: String = {
        val builder = new StringBuilder()
        for (element <- this.stringBuilder) {
            builder.append(s"$element$separator")
        }
        val returnValue = builder.toString()
        returnValue.substring(0, returnValue.length - this.separator.length)
    }
}
