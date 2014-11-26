package mytown.entities;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import mytown._datasource.Datasource;
import mytown._datasource.DatasourceTask;
import mytown.api.interfaces.*;
import mytown.config.Config;
import mytown.entities.flag.Flag;
import mytown.entities.flag.FlagType;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.*;

// TODO Add Datasource calls

/**
 * @author Joe Goett
 */
public class Universe implements IHasServers, IHasResidents, IHasTowns, IHasNations, IHasWorlds, IHasBlocks, IHasPlots, IHasFlags {
    private Map<String, Server> servers;
    private Map<String, Resident> residents;
    private Map<String, Town> towns;
    private Map<String, Nation> nations;
    private List<Flag> flags = new ArrayList<Flag>();

    private Universe() {
        servers = new Hashtable<String, Server>();
        residents = new Hashtable<String, Resident>();
        towns = new Hashtable<String, Town>();
        nations = new Hashtable<String, Nation>();
    }

    /* ----- IHasServers ----- */

    @Override
    public void addServer(Server server) {
        addServerNoSave(server);
        Datasource.get().insert(server);

        /*
        Map<String, Object> args = new Hashtable<String, Object>();
        args.put("uuid", server.getID());
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.INSERT, "Servers", args));
        */
    }

    /**
     * Adds the server without saving it to the Datasource.
     * Do NOT use this unless your loading from the Datasource!
     *
     * @param server
     */
    public void addServerNoSave(Server server) {
        servers.put(server.getID(), server);
    }

    @Override
    public void removeServer(Server server) {
        servers.remove(server.getID());
        Datasource.get().delete(server);

        /*
        Map<String, Object> keys = new Hashtable<String, Object>();
        keys.put("uuid", server.getID());
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.DELETE, "Servers", null, keys));
        */
    }

    @Override
    public boolean hasServer(Server server) {
        return servers.containsKey(server.getID());
    }

    @Override
    public ImmutableList<Server> getServers() {
        return ImmutableList.copyOf(servers.values());
    }

    /**
     * Helper to get the current Server instance
     *
     * @return The Server instance for this server
     */
    public Server getServer() {
        return servers.get(Config.serverID);
    }

    /* ----- IHasResidents ----- */

    @Override
    public void addResident(Resident res) {
        addResidentNoSave(res);
        Datasource.get().insert(res);

        /*
        Map<String, Object> args = new Hashtable<String, Object>();
        args.put("uuid", res.getUUID().toString());
        args.put("name", res.getPlayerName());
        args.put("joined", res.getJoinDate());
        args.put("lastOnline", res.getLastOnline());
        args.put("extraBlocks", res.getExtraBlocks());
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.INSERT, "Residents", args));
        */
    }

    /**
     * Adds the Resident without saving it to the Datasource.
     * Do NOT use this unless your loading from the Datasource!
     *
     * @param res
     */
    public void addResidentNoSave(Resident res) {
        residents.put(res.getUUID().toString(), res);
    }

