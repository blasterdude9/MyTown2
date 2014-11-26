package mytown.entities;

// TODO Implement PlotType

import com.google.common.collect.ImmutableList;
import mytown._datasource.Datasource;
import mytown.api.interfaces.IHasFlags;
import mytown.api.interfaces.IHasResidents;
import mytown.config.Config;
import mytown.entities.flag.Flag;
import mytown.entities.flag.FlagType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Joe Goett
 */
@Datasource.Table("Plots")
public class Plot implements IHasFlags, IHasResidents {
    private int db_ID;
    private final int dim, x1, y1, z1, x2, y2, z2;
    private Town town;
    private String key, name;

    public Plot(String name, Town town, int dim, int x1, int y1, int z1, int x2, int y2, int z2) {
        if (x1 > x2) {
            int aux = x2;
            x2 = x1;
            x1 = aux;
        }

        if (z1 > z2) {
            int aux = z2;
            z2 = z1;
            z1 = aux;
        }

        if (y1 > y2) {
            int aux = y2;
            y2 = y1;
            y1 = aux;
        }
        // Second parameter is always highest
        this.name = name;
        this.town = town;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.dim = dim;


        updateKey();
    }

    @Datasource.DBField(name = "id", where = true)
    public int getDb_ID() {
        return this.db_ID;
    }

    @Datasource.DBField(name = "name")
    public String getName() {
        return name;
    }

    @Datasource.DBField(name = "server")
    public String getServer() {
        return Config.serverID;
    }

    @Datasource.DBField(name = "town")
    public String getTownName() {
        return town.getName();
    }

    @Datasource.DBField(name = "dim")
    public int getDim() {
        return dim;
    }

    @Datasource.DBField(name = "x1")
    public int getStartX() {
        return x1;
    }

    @Datasource.DBField(name = "y1")
    public int getStartY() {
        return y1;
    }

    @Datasource.DBField(name = "z1")
    public int getStartZ() {
        return z1;
    }

    @Datasource.DBField(name = "x2")
    public int getEndX() {
        return x2;
    }

    @Datasource.DBField(name = "y2")
    public int getEndY() {
        return y2;
    }

    @Datasource.DBField(name = "z2")
    public int getEndZ() {
        return z2;
    }

    public String getStartCoordString() {
        return String.format("%s, %s, %s", x1, y1, z1);
    }

    public String getEndCoordString() {
        return String.format("%s, %s, %s", x2, y2, z2);
    }

    public int getStartChunkX() {
        return x1 >> 4;
    }

    public int getStartChunkZ() {
        return z1 >> 4;
    }

    public int getEndChunkX() {
        return x2 >> 4;
    }

    public int getEndChunkZ() {
        return z2 >> 4;
    }

    public Town getTown() {
        return town;
    }

    public String getKey() {
        return key;
    }

    private void updateKey() {
        key = String.format("%s;%s;%s;%s;%s;%s;%s", dim, x1, y1, z1, x2, y2, z2);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDb_ID(int ID) {
        this.db_ID = ID;
    }

    /**
     * Checks if the coords are within this plot and in the same dimension
     *
     * @param dim
     * @param x
     * @param y
     * @param z
     * @return
     */
    public boolean isCoordWithin(int dim, int x, int y, int z) { // TODO Is dim really needed?
        return dim == this.dim && x1 <= x && x <= x2 && y1 <= y && y <= y2 && z1 <= z && z <= z2;
    }

    @Override
    public String toString() {
        return String.format("Plot: {Name: %s, Dim: %s, Start: [%s, %s, %s], End: [%s, %s, %s]}", name, dim, x1, y1, z1, x2, y2, z2);
    }

    /* ---- IHasFlags ----- */

    private List<Flag> flags = new ArrayList<Flag>();

    @Override
    public void addFlag(Flag flag) {
        flags.add(flag);
    }

    @Override
    public boolean hasFlag(FlagType type) {
        for (Flag flag : flags)
            if (flag.flagType.equals(type))
                return true;
        return false;
    }

    @Override
    public ImmutableList<Flag> getFlags() {
        return ImmutableList.copyOf(flags);
    }

    @Override
    public Flag getFlag(FlagType type) {
        for (Flag flag : flags)
            if (flag.flagType.equals(type))
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
    public Object getValueAtCoords(int dim, int x, int y, int z, FlagType flagType) {
        if (!isCoordWithin(dim, x, y, z)) return null;
        return getValue(flagType);
    }

    /* ---- IHasResidents ----- */

    private List<Resident> whitelist = new ArrayList<Resident>();

    @Override
    public void addResident(Resident res) {
        whitelist.add(res);
    }

    @Override
    public void removeResident(Resident res) {
        whitelist.remove(res);
    }

    @Override
    public boolean hasResident(Resident res) {
        return whitelist.contains(res) || owners.contains(res);
    }

    @Override
    public ImmutableList<Resident> getResidents() {
        return ImmutableList.copyOf(whitelist);
    }

    private List<Resident> owners = new ArrayList<Resident>();

    public void addOwner(Resident res) {
        owners.add(res);
    }

    public void removeOwner(Resident res) {
        owners.remove(res);
    }

    public boolean hasOwner(Resident res) {
        return owners.contains(res);
    }

    public ImmutableList<Resident> getOwners() {
        return ImmutableList.copyOf(owners);
    }

    public boolean residentHasFriendInPlot(Resident res) {
        for (Resident r : owners)
            if (r.hasFriend(res))
                return true;
        return false;
    }
}
