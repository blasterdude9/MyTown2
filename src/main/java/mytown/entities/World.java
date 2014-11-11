package mytown.entities;

import com.google.common.collect.ImmutableList;
import mytown.api.interfaces.IHasBlocks;
import mytown.api.interfaces.IHasFlags;
import mytown.api.interfaces.IHasPlots;
import mytown.entities.flag.Flag;
import mytown.entities.flag.FlagType;

import java.util.Hashtable;
import java.util.Map;

/**
 * @author Joe Goett
 */
public class World implements IHasBlocks, IHasPlots, IHasFlags {
    private int id;
    private Map<String, TownBlock> blocks;
    private Map<Integer, Plot> plots;

    public World(int id) {
        blocks = new Hashtable<String, TownBlock>();
        plots = new Hashtable<Integer, Plot>();
        this.id = id;
    }

    public int getID() {
        return id;
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
        return getBlockAtCoords(dim, x >> 4, z >> 4).getPlotAtCoords(dim, x, y, z);
    }

    /* ----- IHasFlags ----- */
    // TODO Per-World Flags

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
        TownBlock block = getBlockAtCoords(dim, x >> 4, z >> 4);
        if (block != null) {
            return block.getTown().getValueAtCoords(dim, x, y, z, type);
        }

        return type.getDefaultValue();
    }
}
