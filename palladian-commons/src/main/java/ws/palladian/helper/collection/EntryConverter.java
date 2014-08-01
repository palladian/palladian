package ws.palladian.helper.collection;

import java.util.Map.Entry;

import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.functional.Function;

public final class EntryConverter<K, V> implements Function<Entry<K, V>, VectorEntry<K, V>> {

    @Override
    public VectorEntry<K, V> compute(final Entry<K, V> input) {
        return new VectorEntry<K, V>() {

            @Override
            public K key() {
                return input.getKey();
            }

            @Override
            public V value() {
                return input.getValue();
            }

        };
    }

}
