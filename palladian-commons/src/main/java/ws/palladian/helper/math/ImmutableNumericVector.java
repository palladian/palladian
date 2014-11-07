package ws.palladian.helper.math;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * A {@link NumericVector} storing data in a {@link Map}.
 * </p>
 * 
 * @author pk
 * 
 * @param <K>
 */
public final class ImmutableNumericVector<K> implements NumericVector<K> {

    private final Map<K, Double> valueMap;

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

    @Override
    public Double get(K k) {
        Validate.notNull(k, "k must not be null");
        Double value = valueMap.get(k);
        return value != null ? value : 0;
    }

    @Override
    public NumericVector<K> add(NumericVector<K> other) {
        Validate.notNull(other, "other must not be null");
        Map<K, Double> addedVector = new HashMap<K, Double>(valueMap);
        for (K key : other.keys()) {
            Double value = other.get(key);
            if (addedVector.containsKey(key)) {
                value += addedVector.get(key);
            }
            addedVector.put(key, value);
        }
        return new ImmutableNumericVector<K>(addedVector);
    }

    @Override
    public double norm() {
        double norm = 0;
        for (Double value : valueMap.values()) {
            norm += value * value;
        }
        return Math.sqrt(norm);
    }

    @Override
    public double sum() {
        double sum = 0;
        for (Double value : valueMap.values()) {
            sum += value;
        }
        return sum;
    }

    @Override
    public double dot(NumericVector<K> other) {
        Validate.notNull(other, "other must not be null");
        double dotProduct = 0;
        for (Entry<K, Double> entry : valueMap.entrySet()) {
            Double otherValue = other.get(entry.getKey());
            if (otherValue != null) {
                dotProduct += entry.getValue() * otherValue;
            }
        }
        return dotProduct;
    }

    @Override
    public double cosine(NumericVector<K> other) {
        Validate.notNull(other, "other must not be null");
        double dotProduct = dot(other);
        return dotProduct != 0 ? dotProduct / (norm() * other.norm()) : 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public double euclidean(NumericVector<K> other) {
        Validate.notNull(other, "other must not be null");
        double distance = 0;
        for (K key : CollectionHelper.distinct(keys(), other.keys())) {
            double value = get(key) - other.get(key);
            distance += value * value;
        }
        return Math.sqrt(distance);
    }

    @Override
    public Set<K> keys() {
        return Collections.unmodifiableSet(valueMap.keySet());
    }

    @Override
    public int size() {
        return valueMap.size();
    }

    @Override
    public String toString() {
        return "Vector " + valueMap;
    }

}
