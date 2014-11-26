package mytown._datasource;

import mytown.config.Config;
import mytown.entities.AdminTown;
import mytown.entities.Town;
import mytown.entities.Universe;
import mytown.handlers.SafemodeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    public void start() {
        log.info("Starting datasource with {} threads", Config.dbThreadCount);

        // Initialize the Backend
        if (!DatasourceRegistry.initBackend()) {
            log.error("Failed to initialize Datasource backend!");
            SafemodeHandler.setSafemode(true);
            return;
        }

        // Load the Backend
        DatasourceRegistry.load();

        // Release Backend Now
        DatasourceRegistry.releaseSynchronizedBackend();

        // Create a backend per-thread for task processing
        for (int t = 0; t < Config.dbThreadCount; t++) {
            executorService.execute(DatasourceRegistry.createBackend());
        }
    }

    public void stop() {
        log.info("Stopping datasource");
        log.warn("This may take some time! Please do NOT force stop!");
        log.warn("{} tasks remaining", tasks.size());
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

    /* ----- ORM? ----- */

    private void fieldGrabber(Object obj, Map<String, Object> args, Map<String, Object> where) {
        if (obj == null) return;
        for (Field f : obj.getClass().getFields()) {
            if (!f.isAnnotationPresent(DBField.class)) continue;
            String name = f.getAnnotation(DBField.class).name();
            Object val = null;
            try {
                val = f.get(obj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (args != null) {
                args.put(name, val);
            }
            if (where != null && f.getAnnotation(DBField.class).where()) {
                where.put(name, val);
            }
        }
        for (Method m : obj.getClass().getMethods()) {
            if (!m.isAnnotationPresent(DBField.class)) continue;
            String name = m.getAnnotation(DBField.class).name();
            Object val = null;
            try {
                val = m.invoke(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (args != null) {
                args.put(name, val);
            }
            if (where != null && m.getAnnotation(DBField.class).where()) {
                where.put(name, val);
            }
        }
    }

    public void insert(Object o) {
        if (!o.getClass().isAnnotationPresent(Table.class)) return;
        String tblName = o.getClass().getAnnotation(Table.class).value();
        Map<String, Object> args = new HashMap<String, Object>();
        fieldGrabber(o, args, null);
        addTask(new DatasourceTask(DatasourceTask.Type.INSERT, tblName, args));
    }

    public void update(Object o) {
        if (!o.getClass().isAnnotationPresent(Table.class)) return;
        String tblName = o.getClass().getAnnotation(Table.class).value();
        Map<String, Object> args = new HashMap<String, Object>();
        Map<String, Object> where = new HashMap<String, Object>();
        fieldGrabber(o, args, where);
        addTask(new DatasourceTask(DatasourceTask.Type.UPDATE, tblName, args, where));
    }

    public void delete(Object o) {
        if (!o.getClass().isAnnotationPresent(Table.class)) return;
        String tblName = o.getClass().getAnnotation(Table.class).value();
        Map<String, Object> where = new HashMap<String, Object>();
        fieldGrabber(o, null, where);
        addTask(new DatasourceTask(DatasourceTask.Type.DELETE, tblName, null, where));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface Table {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public static @interface DBField {
        String name();
        boolean where() default false;
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
