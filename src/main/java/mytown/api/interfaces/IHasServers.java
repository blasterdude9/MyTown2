package mytown.api.interfaces;

import com.google.common.collect.ImmutableList;
import mytown.entities.Server;

/**
 * @author Joe Goett
 */
public interface IHasServers {
    public void addServer(Server server);
    public void removeServer(Server server);
    public boolean hasServer(Server server);
    public ImmutableList<Server> getServers();
}
