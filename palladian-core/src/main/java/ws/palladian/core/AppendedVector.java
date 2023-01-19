package ws.palladian.core;

import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CompositeIterator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class AppendedVector extends AbstractFeatureVector {

    private final List<FeatureVector> vectors;

    public AppendedVector(FeatureVector... vectors) {
        this.vectors = Arrays.asList(vectors);
    }

    public AppendedVector(List<FeatureVector> vectors) {
        this.vectors = vectors;
    }

    @Override
    public Value get(String k) {
        for (FeatureVector vector : vectors) {
            Value value = vector.get(k);
            if (value != null && !value.isNull()) {
                return value;
            }
        }
        // XXX returning null in this case would make more sense? see also:
        // ws.palladian.core.FilteredVector
        return NullValue.NULL;
    }

    @Override
    public Iterator<VectorEntry<String, Value>> iterator() {
        return CompositeIterator.fromIterable(vectors);
    }

}
