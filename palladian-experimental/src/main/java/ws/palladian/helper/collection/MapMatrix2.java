package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;

public class MapMatrix2<K, V> implements Matrix<K, V>, Serializable {
    
    private static final long serialVersionUID = 1L;

    private final Map<Pair<K, K>, V> map;
    
    /** All keys for the x-axis used in the matrix. */
    private final Set<K> keysX;

    /** All keys for the y-axis used in the matrix. */
    private final Set<K> keysY;

    public MapMatrix2() {
        this.map = CollectionHelper.newHashMap();
        this.keysX = new TreeSet<K>();
        this.keysY = new TreeSet<K>();
    }

    @Override
    public V get(K x, K y) {
        return map.get(Pair.of(x,y));
    }

    @Override
    public void set(K x, K y, V value) {
        map.put(Pair.of(x,y), value);
        keysX.add(x);
        keysY.add(y);
    }

    @Override
    public Set<K> getKeysX() {
        return keysX;
    }

    @Override
    public Set<K> getKeysY() {
        return keysY;
    }

    @Override
    public int sizeY() {
        return getKeysY().size();
    }

    @Override
    public int sizeX() {
        return getKeysX().size();
    }

    @Override
    public String asCsv() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public List<Pair<K, V>> getRow(K y) {
        List<Pair<K, V>> row = CollectionHelper.newArrayList();
        for (K x : keysX) {
            V value = get(x, y);
            if (value != null) {
                row.add(Pair.of(x, value));
            }
        }
        return row;
    }

    @Override
    public List<Pair<K, V>> getColumn(K x) {
        List<Pair<K, V>> column = CollectionHelper.newArrayList();
        for (K y : keysY) {
            V value = get(x, y);
            if (value != null) {
                column.add(Pair.of(y, value));
            }
        }
        return column;
    }

}
