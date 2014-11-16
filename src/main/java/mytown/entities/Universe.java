package mytown.entities;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import mytown.api.interfaces.*;
import mytown.config.Config;
import mytown.entities.flag.Flag;
import mytown.entities.flag.FlagType;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

// TODO Add Datasource calls

/**
 * @author Joe Goett
 */
public class Universe implements IHasServers, IHasResidents, IHasTowns, IHasNations, IHasWorlds, IHasBlocks, IHasPlots, IHasFlags {
    private Map<String, Server> servers;
    private Map<String, Resident> residents;
    private Map<String, Town> towns;
    private Map<String, Nation> nations;

    private Universe() {
        servers = new Hashtable<String, Server>();
        towns = new Hashtable<String, Town>();
        nations = new Hashtable<String, Nation>();
    }

    /* ----- IHasServers ----- */

    @Override
    public void addServer(Server server) {
        servers.put(server.getID(), server);
    }

    @Override
    public void removeServer(Server server) {
        servers.remove(server.getID());
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
        residents.put(res.getUUID().toString(), res);
    }

    @Override
    public void removeResident(Resident res) {
        residents.remove(res.getUUID().toString());
    }

    @Override
    public boolean hasResident(Resident res) {
        return residents.containsKey(res.getUUID().toString());
    }

    @Override
    public ImmutableList<Resident> getResidents() {
        return ImmutableList.copyOf(residents.values());
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
        towns.put(town.getName(), town);
    }

    @Override
    public void removeTown(Town town) {
        towns.remove(town.getName());
    }

    @Override
    public boolean hasTown(Town town) {
        return towns.containsKey(town.getName());
    }

    @Override
    public ImmutableList<Town> getTowns() {
        return ImmutableList.copyOf(towns.values());
    }

    /* ----- IHasNations ----- */

    @Override
    public void addNation(Nation nation) {
        nations.put(nation.getName(), nation);
    }

    @Override
    public void removeNation(Nation nation) {
        nations.remove(nation.getName());
    }

    @Override
    public boolean hasNation(Nation nation) {
        return nations.containsKey(nation.getName());
    }

    @Override
    public ImmutableList<Nation> getNations() {
        return ImmutableList.copyOf(nations.values());
    }

    /* ----- IHasWorlds ----- */

    @Override
    public void addWorld(World world) {
        getServer().addWorld(world);
    }

    @Override
    public void removeWorld(World world) {
        getServer().removeWorld(world);
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
        getServer().addBlock(block);
    }

    @Override
    public void removeBlock(TownBlock block) {
        getServer().removeBlock(block);
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
        getServer().addPlot(plot);
    }

    @Override
    public void removePlot(Plot plot) {
        getServer().removePlot(plot);
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

    /* ----- IHasFlags ----- */

    @Override
    public void addFlag(Flag flag) {
    }

    @Override
    public boolean hasFlag(FlagType type) {
        return false;
    }

    @Override
    public ImmutableList<Flag> getFlags() {
        return null;
    }

    @Override
    public Flag getFlag(FlagType type) {
        return null;
    }

    @Override
    public boolean removeFlag(FlagType type) {
        return false;
    }

    @Override
    public Object getValue(FlagType type) {
        return null;
    }

    @Override
    public Object getValueAtCoords(int dim, int x, int y, int z, FlagType type) {
        return getServer().getValueAtCoords(dim, x, y, z, type);
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
