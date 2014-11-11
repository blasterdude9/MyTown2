package mytown.api.interfaces;

import com.google.common.collect.ImmutableList;
import mytown.entities.World;

/**
 * @author Joe Goett
 */
public interface IHasWorlds {
    public void addWorld(World world);
    public void removeWorld(World world);
    public boolean hasWorld(World world);
    public ImmutableList<World> getWorlds();
}
