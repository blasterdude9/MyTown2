package mytown._datasource.backends;

import mytown._datasource.DatasourceBackend;
import mytown._datasource.DatasourceTask;
import mytown._datasource.SQLSchema;
import mytown.config.Config;
import mytown.core.utils.config.ConfigProperty;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Joe Goett
 */
public abstract class SQLBackend extends DatasourceBackend {
    @ConfigProperty(category = "datasource.sql", comment = "The prefix of each of the tables. <prefix>tablename")
    protected String prefix = "";

    @ConfigProperty(category = "datasource.sql", comment = "User defined properties to be passed to the connection.\nFormat: key=value;key=value...")
    protected String[] userProperties = {};

    protected String AUTO_INCREMENT = ""; // Only because SQLite and MySQL are different >.>

    protected Properties dbProperties = new Properties();
    protected String dsn = "";
    protected Connection conn = null;

    @Override
    protected boolean init() {
        // Add user-defined properties
        for (String prop : userProperties) {
            String[] pair = prop.split("=");
            if (pair.length < 2) continue;
            dbProperties.put(pair[0], pair[1]);
        }

        // Register driver if needed
        try {
            Driver driver = (Driver) Class.forName(getDriver()).newInstance();
            DriverManager.registerDriver(driver);
        } catch (Exception ex) {
            log.error("Failed to register JDBC Driver!", ex);
            return false;
        }

        prefix = "test_" + prefix;

        SQLSchema.init(prefix, AUTO_INCREMENT, getConnection());

        return true;
    }

    /**
     * Returns the class of the Driver being used
     *
     * @return
     */
    protected abstract String getDriver();

    /* ----- Queries ----- */

    @Override
    protected void insert(DatasourceTask task) {
        String keyStr = "", valStr = "";
        String aKeys[] = task.args.keySet().toArray(new String[task.args.size()]);

        // Construct keyStr and valStr
        for (int i = 0; i < task.args.size(); i++) {
            keyStr += aKeys[i];
            valStr += "?";

            if (i < task.args.size() - 1) {
                keyStr += ",";
                valStr += ",";
            }
        }

        // Construct INSERT statement
        String sqlStr = "INSERT INTO " + prefix + task.tblName + " (" +
                keyStr +
                ") VALUES(" +
                valStr +
                ");";

        log.debug(sqlStr);

        // Run Query
        try {
            PreparedStatement stmt = prepare(sqlStr);
            List<Object> params = new ArrayList<Object>();
            params.addAll(task.args.values());
            setParams(stmt, params);
            stmt.execute();
        } catch (SQLException e) {
            log.error("Failed insert task!", e);
        }
    }

    @Override
    protected void update(DatasourceTask task) {
        String setStr = "";
        String aKeys[] = task.args.keySet().toArray(new String[task.args.size()]);

        // Construct setStr
        for (int i = 0; i < task.args.size(); i++) {
            setStr += (aKeys[i] + "=?");

            if (i < task.args.size() - 1) {
                setStr += ",";
            }
        }

        // Setup params List
        List<Object> params = new ArrayList<Object>();
        params.addAll(task.args.values());

        // Construct WHERE string
        String whereStr = getWhere(task.args, task.keys, params);

        // Construct UPDATE statement
        String sqlStr = "UPDATE " + prefix + task.tblName + " SET " + setStr + " WHERE "  + whereStr + ";";

        log.debug(sqlStr);

        // Run Query
        try {
            PreparedStatement stmt = prepare(sqlStr);
            setParams(stmt, params);
            stmt.execute();
        } catch (SQLException e) {
            log.error("Failed update task!", e);
        }
    }

    @Override
    protected void delete(DatasourceTask task) {
        List<Object> params = new ArrayList<Object>();
        // Construct WHERE string
        String whereStr = getWhere(task.args, task.keys, params);

        // Construct DELETE FROM statement
        String sqlStr = "DELETE FROM " + prefix + task.tblName + " WHERE " + whereStr + ";";

        log.debug(sqlStr);

        // Run Query
        try {
            PreparedStatement stmt = prepare(sqlStr);
            setParams(stmt, params);
            stmt.execute();
        } catch (SQLException e) {
            log.error("Failed delete task!", e);
        }
    }

    /* ----- Helpers ----- */

    /**
     * Returns a Connection object creating it if it doesn't exist, or has errored out.
     * @return The Connection object
     */
    protected final Connection getConnection() {
        try {
            if (conn == null || conn.isClosed() || (!Config.dbType.equalsIgnoreCase("sqlite") && conn.isValid(1))) {
                if (conn != null && !conn.isClosed()) {
                    try {
                        conn.close();
                    } catch (SQLException ex) {
                    } // Ignore since we are just closing an old connection
                    conn = null;
                }

                conn = DriverManager.getConnection(dsn, dbProperties);

                if (conn == null || conn.isClosed()) {
                    return null;
                }
            }
            return conn;
        } catch (SQLException ex) {
        }
        return conn;
    }

    /**
     * Returns a PreparedStatement with the given SQL
     * @param sqlStr The SQL to prepare
     * @return The PreparedStatement
     * @throws SQLException
     */
    protected final PreparedStatement prepare(String sqlStr) throws SQLException {
        return getConnection().prepareStatement(sqlStr, Statement.RETURN_GENERATED_KEYS);
    }

    /**
     * Returns a WHERE string
     * @param args The args
     * @param keys The keys
     * @param params The params. "Out parameter"
     * @return The WHERE String
     */
    protected final String getWhere(Map<String, Object> args, List<String> keys, List<Object> params) {
        String whereStr = "";

        for (int i = 0; i < keys.size(); i++) {
            whereStr += (keys.get(i) + "=?");

            params.add(args.get(keys.get(i)));

            if (i < keys.size() - 1) {
                whereStr += " AND ";
            }
        }

        return whereStr;
    }

    /**
     * Sets the parameters of the PreparedStatement
     * @param stmt The PreparedStatement
     * @param params The params
     * @throws SQLException
     */
    protected final void setParams(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }
    }
}
