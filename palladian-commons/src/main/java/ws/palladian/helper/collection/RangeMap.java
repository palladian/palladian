package ws.palladian.helper.collection;

import java.util.*;

/**
 * <p>
 * Access objects indexed by a one dimensional numeric index, e.g. get all entries with value > 123.45. Running such
 * range queries on databases is darn slow and can be replaced using this range map.
 * </p>
 *
 * <p>
 * Complexity: O(n)
 * </p>
 * 
 * @author David Urbansky
 */
public class RangeMap<K extends Number, V> extends TreeMap<K, Collection<V>> {

    /**
     * <p>
     * Given a seed and a comparison type, get all values that adhere to the condition.
     * </p>
     * 
     * @param seed The seed value.
     * @param comparisonType The comparison type.
     * @return An ordered list of values.
     */
    public List<V> getValues(K seed, ComparisonType comparisonType) {
        List<V> values = new ArrayList<>();

        if (comparisonType == ComparisonType.EQUALS) {
            values.addAll(get(seed));
            return values;
        }

        double v = seed.doubleValue();
        boolean startCollecting = false;
        for (Map.Entry<K, Collection<V>> entry : this.entrySet()) {

            if (startCollecting) {
                values.addAll(entry.getValue());
                continue;
            }

            double v1 = entry.getKey().doubleValue();

            boolean smaller = v1 < v;
            boolean smallerEquals = v1 <= v;
            boolean bigger = v1 > v;
            boolean biggerEquals = v1 >= v;

            if ((comparisonType == ComparisonType.LESS && smaller)
                    || (comparisonType == ComparisonType.LESS_EQUALS && smallerEquals)
                    || (comparisonType == ComparisonType.MORE && bigger)
                    || (comparisonType == ComparisonType.MORE_EQUALS && biggerEquals)) {

                values.addAll(entry.getValue());

                if (comparisonType == ComparisonType.MORE || comparisonType == ComparisonType.MORE_EQUALS) {
                    startCollecting = true;
                }

            }
        }

        return values;
    }

    /**
     * <p>
     * Get all values within [lowerBound,upperBound].
     * </p>
     * 
     * @param lowerBound The minimum number (inclusive).
     * @param upperBound The maximum number (inclusive).
     * @return A list of object within the given range.
     */
    public List<V> getValuesBetween(K lowerBound, K upperBound) {
        List<V> values = new ArrayList<>();

        double lbv = lowerBound.doubleValue();
        double ubv = upperBound.doubleValue();

        for (Map.Entry<K, Collection<V>> entry : this.entrySet()) {
            double v = entry.getKey().doubleValue();
            if (v >= lbv && v <= ubv) {
                values.addAll(entry.getValue());
            }

            if (v > ubv) {
                break;
            }
        }

        return values;
    }

    public void put(K key, V c) {
        Collection<V> vs = get(key);
        if (vs == null) {
            vs = Collections.synchronizedSet(new HashSet<>());
            put(key, vs);
        }
        vs.add(c);
    }
}
