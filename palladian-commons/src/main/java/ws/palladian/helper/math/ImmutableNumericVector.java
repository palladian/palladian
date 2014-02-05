package ws.palladian.helper.math;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.EntryConverter;
import ws.palladian.helper.collection.Vector;

/**
 * <p>
 * A {@link NumericVector} storing data in a {@link Map}.
 * </p>
 * 
 * @author pk
 * 
 * @param <K>
 */
public final class ImmutableNumericVector<K> extends AbstractNumericVector<K> {

    private final Map<K, Double> valueMap;

    /** Cache the norm of this vector, lazy initialized. */
    private Double norm;

    /** Cache the sum of this vector, lazy initialized. */
    private Double sum;

    /**
     * @return An empty {@link ImmutableNumericVector}.
     */
    public static <K> ImmutableNumericVector<K> empty() {
        return new ImmutableNumericVector<K>(Collections.<K, Double> emptyMap());
    }

    /**
     * <p>
     * Create a new {@link ImmutableNumericVector} from the given value map. The map is copied, making this instance
     * effectively immutable.
     * </p>
     * 
     * @param valueMap The map holding the values, not <code>null</code>.
     */
    public ImmutableNumericVector(Map<K, Double> valueMap) {
        Validate.notNull(valueMap, "valueMap must not be null");
        this.valueMap = new HashMap<K, Double>(valueMap);
    }

    /**
     * <p>
     * Create a new {@link ImmutableNumericVector} by copying the given {@link Vector}.
     * </p>
     * 
     * @param vector The vector with the values, not <code>null</code>.
     */
    public ImmutableNumericVector(Vector<K, Double> vector) {
        Validate.notNull(vector, "vector must not be null");
        valueMap = new HashMap<K, Double>();
        for (VectorEntry<K, Double> entry : vector) {
            valueMap.put(entry.key(), entry.value());
        }
    }

    @Override
    public Double get(K k) {
        Validate.notNull(k, "k must not be null");
        Double value = valueMap.get(k);
        return value != null ? value : 0;
    }

    @Override
    public Set<K> keys() {
        return Collections.unmodifiableSet(valueMap.keySet());
    }

    @Override
    public String toString() {
        return "Vector " + valueMap;
    }

    @Override
    public Iterator<VectorEntry<K, Double>> iterator() {
        Set<Entry<K, Double>> entries = Collections.unmodifiableSet(valueMap.entrySet());
        return CollectionHelper.convert(entries.iterator(), new EntryConverter<K, Double>());
    }

    // these values can be cached, as this class is immutable

    @Override
    public double norm() {
        if (norm == null) {
            norm = super.norm();
        }
        return norm;
    }

    @Override
    public double sum() {
        if (sum == null) {
            sum = super.sum();
        }
        return sum;
    }

    // hashCode + equals

    @Override
    public int hashCode() {
        return valueMap.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ImmutableNumericVector<?> other = (ImmutableNumericVector<?>)obj;
        return valueMap.equals(other.valueMap);
    }

}
