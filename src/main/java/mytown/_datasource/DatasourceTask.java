package mytown._datasource;

import java.util.Map;

/**
 * @author Joe Goett
 */
public final class DatasourceTask {
    public final Type type;
    public final String tblName;
    public final Map<String, Object> args;
    public final Map<String, Object> keys;

    public DatasourceTask(final Type type, final String tblName, final Map<String, Object> args, final Map<String, Object> keys) {
        this.type = type;
        this.tblName = tblName;
        this.args = args;
        this.keys = keys;
    }

    public DatasourceTask(final Type type, final String tblName, final Map<String, Object> args) {
        this(type, tblName, args, null);
    }

    public static enum Type {
        INSERT,
        UPDATE,
        DELETE
    }
}