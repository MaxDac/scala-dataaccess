package eng.db.dataaccess.utils

class Stopwatch {

    private var started: Boolean = false

    private var lastStartedMilliseconds: Long = 0

    private var cumulativeMilliseconds: Long = 0

    private val currentTime: () => Long = () => System.currentTimeMillis()

    def start(): Unit = {
        this.lastStartedMilliseconds = this.currentTime()
        this.started = true
    }

    def stop(): Unit = {
        if (!this.started) throw new UnsupportedOperationException("The stopwatch didn't start")

        this.started = false
        val stopTime = this.currentTime()
        val elapsedMilliseconds = stopTime - this.lastStartedMilliseconds
        this.lastStartedMilliseconds = 0
        this.cumulativeMilliseconds += elapsedMilliseconds
    }

    def restart(): Unit = {
        this.start()
        this.cumulativeMilliseconds = 0
    }

    def getElapsed: Long = {
        val now = this.currentTime()

        if (this.lastStartedMilliseconds == 0) {
            this.cumulativeMilliseconds
        } else {
            now - this.lastStartedMilliseconds + this.cumulativeMilliseconds
        }
    }

    def stopLogAndGo(logEntry: String, mustDividePer: Int = 0): Long = {
        this.stop()
        val elapsed = this.getElapsed
        println(s"Elapsed time for $logEntry: $elapsed.")

        if (mustDividePer > 0) {
            val divided = elapsed / mustDividePer
            println(s"Each cycle lasts an average of $divided")
        }

        this.restart()
        elapsed
    }
}
