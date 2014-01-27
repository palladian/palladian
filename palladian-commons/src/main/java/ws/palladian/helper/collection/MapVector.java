package ws.palladian.helper.collection;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.Validate;

public class MapVector<K, V> implements Vector<K, V> {

    private final Map<K, V> map;

    public MapVector(Map<K, V> map) {
        Validate.notNull(map, "map must not be null");
        this.map = map;
    }

    @Override
    public Iterator<VectorEntry<K, V>> iterator() {
        return CollectionHelper.convert(map.entrySet().iterator(), new EntryConverter<K, V>());
    }

    @Override
    public V get(K k) {
        return map.get(k);
    }
    
    @Override
    public int size() {
        return map.size();
    }

}
