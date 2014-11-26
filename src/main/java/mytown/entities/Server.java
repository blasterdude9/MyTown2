package mytown.entities;

import com.google.common.collect.ImmutableList;
import mytown._datasource.Datasource;
import mytown.api.interfaces.IHasBlocks;
import mytown.api.interfaces.IHasFlags;
import mytown.api.interfaces.IHasPlots;
import mytown.api.interfaces.IHasWorlds;
import mytown.entities.flag.Flag;
import mytown.entities.flag.FlagType;

import java.util.*;

/**
 * @author Joe Goett
 */
@Datasource.Table("Servers")
public class Server implements IHasWorlds, IHasBlocks, IHasPlots, IHasFlags {
    private String id;
    private Map<Integer, World> worlds;
    private List<Flag> flags = new ArrayList<Flag>();

    public Server(String id) {
        worlds = new Hashtable<Integer, World>();
        this.id = id;
    }

    @Datasource.DBField(name = "uuid", where = true)
    public String getID() {
        return id;
    }

    /* ----- IHasWorlds ----- */

    @Override
    public void addWorld(World world) {
        worlds.put(world.getID(), world);
    }

    @Override
    public void removeWorld(World world) {
        worlds.remove(world.getID());
    }

    @Override
    public boolean hasWorld(World world) {
        return worlds.containsKey(world.getID());
    }

    @Override
    public ImmutableList<World> getWorlds() {
        return ImmutableList.copyOf(worlds.values());
    }

    public World getWorld(int dim) {
        return worlds.get(dim);
    }

    /* ----- IHasTownBlocks ----- */

    @Override
    public void addBlock(TownBlock block) {
        worlds.get(block.getDim()).addBlock(block);
    }

    @Override
    public void removeBlock(TownBlock block) {
        worlds.remove(block.getDim()).removeBlock(block);
    }

    @Override
    public boolean hasBlock(TownBlock block) {
        return worlds.remove(block.getDim()).hasBlock(block);
    }

    @Override
    public ImmutableList<TownBlock> getBlocks() {
        List<TownBlock> tmpBlockList = new ArrayList<TownBlock>();
        for (World w : worlds.values()) {
            tmpBlockList.addAll(w.getBlocks());
        }
        return ImmutableList.copyOf(tmpBlockList);
    }

    @Override
    public TownBlock getBlockAtCoords(int dim, int x, int z) {
        return worlds.get(dim).getBlockAtCoords(dim, x, z);
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
        worlds.get(plot.getDim()).addPlot(plot);
    }

    @Override
    public void removePlot(Plot plot) {
        worlds.get(plot.getDim()).removePlot(plot);
    }

    @Override
    public boolean hasPlot(Plot plot) {
        return worlds.get(plot.getDim()).hasPlot(plot);
    }

    @Override
    public ImmutableList<Plot> getPlots() {
        List<Plot> tmpPlotList = new ArrayList<Plot>();
        for (World w : worlds.values()) {
            tmpPlotList.addAll(w.getPlots());
        }
        return ImmutableList.copyOf(tmpPlotList);
    }

    @Override
    public Plot getPlotAtCoords(int dim, int x, int y, int z) {
        World w = worlds.get(dim);
        if (w == null) {
            return null;
        }
        return w.getPlotAtCoords(dim, x, y, z);
    }

    public Plot getPlot(int id) {
        for (World w : getWorlds()) {
            Plot p = w.getPlot(id);
            if (p == null) continue;
            return p;
        }

        return null;
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
        return null; // Allow inheritance up the tree
    }

    @Override
    public Object getValueAtCoords(int dim, int x, int y, int z, FlagType type) {
        World w = worlds.get(dim);
        if (w != null) {
            Object o =  w.getValueAtCoords(dim, x, y, z, type);
            if (o != null) {
                return o;
            }
        }

        return getValue(type);
    }
}
