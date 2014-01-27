package ws.palladian.helper.collection;

import java.util.Collections;
import java.util.Iterator;

/**
 * <p>
 * A vector which can be used instead of <code>null</code> (null object pattern).
 * </p>
 * 
 * @author pk
 * 
 * @param <K>
 * @param <V>
 */
public final class NullVector<K, V> implements Vector<K, V> {

    @Override
    public Iterator<VectorEntry<K, V>> iterator() {
        return Collections.<VectorEntry<K, V>> emptySet().iterator();
    }

    @Override
    public V get(K k) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

}
