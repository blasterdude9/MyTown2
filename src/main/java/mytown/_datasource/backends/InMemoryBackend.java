package mytown._datasource.backends;

import mytown._datasource.DatasourceBackend;
import mytown._datasource.DatasourceTask;

/**
 * @author Joe Goett
 */
public class InMemoryBackend extends DatasourceBackend {
    @Override
    protected boolean init() {
        return true;
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
