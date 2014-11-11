package mytown._datasource;

import mytown.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Joe Goett
 */
public class Datasource {
    private final Logger log = LogManager.getLogger("MyTown2.Datasource");
    private final ExecutorService executorService = Executors.newFixedThreadPool(Config.dbThreadCount);
    public final AtomicBoolean running = new AtomicBoolean(true);

    public void start() {
        log.info("Starting datasource with {} threads", Config.dbThreadCount);
        for (int t = 0; t < Config.dbThreadCount; t++) {
            executorService.execute(DatasourceRegistry.createBackend());
        }

        HashMap<String, Object> args = new HashMap<String, Object>();
        args.put("name", "Test");

        ArrayList<String> keys = new ArrayList<String>();
        keys.add("name");

        addTask(new DatasourceTask(DatasourceTask.Type.DELETE, "Towns", args, keys));
    }

    public void stop() {
        log.info("Stopping datasource");
        running.set(false);
        // TODO Wait till all tasks are finished
    }

    /* ----- Task System ----- */

    protected final ConcurrentLinkedQueue<DatasourceTask> tasks = new ConcurrentLinkedQueue<DatasourceTask>();

    public void addTask(DatasourceTask task) {
        tasks.add(task);
    }

    /* ----- Singleton ----- */

    private static Datasource instance;

    private Datasource() {}

    public static synchronized Datasource get() {
        if (instance == null)
            instance = new Datasource();
        return instance;
    }
}
