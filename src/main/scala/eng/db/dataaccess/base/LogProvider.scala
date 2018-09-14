package eng.db.dataaccess.base

trait LogProvider {
    def write(line: String)
    def flush()
}
