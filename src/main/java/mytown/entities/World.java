package mytown.entities;

import com.google.common.collect.ImmutableList;
import mytown._datasource.Datasource;
import mytown.api.interfaces.IHasBlocks;
import mytown.api.interfaces.IHasFlags;
import mytown.api.interfaces.IHasPlots;
import mytown.config.Config;
import mytown.entities.flag.Flag;
import mytown.entities.flag.FlagType;

import java.util.*;

/**
 * @author Joe Goett
 */
@Datasource.Table("Worlds")
public class World implements IHasBlocks, IHasPlots, IHasFlags {
    private int id;
    private Map<String, TownBlock> blocks;
    private Map<Integer, Plot> plots;
    private List<Flag> flags = new ArrayList<Flag>();

    public World(int id) {
        blocks = new Hashtable<String, TownBlock>();
        plots = new Hashtable<Integer, Plot>();
        this.id = id;
    }

    @Datasource.DBField(name = "dim", where = true)
    public int getID() {
        return id;
    }
    
    @Datasource.DBField(name = "server", where = true)
    public String getServerID() {
        return Config.serverID;
    }

    /* ----- IHasTownBlocks ----- */

    @Override
    public void addBlock(TownBlock block) {
        blocks.put(block.getKey(), block);
    }

    @Override
    public void removeBlock(TownBlock block) {
        blocks.remove(block.getKey());
    }

    @Override
    public boolean hasBlock(TownBlock block) {
        return blocks.containsKey(block.getKey());
    }

    @Override
    public ImmutableList<TownBlock> getBlocks() {
        return ImmutableList.copyOf(blocks.values());
    }

    @Override
    public TownBlock getBlockAtCoords(int dim, int x, int z) {
        return blocks.get(String.format(TownBlock.keyFormat, dim, x, z));
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
        for (int x = plot.getStartChunkX(); x <= plot.getEndChunkX(); x++) {
            for (int z = plot.getStartChunkZ(); z <= plot.getEndChunkZ(); z++) {
                TownBlock b = getBlockAtCoords(plot.getDim(), x, z);
                if (b != null) {
                    b.addPlot(plot);
                }
            }
        }

        plots.put(plot.getDb_ID(), plot);
    }

    @Override
    public void removePlot(Plot plot) {
        plots.remove(plot.getDb_ID());
    }

    @Override
    public boolean hasPlot(Plot plot) {
        return plots.containsKey(plot.getDb_ID());
    }

    @Override
    public ImmutableList<Plot> getPlots() {
        return ImmutableList.copyOf(plots.values());
    }

    @Override
    public Plot getPlotAtCoords(int dim, int x, int y, int z) {
        TownBlock b = getBlockAtCoords(dim, x >> 4, z >> 4);
        if (b == null) {
            return null;
        }
        return b.getPlotAtCoords(dim, x, y, z);
    }

    public Plot getPlot(int id) {
        return plots.get(id);
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
        TownBlock block = getBlockAtCoords(dim, x >> 4, z >> 4);
        if (block != null) {
            Object o =  block.getTown().getValueAtCoords(dim, x, y, z, type);
            if (o != null) {
                return o;
            }
        }

        return getValue(type);
    }
}
