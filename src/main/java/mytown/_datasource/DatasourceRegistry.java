package mytown._datasource;

import mytown.MyTown;
import mytown._datasource.backends.InMemoryBackend;
import mytown._datasource.backends.MySQLBackend;
import mytown._datasource.backends.SQLiteBackend;
import mytown.config.Config;
import mytown.core.utils.config.ConfigProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Hashtable;
import java.util.Map;

/**
 * @author Joe Goett
 */
public final class DatasourceRegistry {
    private static Logger logger = LogManager.getLogger();
    private static Map<String, Class<? extends DatasourceBackend>> backends = new Hashtable<String, Class<? extends DatasourceBackend>>();

    // Register Built-in backends
    static {
        register("in-memory", InMemoryBackend.class);
        register("mysql", MySQLBackend.class);
        register("sqlite", SQLiteBackend.class);
    }

    public static DatasourceBackend createBackend() {
        if (!backends.containsKey(Config.dbType.toLowerCase())) {
            logger.error("Unknown Datasource Type!");
            return null;
        }

        try {
            DatasourceBackend backend = (DatasourceBackend) backends.get(Config.dbType.toLowerCase()).newInstance();
            ConfigProcessor.load(MyTown.instance.config, backend.getClass(), backend);
            return backend;
        } catch (Exception e) {
            logger.error("Failed to create Backend!", e);
        }
        return null;
    }

    public static void register(String typeName, Class<? extends DatasourceBackend> clazz) {
        backends.put(typeName.toLowerCase(), clazz);
    }

    public static void register(String typeName, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!clazz.isAssignableFrom(DatasourceBackend.class)) {
                logger.info("Could not register {} as a Datasource Backend!", typeName);
                return;
            }
            register(typeName, (Class<DatasourceBackend>) clazz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private DatasourceRegistry() {}

    public static boolean initBackend() {
        return createBackend().init(); // TODO Insert Server UUID and Worlds if they are not already in the datasource
    }
}
