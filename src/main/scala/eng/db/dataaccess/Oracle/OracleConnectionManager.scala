package eng.db.dataaccess.Oracle

import java.sql.{Connection, DriverManager}

import com.typesafe.config.ConfigFactory
import oracle.ucp.jdbc.PoolDataSourceFactory

/**
  * It picks up the connection string under conf.dataAccess.connectionString
  * the user under conf.dataAccess.user
  * the password under conf.dataAccess.pass
  */
object OracleConnectionManager {
    private val configuration = ConfigFactory.parseResources("defaults.conf").resolve()
    private val connectionString = configuration.getString("conf.dataAccess.connectionString")
    private val username = configuration.getString("conf.dataAccess.user")
    private val password = configuration.getString("conf.dataAccess.pass")

    private val poolDataSource = PoolDataSourceFactory.getPoolDataSource()
    poolDataSource.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource")
    poolDataSource.setURL(this.connectionString)
    poolDataSource.setUser(this.username)
    poolDataSource.setPassword(this.password)

    // Setting connection pool properties
    // Check whether it's the case to put them in the configuration file
    poolDataSource.setInitialPoolSize(5)
    poolDataSource.setMinPoolSize(0)
    poolDataSource.setMaxPoolSize(3)

    def getConnectionFromPool: Connection = {
        this.poolDataSource.getConnection()
    }

    def getConnection: Connection = {
        Class.forName("oracle.jdbc.driver.OracleDriver")
        DriverManager.getConnection(this.connectionString, this.username, this.password)
    }

    def close(connection: Connection): Unit = {
        //println("Closing connection")
        connection.close()
    }
}
