package ws.palladian.helper.collection;

import it.unimi.dsi.fastutil.floats.Float2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>
 * Access objects indexed by a one dimensional numeric index, e.g. get all entries with value > 123.45. Running such
 * range queries on databases is darn slow and can be replaced using this range map.
 * </p>
 * <p>
 * This is a more memory efficient version of the generic range map and is used for float=>[id] (sets of integers)
 * </p>
 *
 * <p>
 * Complexity: O(n)
 * </p>
 *
 * @author David Urbansky
 */
public class Number2IdRangeMap extends Float2ObjectAVLTreeMap<IntOpenHashSet> {
    /**
     * <p>
     * Given a seed and a comparison type, get all values that adhere to the condition.
     * </p>
     *
     * @param v              The seed value.
     * @param comparisonType The comparison type.
     * @return An ordered list of values.
     */
    public IntArrayList getValues(double v, ComparisonType comparisonType) {
        return getValues((float) v, comparisonType);
    }

    public IntArrayList getValues(float v, ComparisonType comparisonType) {
        IntArrayList values = new IntArrayList();

        if (comparisonType == ComparisonType.EQUALS) {
            IntOpenHashSet c = get(v);
            if (c != null) {
                values.addAll(c);
            }
            return values;
        }

        boolean startCollecting = false;
        for (Float2ObjectMap.Entry<IntOpenHashSet> entry : float2ObjectEntrySet()) {
            IntOpenHashSet value = entry.getValue();
            if (value == null) {
                continue;
            }

            if (startCollecting) {
                values.addAll(value);
                continue;
            }

            float v1 = entry.getFloatKey();

            boolean smaller = v1 < v;
            boolean smallerEquals = v1 <= v;
            boolean bigger = v1 > v;
            boolean biggerEquals = v1 >= v;

            if ((comparisonType == ComparisonType.LESS && smaller) || (comparisonType == ComparisonType.LESS_EQUALS && smallerEquals) || (comparisonType == ComparisonType.MORE
                    && bigger) || (comparisonType == ComparisonType.MORE_EQUALS && biggerEquals)) {
                values.addAll(value);

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
    public IntOpenHashSet getValuesBetween(double lowerBound, double upperBound) {
        return getValuesBetween((float) lowerBound, (float) upperBound);
    }

    public IntOpenHashSet getValuesBetween(float lowerBound, float upperBound) {
        IntOpenHashSet values = new IntOpenHashSet();

        for (Float2ObjectMap.Entry<IntOpenHashSet> entry : float2ObjectEntrySet()) {
            float v = entry.getFloatKey();
            if (v >= lowerBound && v <= upperBound) {
                values.addAll(entry.getValue());
            }

            if (v > upperBound) {
                break;
            }
        }

        return values;
    }

    public void put(double key, int c) {
        this.put((float) key, c);
    }

    public void put(float key, int c) {
        IntOpenHashSet vs = get(key);
        if (vs == null) {
            vs = new IntOpenHashSet();
            put(key, vs);
        }
        vs.add(c);
    }

    public void add(float key, int id) {
        IntOpenHashSet existingIds = get(key);
        if (existingIds == null) {
            existingIds = new IntOpenHashSet(1);
            put(key, existingIds);
        }
        existingIds.add(id);
    }

    public void add(float key, IntOpenHashSet ids) {
        IntOpenHashSet existingIds = get(key);
        if (existingIds == null) {
            existingIds = new IntOpenHashSet(ids.size());
            put(key, existingIds);
        }
        existingIds.addAll(ids);
    }

    public static float condenseKey(double value) {
        return condenseKey((float) value);
    }

    /**
     * Fewer keys lead to more memory efficiency. If we store 0.123 and 0.124 together in 0.12, we save memory for keys and value sets.
     *
     * @param value The key to condense.
     * @return A rounded version of the key.
     */
    public static float condenseKey(float value) {
        return (float) MathHelper.round(value, 2);
    }
}
