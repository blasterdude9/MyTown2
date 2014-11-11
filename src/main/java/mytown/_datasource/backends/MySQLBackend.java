package mytown._datasource.backends;

import mytown.core.utils.config.ConfigProperty;

/**
 * @author Joe Goett
 */
public class MySQLBackend extends SQLBackend {
    // Config
    @ConfigProperty(category = "datasource.sql", comment = "Username to use")
    private String username = "";

    @ConfigProperty(category = "datasource.sql", comment = "Password to use")
    private String password = "";

    @ConfigProperty(category = "datasource.sql", comment = "Hostname:Port of the database")
    private String host = "localhost";

    @ConfigProperty(category = "datasource.sql", comment = "The database name")
    private String database = "mytown";

    @Override
    protected boolean init() {
        this.dsn = "jdbc:mysql://" + host + "/" + database;

        this.AUTO_INCREMENT = "AUTO_INCREMENT";

        // Setup Properties
        dbProperties.put("autoReconnect", "true");
        dbProperties.put("user", username);
        dbProperties.put("password", password);
        dbProperties.put("relaxAutoCommit", "true");

        return super.init();
    }

    @Override
    protected String getDriver() {
        return "com.mysql.jdbc.Driver";
    }
}
