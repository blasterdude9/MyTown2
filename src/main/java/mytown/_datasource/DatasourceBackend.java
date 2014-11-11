package mytown._datasource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Joe Goett
 */
public abstract class DatasourceBackend implements Runnable {
    protected final Logger log = LogManager.getLogger("MyTown2.Datasource.Backend");
    private boolean initialized = false;

    /**
     * Initializes the backend
     * @return
     */
    protected abstract boolean init();

    /* ----- Queries ----- */

    /**
     * Runs the task as an insert task
     * @param task The task to run
     */
    protected abstract void insert(DatasourceTask task);

    /**
     * Runs the task as an update task
     * @param task The task to run
     */
    protected abstract void update(DatasourceTask task);

    /**
     * Runs the task as an delete task
     * @param task The task to run
     */
    protected abstract void delete(DatasourceTask task);

    /* ----- Threaded Implementation ----- */

    protected final DatasourceTask getTask() {
        return Datasource.get().tasks.poll();
    }

    private void runTask(DatasourceTask task) {
        if (task == null) return;
        switch (task.type) {
            case INSERT:
                insert(task);
                break;
            case UPDATE:
                update(task);
                break;
            case DELETE:
                delete(task);
                break;
        }
    }

    @Override
    public final void run() {
        if (!initialized) {
            initialized = init();
        }
        while (Datasource.get().running.get() || !Datasource.get().tasks.isEmpty()) {
            runTask(getTask());
            // TODO Sleep?
        }
    }
}
