package mytown._datasource;

import mytown.config.Config;
import mytown.handlers.SafemodeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Joe Goett
 */
public class Datasource {
    private final Logger log = LogManager.getLogger("MyTown2.Datasource");
    private final ExecutorService executorService = Executors.newFixedThreadPool(Config.dbThreadCount);
    public final AtomicBoolean running = new AtomicBoolean(true);

    public void start() { // TODO Load everything out of the Datasource
        log.info("Starting datasource with {} threads", Config.dbThreadCount);

        // Initialize the Backend
        if (!DatasourceRegistry.initBackend()) {
            log.error("Failed to initialize Datasource backend!");
            SafemodeHandler.setSafemode(true);
            return;
        }

        // Create a backend per-thread for task processing
        for (int t = 0; t < Config.dbThreadCount; t++) {
            executorService.execute(DatasourceRegistry.createBackend());
        }
    }

    public void stop() {
        log.info("Stopping datasource");
        log.warn("This may take some time! Please do NOT force stop!");
        log.info("{} tasks remaining", tasks.size());
        running.set(false);
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // TODO Maybe add a timeout?
        } catch(InterruptedException e) {
        }
        log.info("Datasource stopped.");
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