    @Override
    public void removeResident(Resident res) {
        residents.remove(res.getUUID().toString());

        Map<String, Object> keys = new Hashtable<String, Object>();
        keys.put("uuid", res.getUUID().toString());
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.DELETE, "Residents", null, keys));
    }

    @Override
    public boolean hasResident(Resident res) {
        return residents.containsKey(res.getUUID().toString());
    }

    @Override
    public ImmutableList<Resident> getResidents() {
        return ImmutableList.copyOf(residents.values());
    }

    public Resident getResidentByUUID(String uuid) {
        return residents.get(uuid);
    }

    public Resident getResidentByUUID(UUID uuid) {
        return getResidentByUUID(uuid.toString());
    }

    public Resident getOrMakeResident(UUID uuid, String playerName, boolean save) {
        Resident res = residents.get(uuid.toString());
        if (res == null) {
            res = new Resident(uuid.toString(), playerName);
            if (save && res != null) {
                // TODO Save to DB
            }
        }
        return res;
    }

    public Resident getOrMakeResident(UUID uuid, String playerName) {
        return getOrMakeResident(uuid, playerName, true);
    }

    public Resident getOrMakeResident(EntityPlayer player) {
        return getOrMakeResident(player.getPersistentID(), player.getDisplayName());
    }

    public Resident getOrMakeResident(Entity e) {
        if (e instanceof EntityPlayer) {
            return getOrMakeResident((EntityPlayer) e);
        }
        return null;
    }

    public Resident getOrMakeResident(ICommandSender sender) {
        if (sender instanceof EntityPlayer) {
            return getOrMakeResident((EntityPlayer) sender);
        }
        return null;
    }

    public Resident getOrMakeResident(String username) {
        GameProfile profile = MinecraftServer.getServer().func_152358_ax().func_152655_a(username);
        return profile == null ? null : getOrMakeResident(profile.getId(), profile.getName());
    }

    /* ----- IHasTowns ----- */

    @Override
    public void addTown(Town town) {
        addTownNoSave(town);
        Datasource.get().insert(town);

        /*
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("name", town.getName());
        args.put("isAdminTown", (town instanceof AdminTown));
        args.put("extraBlocks", town.getExtraBlocks());
        args.put("maxPlots", town.getMaxPlots());
        args.put("spawn", null); // TODO Save Spawn!
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.INSERT, "Towns", args));
        */
    }

    /**
     * Adds the Town without saving it to the Datasource.
     * Do NOT use this unless your loading from the Datasource!
     *
     * @param town
     */
    public void addTownNoSave(Town town) {
        towns.put(town.getName(), town);
    }

    @Override
    public void removeTown(Town town) {
        towns.remove(town.getName());

        Map<String, Object> keys = new Hashtable<String, Object>();
        keys.put("name", town.getName());
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.DELETE, "Towns", null, keys));
    }

    @Override
    public boolean hasTown(Town town) {
        return towns.containsKey(town.getName());
    }

    @Override
    public ImmutableList<Town> getTowns() {
        return ImmutableList.copyOf(towns.values());
    }

    public Town getTown(String name) {
        return towns.get(name);
    }

    /* ----- IHasNations ----- */

    @Override
    public void addNation(Nation nation) {
        addNationNoSave(nation);
        Datasource.get().insert(nation);

        /*
        Map<String, Object> args = new Hashtable<String, Object>();
        args.put("name", nation.getName());
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.INSERT, "Nations", args));
        */
    }

    /**
     * Adds the Nation without saving it to the Datasource.
     * Do NOT use this unless your loading from the Datasource!
     *
     * @param nation
     */
    public void addNationNoSave(Nation nation) {
        nations.put(nation.getName(), nation);
    }

    @Override
    public void removeNation(Nation nation) {
        nations.remove(nation.getName());

        Map<String, Object> keys = new Hashtable<String, Object>();
        keys.put("name", nation.getName());
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.DELETE, "Nations", null, keys));
    }

    @Override
    public boolean hasNation(Nation nation) {
        return nations.containsKey(nation.getName());
    }

    @Override
    public ImmutableList<Nation> getNations() {
        return ImmutableList.copyOf(nations.values());
    }

    public Nation getNation(String name) {
        return null;
    }

    /* ----- IHasWorlds ----- */

    @Override
    public void addWorld(World world) {
        addWorldNoSave(world);
        Datasource.get().insert(world);

        /*
        Map<String, Object> args = new Hashtable<String, Object>();
        args.put("dim", world.getID());
        args.put("server", Config.serverID);
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.INSERT, "Worlds", args));
        */
    }

    /**
     * Adds the World without saving it to the Datasource.
     * Do NOT use this unless your loading from the Datasource!
     *
     * @param world
     */
    public void addWorldNoSave(World world) {
        getServer().addWorld(world);
    }

    @Override
    public void removeWorld(World world) {
        getServer().removeWorld(world);

        Map<String, Object> keys = new Hashtable<String, Object>();
        keys.put("dim", world.getID());
        keys.put("server", Config.serverID);
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.DELETE, "Worlds", null, keys));
    }

    @Override
    public boolean hasWorld(World world) {
        return getServer().hasWorld(world);
    }

    @Override
    public ImmutableList<World> getWorlds() {
        return getServer().getWorlds(); // TODO Should this return all the worlds in the Universe instead?
    }

    /* ----- IHasTownBlocks ----- */

    @Override
    public void addBlock(TownBlock block) {
        addBlockNoSave(block);
        Datasource.get().insert(block);

        /*
        Map<String, Object> args = new Hashtable<String, Object>();
        args.put("server", Config.serverID);
        args.put("town", block.getTown().getName());
        args.put("dim", block.getDim());
        args.put("x", block.getX());
        args.put("z", block.getZ());
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.INSERT, "TownBlocks", args));
        */
    }

    /**
     * Adds the TownBlock without saving it to the Datasource.
     * Do NOT use this unless your loading from the Datasource!
     *
     * @param block
     */
    public void addBlockNoSave(TownBlock block) {
        getServer().addBlock(block);
    }

    @Override
    public void removeBlock(TownBlock block) {
        getServer().removeBlock(block);

        Map<String, Object> keys = new Hashtable<String, Object>();
        keys.put("server", Config.serverID);
        keys.put("dim", block.getDim());
        keys.put("x", block.getX());
        keys.put("z", block.getZ());
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.DELETE, "TownBlocks", null, keys));
    }

    @Override
    public boolean hasBlock(TownBlock block) {
        return getServer().hasBlock(block);
    }

    @Override
    public ImmutableList<TownBlock> getBlocks() {
        return getServer().getBlocks(); // TODO Should this return all blocks in the Universe instead?
    }

    @Override
    public TownBlock getBlockAtCoords(int dim, int x, int z) {
        return getServer().getBlockAtCoords(dim, x, z);
    }

    @Override
    public int getExtraBlocks() {
        return 0;
    }

    @Override
    public void setExtraBlocks(int extra) {

    }

    @Override
    public int getMaxBlocks() {
        return 0;
    }

    @Override
    public boolean hasMaxAmountOfBlocks() {
        return false;
    }

    /* ----- IHasPlots ----- */

    @Override
    public void addPlot(Plot plot) {
        addPlotNoSave(plot);
        Datasource.get().insert(plot);

        /*
        Map<String, Object> args = new Hashtable<String, Object>();
        args.put("name", plot.getName());
        args.put("server", Config.serverID);
        args.put("town", plot.getTown().getName());
        args.put("dim", plot.getDim());
        args.put("x1", plot.getStartX());
        args.put("y1", plot.getStartY());
        args.put("z1", plot.getStartZ());
        args.put("x2", plot.getEndX());
        args.put("y2", plot.getEndY());
        args.put("z2", plot.getEndZ());
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.INSERT, "TownPlots", args));
        */
    }

    /**
     * Adds the Plot without saving it to the Datasource.
     * Do NOT use this unless your loading from the Datasource!
     *
     * @param plot
     */
    public void addPlotNoSave(Plot plot) {
        getServer().addPlot(plot);
    }

    @Override
    public void removePlot(Plot plot) {
        getServer().removePlot(plot);

        Map<String, Object> keys = new Hashtable<String, Object>();
        keys.put("id", plot.getDb_ID());
        Datasource.get().addTask(new DatasourceTask(DatasourceTask.Type.DELETE, "TownPlots", null, keys));
    }

    @Override
    public boolean hasPlot(Plot plot) {
        return getServer().hasPlot(plot);
    }

    @Override
    public ImmutableList<Plot> getPlots() {
        return getServer().getPlots(); // TODO Should this return all plots in the Universe?
    }

    @Override
    public Plot getPlotAtCoords(int dim, int x, int y, int z) {
        return getServer().getPlotAtCoords(dim, x, y, z);
    }

    public Plot getPlot(int i) {
        return getServer().getPlot(i);
    }

    /* ----- IHasFlags ----- */

    @Override
    public void addFlag(Flag flag) {
        flags.add(flag);
    }

    @Override
    public boolean hasFlag(FlagType type) {
        return false;
    }

    @Override
    public ImmutableList<Flag> getFlags() {
        return ImmutableList.copyOf(flags);
    }

    @Override
    public Flag getFlag(FlagType type) {
        for (Flag flag : flags)
            if (flag.flagType == type)
                return flag;
        return null;
    }

    @Override
    public boolean removeFlag(FlagType type) {
        for (Iterator<Flag> it = flags.iterator(); it.hasNext(); ) {
            if (it.next().flagType == type) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getValue(FlagType type) {
        for (Flag flag : flags) {
            if (flag.flagType == type)
                return flag.getValue();
        }
        return type.getDefaultValue();
    }

    @Override
    public Object getValueAtCoords(int dim, int x, int y, int z, FlagType type) {
        Object o = getServer().getValueAtCoords(dim, x, y, z, type);
        if (o != null) {
            return o;
        }

        return getValue(type);
    }

    /* ----- Singleton ----- */

    private static Universe instance = null;

    /**
     * @return The Singleton instance of Universe
     */
    public static Universe get() {
        if (instance == null)
            instance = new Universe();
        return instance;
    }
}
