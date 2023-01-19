package ws.palladian.core.featurevector;

import ws.palladian.core.ImmutableFeatureVectorEntry;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.collection.Vector.VectorEntry;

import java.util.*;
import java.util.Map.Entry;

/**
 * This class keeps shared state and logic of multiple
 * {@link FlyweightFeatureVector} instances. It handles e.g. key-based lookup,
 * get, set, and iteration for {@link Value} arrays. This class is and must be
 * immutable, once constructed.
 *
 * @author Philipp Katz
 */
public class FlyweightVectorSchema {

    private final Map<String, Integer> keys;

    public FlyweightVectorSchema(String... keys) {
        this.keys = new LinkedHashMap<>();
        for (int idx = 0; idx < keys.length; idx++) {
            this.keys.put(keys[idx], idx);
        }
    }

    public FlyweightVectorSchema(FeatureInformation featureInformation) {
        this(featureInformation.getFeatureNames().toArray(new String[0]));
    }

    public FlyweightVectorSchema(Collection<String> keys) {
        this(keys.toArray(new String[0]));
    }

    public Value get(String name, Value[] values) {
        Integer index = keys.get(name);
        if (index == null) { // there is no such key
            return null;
        }
        Value value = values[index];
        return value != null ? value : NullValue.NULL;
    }

    public void set(String name, Value value, Value[] values) {
        Integer index = keys.get(name);
        if (index == null) {
            throw new IllegalArgumentException("Schema contains no key with name \"" + name + "\".");
        }
        // TODO : this should also perform type checking!
        values[index] = value;
    }

    public int size() {
        return keys.size();
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(keys.keySet());
    }

    public Iterator<VectorEntry<String, Value>> iterator(final Value[] values) {
        return new AbstractIterator2<VectorEntry<String, Value>>() {
            final Iterator<Entry<String, Integer>> keyIterator = keys.entrySet().iterator();

            @Override
            protected VectorEntry<String, Value> getNext() {
                if (keyIterator.hasNext()) {
                    final Entry<String, Integer> current = keyIterator.next();
                    Value value = values[current.getValue()];
                    if (value == null) {
                        value = NullValue.NULL;
                    }
                    return new ImmutableFeatureVectorEntry(current.getKey(), value);
                }
                return finished();
            }
        };
    }

    public FlyweightVectorBuilder builder() {
        return new FlyweightVectorBuilder(this);
    }

}
