package mytown._datasource.backends;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mytown._datasource.DatasourceBackend;
import mytown._datasource.DatasourceTask;
import mytown._datasource.SQLSchema;
import mytown.config.Config;
import mytown.core.utils.config.ConfigProperty;
import mytown.entities.*;
import mytown.entities.flag.Flag;
import mytown.entities.flag.FlagType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.sql.*;
import java.util.*;

// TODO Add null checks to things that need it. Maybe just make an Assert.NotNull that throws an NPE or something

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

    protected Gson gson;

    public SQLBackend() {
        Gson gson = new GsonBuilder().create();

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
        }

        prefix = "test_" + prefix; // TODO Remove test_
    }

    @Override
    protected boolean init() {
        SQLSchema.init(prefix, AUTO_INCREMENT, getConnection());
        return true;
    }

    @Override
    protected void load() {
        log.info("Loading...");
        try {
            loadUniverse();
            loadServer();
            loadWorlds();
            // TODO Load Teleports
            loadResidents();
            loadTowns();
            loadNations();
            loadRanks();
            loadTownBlocks();
            loadTownPlots();
            loadResidentsToTowns();
            loadResidentsToPlots();
            loadTownsToNations();
            loadBlockWhitelists();
            loadSelectedTowns();
            loadFriends();
            loadFriendRequests();
            loadTownInvites();
            log.info("Loaded!");
        } catch (SQLException e) {
            log.info("Failed to load!");
            e.printStackTrace();
        }
    }

    @Override
    protected void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                } // Ignore since we are just closing an old connection
                conn = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        String whereStr = getWhere(task.keys, params);

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
        String whereStr = getWhere(task.keys, params);

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

    /* ----- Load ----- */

    protected void loadUniverse() throws SQLException {
        log.info("Loading Universe...");
        PreparedStatement stmt = prepare("SELECT * FROM " + prefix + "UniverseFlags;");
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            String flagName = rs.getString("name");
            FlagType type = FlagType.valueOf(flagName);

            Flag flag = new Flag(type, gson.fromJson(rs.getString("serializedValue"), type.getType()));
            Universe.get().addFlag(flag);
        }
    }

    protected void loadServer() throws SQLException {
        log.info("Loading Server...");
        Server srv = new Server(Config.serverID);
        Universe.get().addServerNoSave(srv);

        // Check that Server exists
        PreparedStatement stmt = prepare("SELECT * FROM " + prefix + "Servers WHERE uuid=?;");
        stmt.setString(1, Config.serverID);
        ResultSet rs = stmt.executeQuery();
        if (!rs.next()) { // Insert if the Server does not exist
            Map<String, Object> args = new Hashtable<String, Object>();
            args.put("uuid", srv.getID());
            insert(new DatasourceTask(DatasourceTask.Type.INSERT, "Servers", args));
        }

        // Load Per-Server Flags
        stmt = prepare("SELECT * FROM " + prefix + "ServerFlags WHERE server=?;");
        rs = stmt.executeQuery();

        while(rs.next()) {
            String flagName = rs.getString("name");
            FlagType type = FlagType.valueOf(flagName);

            Flag flag = new Flag(type, gson.fromJson(rs.getString("serializedValue"), type.getType()));
            Universe.get().getServer().addFlag(flag);
        }
    }

    protected void loadWorlds() throws SQLException {
        log.info("Loading Worlds...");
        PreparedStatement stmt = prepare("SELECT * FROM " + prefix + "Worlds WHERE server=?;");
        stmt.setString(1, Config.serverID);
        ResultSet rs = stmt.executeQuery();
        while(rs.next()) {
            Universe.get().addWorldNoSave(new World(rs.getInt("dim")));
        }

        // Remove worlds that no longer exist
        for (World world : Universe.get().getWorlds()) {
            if (DimensionManager.getWorld(world.getID()) == null) {
                Universe.get().removeWorld(world);
            }
        }

        // Add any worlds that have not been added yet
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            World newWorld = new World(world.provider.dimensionId);
            if (!Universe.get().hasWorld(newWorld)) {
                Map<String, Object> args = new Hashtable<String, Object>();
                args.put("dim", newWorld.getID());
                args.put("server", Config.serverID);
                insert(new DatasourceTask(DatasourceTask.Type.INSERT, "Worlds", args));
            }
        }

        // Load Per-World Flags
        stmt = prepare("SELECT * FROM " + prefix + "WorldFlags WHERE server=?;");
        rs = stmt.executeQuery();

        while(rs.next()) {
            String flagName = rs.getString("name");
            int dim = rs.getInt("dim");
            FlagType type = FlagType.valueOf(flagName);

            Flag flag = new Flag(type, gson.fromJson(rs.getString("serializedValue"), type.getType()));
            Universe.get().getServer().getWorld(dim).addFlag(flag);
        }
    }

    protected void loadResidents() throws SQLException {
        log.info("Loading Residents...");
        PreparedStatement stmt = prepare("SELECT * FROM " + prefix + "Residents;");
        ResultSet rs = stmt.executeQuery();

        while(rs.next()) {
            Universe.get().addResidentNoSave(new Resident(rs.getString("uuid"), rs.getString("name"), rs.getLong("joined"), rs.getLong("lastOnline"), rs.getInt("extraBlocks")));
        }
    }

    protected void loadTowns() throws SQLException {
        log.info("Loading Towns...");
        PreparedStatement stmt = prepare("SELECT * FROM " + prefix + "Towns;");
        ResultSet rs = stmt.executeQuery();

        while(rs.next()) {
            Universe.get().addTownNoSave(new Town(rs.getString("name"), rs.getInt("extraBlocks"), rs.getInt("maxPlots")));
        }

        // Load Town-Flags
        stmt = prepare("SELECT * FROM " + prefix + "TownFlags WHERE server=?;");
        stmt.setString(1, Config.serverID);
        rs = stmt.executeQuery();

        while(rs.next()) {
            String flagName = rs.getString("name");
            int dim = rs.getInt("dim");
            FlagType type = FlagType.valueOf(flagName);

            Flag flag = new Flag(type, gson.fromJson(rs.getString("serializedValue"), type.getType()));
            Universe.get().getTown(rs.getString("town")).addFlag(flag);
        }
    }

    protected void loadNations() throws SQLException {
        log.info("Loading Nations...");
        PreparedStatement stmt = prepare("SELECT * FROM " + prefix + "Nations;");
        ResultSet rs = stmt.executeQuery();

        while(rs.next()) {
            Universe.get().addNationNoSave(new Nation(rs.getString("name")));
        }
    }

    protected void loadRanks() throws SQLException {
        log.info("Loading Ranks...");
        PreparedStatement stmt = prepare("SELECT * FROM " + prefix + "Ranks;");
        ResultSet rs = stmt.executeQuery();

        while(rs.next()) {
            Town t = Universe.get().getTown(rs.getString("town"));
            if (t == null) continue;
            t.addRank(new Rank(rs.getString("name"), t));
        }

        // Load RankPermissions
        stmt = prepare("SELECT * FROM " + prefix + "RankPermissions;");
        rs = stmt.executeQuery();

        while(rs.next()) {
            Town t = Universe.get().getTown(rs.getString("town"));
            if (t == null) continue;
            Rank r = t.getRank(rs.getString("rank"));
            if (r == null) continue;
            r.addPermission(rs.getString("node"));
        }
    }

    protected void loadTownBlocks() throws SQLException {
        log.info("Loading TownBlocks...");
        PreparedStatement stmt = prepare("SELECT * FROM " + prefix + "TownBlocks WHERE server=?;");
        stmt.setString(1, Config.serverID);
        ResultSet rs = stmt.executeQuery();

        while(rs.next()) {
            Town t = Universe.get().getTown(rs.getString("town"));
            if (t == null) continue;
            TownBlock b = new TownBlock(rs.getInt("dim"), rs.getInt("x"), rs.getInt("z"), t);
            Universe.get().addBlockNoSave(b);
            t.addBlock(b);
        }
    }

    protected void loadTownPlots() throws SQLException {
        log.info("Loading TownPlots...");
        PreparedStatement stmt = prepare("SELECT * FROM " + prefix + "TownPlots WHERE server=?;");
        stmt.setString(1, Config.serverID);
        ResultSet rs = stmt.executeQuery();

        while(rs.next()) {
            Town t = Universe.get().getTown(rs.getString("town"));
            if (t == null) continue;
            Plot p = new Plot(rs.getString("name"), t, rs.getInt("dim"), rs.getInt("x1"), rs.getInt("y1"), rs.getInt("z1"), rs.getInt("x2"), rs.getInt("y2"), rs.getInt("z2"));
            Universe.get().addPlotNoSave(p);
            t.addPlot(p);
        }

        // Load Plot Flags
        stmt = prepare("SELECT * FROM " + prefix + "PlotFlags WHERE server=?;");
        stmt.setString(1, Config.serverID);
        rs = stmt.executeQuery();

        while(rs.next()) {
            String flagName = rs.getString("name");
            int plot = rs.getInt("plot");
            FlagType type = FlagType.valueOf(flagName);

            Flag flag = new Flag(type, gson.fromJson(rs.getString("serializedValue"), type.getType()));
            Universe.get().getPlot(plot).addFlag(flag);
        }
    }

    protected void loadResidentsToTowns() throws SQLException {
        ResultSet rs = prepare("SELECT * FROM " + prefix + "ResidentsToTowns;").executeQuery();

        while(rs.next()) {
            Resident res = Universe.get().getResidentByUUID(rs.getString("resident"));
            Town town = Universe.get().getTown(rs.getString("town"));
            Rank rank = town.getRank(rs.getString("rank"));
            town.addResident(res, rank);
        }
    }

    protected void loadResidentsToPlots() throws SQLException {
        PreparedStatement stmt = prepare("SELECT * FROM " + prefix + "ResidentsToPlots WHERE server=?;");
        stmt.setString(1, Config.serverID);
        ResultSet rs = stmt.executeQuery();

        while(rs.next()) {
            Resident res = Universe.get().getResidentByUUID(rs.getString("resident"));
            Plot plot = Universe.get().getPlot(rs.getInt("plot"));

            if (rs.getBoolean("isOwner"))
                plot.addOwner(res);

            plot.addResident(res);
        }
    }

    protected void loadTownsToNations() throws SQLException {
        ResultSet rs = prepare("SELECT * FROM " + prefix + "TownsToNations;").executeQuery();

        while(rs.next()) {
            Town t = Universe.get().getTown(rs.getString("town"));
            Nation n = Universe.get().getNation(rs.getString("nation"));
            n.addTown(t, rs.getString("rank"));
        }
    }

    protected void loadBlockWhitelists() throws SQLException {
        PreparedStatement stmt = prepare("SELECT * FROM " + prefix + "BlockWhitelists WHERE server=?");
        stmt.setString(1, Config.serverID);
        ResultSet rs = stmt.executeQuery();

        while(rs.next()) {
            BlockWhitelist bw = new BlockWhitelist(rs.getInt("id"), rs.getInt("dim"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), FlagType.valueOf(rs.getString("flagName")));
            Universe.get().getTown(rs.getString("town")).addBlockWhitelist(bw);
        }
    }

    protected void loadSelectedTowns() throws SQLException {
        ResultSet rs = prepare("SELECT * FROM " + prefix + "SelectedTown;").executeQuery();

        while(rs.next()) {
            Resident res = Universe.get().getResidentByUUID(rs.getString("resident"));
            Town town = Universe.get().getTown(rs.getString("town"));

            res.selectTown(town);
        }
    }

    protected void loadFriends() throws SQLException {
        ResultSet rs = prepare("SELECT * FROM " + prefix + "Friends;").executeQuery();

        while(rs.next()) {
            Resident res1 = Universe.get().getResidentByUUID(rs.getString("resident1"));
            Resident res2 = Universe.get().getResidentByUUID(rs.getString("resident2"));

            res1.addFriend(res2);
            res2.addFriend(res1);
        }
    }

    protected void loadFriendRequests() throws SQLException {
        ResultSet rs = prepare("SELECT * FROM " + prefix + "FriendRequests;").executeQuery();

        while(rs.next()) {
            Resident resFrom = Universe.get().getResidentByUUID(rs.getString("resFrom"));
            Resident resTo = Universe.get().getResidentByUUID(rs.getString("resTo"));
            resTo.addFriend(resFrom);
        }
    }

    protected void loadTownInvites() throws SQLException {
        ResultSet rs = prepare("SELECT * FROM " + prefix + "TownInvites").executeQuery();

        while(rs.next()) {
            Resident res = Universe.get().getResidentByUUID(rs.getString("resident"));
            Town town = Universe.get().getTown(rs.getString("town"));

            res.addInvite(town);
        }
    }

    /* ----- Helpers ----- */

    /**
     * Returns a Connection object creating it if it doesn't exist, or has errored out.
     * @return The Connection object
     */
    protected final Connection getConnection() {
        try {
            if (conn == null || conn.isClosed() || (!Config.dbType.equalsIgnoreCase("sqlite") && !conn.isValid(1))) {
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
     * @param keys The keys
     * @param params The params. "Out parameter"
     * @return The WHERE String
     */
    protected final String getWhere(Map<String, Object> keys, List<Object> params) {
        String whereStr = "";

        String[] k = keys.keySet().toArray(new String[keys.size()]);
        for (int i=0; i< keys.size(); i++) {
            whereStr += (k[i] + "=?");
            params.add(keys.get(k[i]));

            if (i < keys.size() - 1) {
                whereStr += " AND ";
            }
        }

        for (Map.Entry<String, Object> e : keys.entrySet()) {
            whereStr += (e.getKey() + "=?");
            params.add(e.getValue());
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
