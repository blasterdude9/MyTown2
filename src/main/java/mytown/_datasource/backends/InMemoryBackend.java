package mytown._datasource.backends;

import mytown._datasource.DatasourceBackend;
import mytown._datasource.DatasourceTask;

/**
 * @author Joe Goett
 */
public class InMemoryBackend extends DatasourceBackend {
    @Override
    protected boolean init() {
        log.warn("I doubt you really meant to select this Backend. NOTHING WILL BE SAVED!!!!!");
        return true;
    }

    @Override
    protected void load() {
    }

    @Override
    protected void close() {
    }

    @Override
    protected void insert(DatasourceTask task) {
        log.info("Inserting into {}", task.tblName);
    }

    @Override
    protected void update(DatasourceTask task) {
        log.info("Updating into {}", task.tblName);
    }

    @Override
    protected void delete(DatasourceTask task) {
        log.info("Deleting into {}", task.tblName);
    }
}
