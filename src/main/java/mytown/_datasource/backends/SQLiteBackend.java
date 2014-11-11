package mytown._datasource.backends;

import mytown.core.utils.config.ConfigProperty;
import mytown.util.Constants;

/**
 * @author Joe Goett
 */
public class SQLiteBackend extends SQLBackend {
    @ConfigProperty(category = "datasource.sql", comment = "The database file path. Used by SQLite")
    private String dbPath = Constants.CONFIG_FOLDER + "data.db";

    @Override
    protected boolean init() {
        this.dsn = "jdbc:sqlite:" + dbPath;

        // Setup Properties
        dbProperties.put("foreign_keys", "ON");

        return super.init();
    }

    @Override
    protected String getDriver() {
        return "org.sqlite.JDBC";
    }
}
