package mytown.entities;

import com.google.common.base.Joiner;
import mytown.MyTown;
import mytown._datasource.Datasource;

import java.util.*;

/**
 * @author Joe Goett
 */
@Datasource.Table("Ranks")
public class Rank {
    private String key, name;
    private List<String> permissions;
    private Town town;

    public Rank(String name, Town town) {
        //#ilikeinterusageofconstructors
        this(name, new ArrayList<String>(), town);
    }

    public Rank(String name, List<String> permissions, Town town) {
        this.name = name;
        this.town = town;
        this.permissions = permissions;
        updateKey();
    }

    @Datasource.DBField(name = "name", where = true)
    public String getName() {
        return name;
    }

    @Datasource.DBField(name = "town", where = true)
    public String getTownName() {
        return town.getName();
    }

    @Datasource.DBField(name = "isDefault")
    public boolean isDefault() {
        return town.getDefaultRank() == this;
    }

    /**
     * Adds permission to the list
     *
     * @param permission
     * @return false if permission already exists, true otherwise
     */
    public boolean addPermission(String permission) {
        return permissions.add(permission);
    }

    public void addPermissions(Collection<String> permissions) {
        this.permissions.addAll(permissions);
    }

    /**
     * Removes permission from the list
     *
     * @param permission
     * @return false if permission doesn't exist, true otherwise
     */
    public boolean removePermission(String permission) {
        return permissions.remove(permission);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermissionOrSuperPermission(String permission) {
        if (hasPermission(permission))
            return true;
        for (String p : permissions) {
            if (permission.contains(p)) {
                MyTown.instance.log.info("Rank " + getName() + " doesn't contain " + permission + " but contains permission " + p);
                return true;
            }
        }
        return false;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public String getPermissionsString() {
        return Joiner.on(", ").join(getPermissions());
    }

    public String getKey() {
        return key;
    }

    private void updateKey() {
        key = String.format("%s;%s", town.getName(), name);
    }

    public Town getTown() {
        return town;
    }

    @Override
    public String toString() {
        return String.format("Rank: {Name: %s, Town: %s, Permissions: [%s]}", getName(), getTown().getName(), Joiner.on(", ").join(getPermissions()));
    }

    /**
     * Map that holds the name and the rank's permission of all the ranks that are added to a town on creation.
     * And can be configured in the config file.
     */
    public static Map<String, List<String>> defaultRanks = new HashMap<String, List<String>>();
    public static String theDefaultRank;
    public static String theMayorDefaultRank; // ok not the best name
    public static List<String> theOutsiderPerms = new ArrayList<String>();

    public static boolean outsiderPermCheck(String permission) {
        if (theOutsiderPerms.contains(permission)) {
            MyTown.instance.log.info("Returning true since it has permission in the outsider list.");
            return true;
        }
        for (String p : theOutsiderPerms) {
            if (permission.contains(p)) {
                MyTown.instance.log.info("Returning true since it has permission in the outsider list.");
                return true;
            }
        }
        return false;
    }

}
