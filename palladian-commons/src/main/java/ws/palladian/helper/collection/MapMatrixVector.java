package ws.palladian.helper.collection;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.Matrix.MatrixVector;

final class MapMatrixVector<K, V> implements MatrixVector<K, V> {

    private final K key;
    private final Map<K, V> map;

    MapMatrixVector(K key, Map<K, V> map) {
        Validate.notNull(key, "key must not be null");
        Validate.notNull(map, "map must not be null");
        this.key = key;
        this.map = map;
    }

    @Override
    public Iterator<VectorEntry<K, V>> iterator() {
        Set<Entry<K, V>> entrySet = Collections.unmodifiableSet(map.entrySet());
        return CollectionHelper.convert(entrySet.iterator(), new EntryConverter<K, V>());
    }

    @Override
    public V get(K k) {
        return map.get(k);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public K key() {
        return key;
    }

    @Override
    public Set<K> keys() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MapMatrixVector<?, ?> other = (MapMatrixVector<?, ?>)obj;
        return map.equals(other.map);
    }

}
