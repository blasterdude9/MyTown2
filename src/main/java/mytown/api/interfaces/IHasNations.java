package mytown.api.interfaces;

import com.google.common.collect.ImmutableList;
import mytown.entities.Nation;

/**
 * @author Joe Goett
 */
public interface IHasNations {
    public void addNation(Nation nation);
    public void removeNation(Nation nation);
    public boolean hasNation(Nation nation);
    public ImmutableList<Nation> getNations();
}
