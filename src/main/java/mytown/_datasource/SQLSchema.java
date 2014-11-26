package mytown._datasource;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Joe Goett
 */
public class SQLSchema {
    private static final Logger logger = LogManager.getLogger("MyTown2.Datasource.SQL.Schema");
    private static List<SchemeUpdate> updates = new ArrayList<SchemeUpdate>();
    private static String prefix = "", autoIncrement = "";

    public static void init(String prefix, String autoIncrement, Connection conn) {
        SQLSchema.prefix = prefix;
        SQLSchema.autoIncrement = autoIncrement;

        addUpdates();
        doUpdates(conn);
    }

    private static void doUpdates(Connection conn) {
        List<String> ids = Lists.newArrayList();
        PreparedStatement statement;
        // Get updates already done
        try {
            statement = conn.prepareStatement("SELECT id FROM " + prefix + "Updates");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                ids.add(rs.getString("id"));
            }
        } catch (Exception e) {
        } // Ignore. Just missing the updates table for now

        // Run updates
        for (SchemeUpdate update : updates) {
            if (ids.contains(update.id)) continue; // Update was already done, skip
            try {
                // Update!
                logger.info("Running Update {} - {}", update.id, update.description);
                statement = conn.prepareStatement(update.sql);
                statement.execute();

                // Insert the update key so as to not run the update again
                statement = conn.prepareStatement("INSERT INTO " + prefix + "Updates (id,description) VALUES(?,?);");
                statement.setString(1, update.id);
                statement.setString(2, update.description);
                statement.executeUpdate();

                // Add the id, just to make sure it doesn't run twice
                ids.add(update.id);
            } catch (SQLException e) {
                logger.error("Failed to apply update", e);
            }
        }
    }

    private static void addUpdates() {
        addUpdate("11.9.2014.1", "Add Updates Table", "CREATE TABLE IF NOT EXISTS %PREFIX%Updates(" +
                "id VARCHAR(20) NOT NULL," +
                "description VARCHAR(50) NOT NULL," +
                "PRIMARY KEY(id)" +
                ");");

        addUpdate("11.9.2014.2", "Add Servers Table", "CREATE TABLE IF NOT EXISTS %PREFIX%Servers(" +
                "uuid CHAR(36) NOT NULL," +
                "PRIMARY KEY(uuid)" +
                ");");

        addUpdate("11.9.2014.3", "Add Worlds Table", "CREATE TABLE IF NOT EXISTS %PREFIX%Worlds(" +
                "dim INTEGER NOT NULL," +
                "server CHAR(36) NOT NULL," +
                "CONSTRAINT fk_worlds_server FOREIGN KEY(server) REFERENCES %PREFIX%Servers(uuid) ON DELETE CASCADE," +
                "PRIMARY KEY(server, dim)" +
                ");");

        addUpdate("11.9.2014.4", "Add Teleports Table", "CREATE TABLE IF NOT EXISTS %PREFIX%Teleports(" +
                "id INTEGER %AUTOINCREMENT%," +
                "server CHAR(36) NOT NULL," +
                "dim INTEGER NOT NULL," +
                "x FLOAT NOT NULL," +
                "y FLOAT NOT NULL," +
                "z FLOAT NOT NULL," +
                "yaw FLOAT NOT NULL," +
                "pitch FLOAT NOT NULL," +
                "CONSTRAINT fk_teleports_server FOREIGN KEY(server) REFERENCES %PREFIX%Servers(uuid) ON DELETE CASCADE," +
                "CONSTRAINT fk_teleports_dim FOREIGN KEY(server, dim) REFERENCES %PREFIX%Worlds(server, dim) ON DELETE CASCADE," +
                "PRIMARY KEY(id)" +
                ");");

        addUpdate("11.9.2014.5", "Add Residents Table", "CREATE TABLE IF NOT EXISTS %PREFIX%Residents(" +
                "uuid CHAR(36) NOT NULL," +
                "name VARCHAR(100) NOT NULL," +
                "joined BIGINT NOT NULL," +
                "lastOnline BIGINT NOT NULL," +
                "extraBlocks INTEGER," +
                "PRIMARY KEY(uuid)" +
                ");");

        addUpdate("11.9.2014.6", "Add Towns Table", "CREATE TABLE IF NOT EXISTS %PREFIX%Towns(" +
                "name VARCHAR(50) NOT NULL," +
                "isAdminTown BOOLEAN," +
                "extraBlocks INTEGER," +
                "maxPlots INTEGER," +
                "spawn INTEGER," +
                "CONSTRAINT fk_town_spawn FOREIGN KEY(spawn) REFERENCES %PREFIX%Teleports(id) ON DELETE SET NULL," +
                "PRIMARY KEY(name)" +
                ");");

        addUpdate("11.9.2014.7", "Add Nations Table", "CREATE TABLE IF NOT EXISTS %PREFIX%Nations(" +
                "name VARCHAR(32) NOT NULL," +
                "PRIMARY KEY(name)" +
                ");");

        addUpdate("11.9.2014.8", "Add Ranks Table", "CREATE TABLE IF NOT EXISTS %PREFIX%Ranks(" +
                "name VARCHAR(100) NOT NULL," +
                "town VARCHAR(50) NOT NULL," +
                "isDefault BOOLEAN NOT NULL," +
                "CONSTRAINT fk_ranks_town FOREIGN KEY(town) REFERENCES %PREFIX%Towns(name) ON UPDATE CASCADE ON DELETE CASCADE," +
                "PRIMARY KEY(town, name)" +
                ");");

        addUpdate("11.9.2014.9", "Add RankPermissions Table", "CREATE TABLE IF NOT EXISTS %PREFIX%RankPermissions(" +
                "node VARCHAR(200) NOT NULL," +
                "town VARCHAR(50) NOT NULL," +
                "rank VARCHAR(100) NOT NULL," +
                "CONSTRAINT fk_rankperms_town FOREIGN KEY(town) REFERENCES %PREFIX%Towns(name) ON UPDATE CASCADE ON DELETE CASCADE," +
                "CONSTRAINT fk_rankperms_rank FOREIGN KEY(town, rank) REFERENCES %PREFIX%Ranks(town, name) ON DELETE CASCADE," +
                "PRIMARY KEY(node, rank, town)" +
                ");");

        addUpdate("11.9.2014.10", "Add TownBlocks Table", "CREATE TABLE IF NOT EXISTS %PREFIX%TownBlocks(" +
                "server CHAR(36) NOT NULL," +
                "town VARCHAR(50) NOT NULL," +
                "dim INTEGER NOT NULL," +
                "x INTEGER NOT NULL," +
                "z INTEGER NOT NULL," +
                "CONSTRAINT fk_townblocks_server FOREIGN KEY(server) REFERENCES %PREFIX%Servers(uuid) ON DELETE CASCADE," +
                "CONSTRAINT fk_townblocks_town FOREIGN KEY(town) REFERENCES %PREFIX%Towns(name) ON UPDATE CASCADE ON DELETE CASCADE," +
                "CONSTRAINT fk_townblocks_dim FOREIGN KEY(server, dim) REFERENCES %PREFIX%Worlds(server, dim) ON DELETE CASCADE," +
                "PRIMARY KEY(server, dim, x, z)" +
                ");");

        addUpdate("10.9.2014.11", "Add TownPlots Table", "CREATE TABLE IF NOT EXISTS %PREFIX%TownPlots(" +
                "id INTEGER %AUTOINCREMENT%," +
                "name VARCHAR(50) NOT NULL," +
                "server CHAR(36) NOT NULL," +
                "town VARCHAR(50) NOT NULL," +
                "dim INTEGER NOT NULL," +
                "x1 INTEGER NOT NULL," +
                "y1 INTEGER NOT NULL," +
                "z1 INTEGER NOT NULL," +
                "x2 INTEGER NOT NULL," +
                "y2 INTEGER NOT NULL," +
                "z2 INTEGER NOT NULL," +
                "CONSTRAINT fk_townplots_server FOREIGN KEY(server) REFERENCES %PREFIX%Servers(uuid) ON DELETE CASCADE," +
                "CONSTRAINT fk_townplots_town FOREIGN KEY(town) REFERENCES %PREFIX%Towns(name) ON UPDATE CASCADE ON DELETE CASCADE," +
                "CONSTRAINT fk_townplots_dim FOREIGN KEY(server, dim) REFERENCES %PREFIX%Worlds(server, dim) ON DELETE CASCADE," +
                "PRIMARY KEY(id)" +
                ");");

        // Flag Tables
        addUpdate("10.9.2014.12", "Add UniverseFlags Table", "CREATE TABLE IF NOT EXISTS %PREFIX%UniverseFlags(" +
                "name VARCHAR(50) NOT NULL," +
                "serializedValue VARCHAR(400)," +
                "PRIMARY KEY(name)" +
                ");");

        addUpdate("10.9.2014.13", "Add ServerFlags Table", "CREATE TABLE IF NOT EXISTS %PREFIX%ServerFlags(" +
                "name VARCHAR(50) NOT NULL," +
                "serializedValue VARCHAR(400)," +
                "server CHAR(36) NOT NULL," +
                "CONSTRAINT fk_sf_server FOREIGN KEY(server) REFERENCES %PREFIX%Servers(uuid) ON DELETE CASCADE," +
                "PRIMARY KEY(server, name)" +
                ");");

        addUpdate("10.9.2014.14", "Add WorldFlags Table", "CREATE TABLE IF NOT EXISTS %PREFIX%WorldFlags(" +
                "name VARCHAR(50) NOT NULL," +
                "serializedValue VARCHAR(400)," +
                "server CHAR(36) NOT NULL," +
                "dim INTEGER NOT NULL," +
                "CONSTRAINT fk_wf_server FOREIGN KEY(server) REFERENCES %PREFIX%Servers(uuid) ON DELETE CASCADE," +
                "CONSTRAINT fk_wf_dim FOREIGN KEY(server, dim) REFERENCES %PREFIX%Worlds(server, dim) ON DELETE CASCADE," +
                "PRIMARY KEY(server, dim, name)" +
                ");");

        addUpdate("10.9.2014.15", "Add TownFlags Table", "CREATE TABLE IF NOT EXISTS %PREFIX%TownFlags(" +
                "name VARCHAR(50) NOT NULL," +
                "serializedValue VARCHAR(400)," +
                "town VARCHAR(50) NOT NULL," +
                "CONSTRAINT fk_tf_town FOREIGN KEY(town) REFERENCES %PREFIX%Towns(name) ON UPDATE CASCADE ON DELETE CASCADE," +
                "PRIMARY KEY(town, name)" +
                ");");

        addUpdate("10.9.2014.16", "Add PlotFlags Table", "CREATE TABLE IF NOT EXISTS %PREFIX%PlotFlags(" +
                "name VARCHAR(50) NOT NULL," +
                "serializedValue VARCHAR(400)," +
                "plot INTEGER NOT NULL," +
                "server CHAR(36) NOT NULL," +
                "CONSTRAINT fk_pf_plot FOREIGN KEY(plot) REFERENCES %PREFIX%TownPlots(id) ON DELETE CASCADE," +
                "CONSTRAINT fk_pf_server FOREIGN KEY(server) REFERENCES %PREFIX%Servers(uuid) ON DELETE CASCADE," +
                "PRIMARY KEY(plot, server, name)" +
                ");");

        // Create "Join" Tables
        addUpdate("10.9.2014.17", "Add ResidentsToTowns Table", "CREATE TABLE IF NOT EXISTS %PREFIX%ResidentsToTowns(" +
                "resident CHAR(36) NOT NULL," +
                "town VARCHAR(50) NOT NULL," +
                "rank VARCHAR(100) NOT NULL," +
                "CONSTRAINT fk_rts_resident FOREIGN KEY(resident) REFERENCES %PREFIX%Residents(uuid) ON DELETE CASCADE," +
                "CONSTRAINT fk_rts_town FOREIGN KEY(town) REFERENCES %PREFIX%Towns(name) ON UPDATE CASCADE ON DELETE CASCADE," +
                "CONSTRAINT fk_rts_rank FOREIGN KEY(town, rank) REFERENCES %PREFIX%Ranks(town, name) ON UPDATE CASCADE ON DELETE CASCADE," +
                "PRIMARY KEY(resident, town)" +
                ");");

        addUpdate("10.9.2014.18", "Add ResidentsToPlots", "CREATE TABLE IF NOT EXISTS %PREFIX%ResidentsToPlots(" +
                "resident CHAR(36) NOT NULL," +
                "plot INTEGER NOT NULL," +
                "isOwner BOOLEAN," + // false if it's ONLY whitelisted, if neither then shouldn't be in this list
                "server CHAR(36) NOT NULL," + // Just to shrink the number of results
                "CONSTRAINT fk_rtp_resident FOREIGN KEY(resident) REFERENCES %PREFIX%Residents(uuid) ON DELETE CASCADE," +
                "CONSTRAINT fk_rtp_server FOREIGN KEY(server) REFERENCES %PREFIX%Servers(uuid) ON DELETE CASCADE," +
                "CONSTRAINT fk_rtp_plot FOREIGN KEY(plot) REFERENCES %PREFIX%TownPlots(id) ON DELETE CASCADE," +
                "PRIMARY KEY(resident, plot)" +
                ");");

        addUpdate("10.9.2014.19", "Add TownsToNations Table", "CREATE TABLE IF NOT EXISTS %PREFIX%TownsToNations(" +
                "town VARCHAR(50) NOT NULL," +
                "nation VARCHAR(32) NOT NULL," +
                "rank CHAR(1) DEFAULT 'T'," +
                "CONSTRAINT fk_ttn_town FOREIGN KEY(town) REFERENCES %PREFIX%Towns(name) ON UPDATE CASCADE ON DELETE CASCADE," +
                "CONSTRAINT fk_ttn_nation FOREIGN KEY(nation) REFERENCES %PREFIX%Nations(name) ON UPDATE CASCADE ON DELETE CASCADE," +
                "PRIMARY KEY(town, nation)" +
                ");");

        addUpdate("10.9.2014.20", "Add BlockWhitelists Table", "CREATE TABLE IF NOT EXISTS %PREFIX%BlockWhitelists(" +
                "id INTEGER %AUTOINCREMENT%," +
                "server CHAR(36) NOT NULL," +
                "dim INTEGER NOT NULL," +
                "x INTEGER NOT NULL," +
                "y INTEGER NOT NULL," +
                "z INTEGER NOT NULL," +
                "town VARCHAR(50) NOT NULL," +
                "flag VARCHAR(50) NOT NULL," +
                "CONSTRAINT fk_bw_server FOREIGN KEY(server) REFERENCES %PREFIX%Servers(uuid) ON DELETE CASCADE," +
                "CONSTRAINT fk_bw_world FOREIGN KEY(server, dim) REFERENCES %PREFIX%Worlds(server, dim) ON DELETE CASCADE," +
                "CONSTRAINT fk_bw_town FOREIGN KEY(town) REFERENCES %PREFIX%Towns(name) ON UPDATE CASCADE ON DELETE CASCADE," +
                "CONSTRAINT fk_bw_flag FOREIGN KEY(town, flag) REFERENCES %PREFIX%TownFlags(town, name) ON DELETE CASCADE," +
                "PRIMARY KEY(id)" +
                ");");

        addUpdate("10.9.2014.21", "Add SelectedTown Table", "CREATE TABLE IF NOT EXISTS %PREFIX%SelectedTown(" +
                "resident CHAR(36) NOT NULL," +
                "town VARCHAR(50) NOT NULL," +
                "CONSTRAINT fk_st_resident FOREIGN KEY(resident) REFERENCES %PREFIX%Residents(uuid) ON DELETE CASCADE," +
                "CONSTRAINT fk_st_town FOREIGN KEY(town) REFERENCES %PREFIX%Towns(name) ON UPDATE CASCADE ON DELETE CASCADE," +
                "PRIMARY KEY(resident)" +
                ");");

        addUpdate("10.9.2014.22", "Add Friends Table", "CREATE TABLE IF NOT EXISTS %PREFIX%Friends(" +
                "resident1 CHAR(36) NOT NULL," +
                "resident2 CHAR(36) NOT NULL," +
                "CONSTRAINT fk_st_resident1 FOREIGN KEY(resident1) REFERENCES %PREFIX%Residents(uuid) ON DELETE CASCADE," +
                "CONSTRAINT fk_st_resident2 FOREIGN KEY(resident2) REFERENCES %PREFIX%Residents(uuid) ON DELETE CASCADE," +
                "PRIMARY KEY(resident1, resident2)" +
                ");");

        addUpdate("10.9.2014.23", "Add FriendRequests Table", "CREATE TABLE IF NOT EXISTS %PREFIX%FriendRequests(" +
                "resFrom CHAR(36) NOT NULL," +
                "resTo CHAR(36) NOT NULL," +
                "CONSTRAINT fk_fr_resfrom FOREIGN KEY(resFrom) REFERENCES %PREFIX%Residents(uuid) ON DELETE CASCADE," +
                "CONSTRAINT fk_fr_resto FOREIGN KEY(resTo) REFERENCES %PREFIX%Residents(uuid) ON DELETE CASCADE," +
                "PRIMARY KEY(resFrom, resTo)" +
                ");");

        addUpdate("10.9.2014.24", "Add TownInvites Table", "CREATE TABLE IF NOT EXISTS %PREFIX%TownInvites(" +
                "resident CHAR(36) NOT NULL," +
                "town VARCHAR(50) NOT NULL," +
                "CONSTRAINT fk_ti_resident FOREIGN KEY(resident) REFERENCES %PREFIX%Residents(uuid) ON DELETE CASCADE," +
                "CONSTRAINT fk_ti_town FOREIGN KEY(town) REFERENCES %PREFIX%Towns(name) ON UPDATE CASCADE ON DELETE CASCADE," +
                "PRIMARY KEY(resident, town)" +
                ");");
    }

    private static void addUpdate(final String id, final String description, final String sql) {
        updates.add(new SchemeUpdate(id, description, sql.replace("%PREFIX%", prefix).replace("%AUTOINCREMENT%", autoIncrement)));
    }

    /**
     * Defines a Scheme Update
     */
    public static class SchemeUpdate {
        public final String id;
        public final String description;
        public final String sql;

        public SchemeUpdate(final String id, final String description, final String sql) {
            this.id = id;
            this.description = description;
            this.sql = sql;
        }
    }
}
