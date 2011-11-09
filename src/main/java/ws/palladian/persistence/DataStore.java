package ws.palladian.persistence;

import java.util.Map;

public interface DataStore<K, V> extends Map<K, V> {

    public abstract boolean persist();

    public abstract boolean load();

}
