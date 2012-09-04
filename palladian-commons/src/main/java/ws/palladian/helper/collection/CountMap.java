package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

/**
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @param <T>
 */
public class CountMap<T> implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = -3624991964111312886L;

    private final Map<T, Integer> map = CollectionHelper.newHashMap();

    @SuppressWarnings("deprecation")
    public LinkedHashMap<T, Integer> getSortedMap() {
        return CollectionHelper.sortByValue(map);
    }

    @SuppressWarnings("deprecation")
    public LinkedHashMap<T, Integer> getSortedMapDescending() {
        return CollectionHelper.sortByValue(map, false);
    }

    /**
     * <p>
     * Create a new {@link CountMap}.
     * </p>
     */
    public static <T> CountMap<T> create() {
        return new CountMap<T>();
    }

    /** Private constructor, use {@link #create()} instead. */
    private CountMap() {

    }

    /**
     * <p>
     * Increment the count of the specified item by one.
     * </p>
     * 
     * @param item The item which count that should be incremented, not <code>null</code>.
     */
    public void increment(T item) {
        Validate.notNull(map, "map must not be null");

        Integer count = get(item);
        int counter = count.intValue();
        counter++;
        map.put(item, counter);
    }

    /**
     * <p>
     * Increment the count of the specified item by a certain number.
     * </p>
     * 
     * @param item The item which count should be incremented, not <code>null</code>.
     * @param increment The count by which to increment (negative values decrement).
     */
    public void increment(T item, int increment) {
        Validate.notNull(map, "map must not be null");

        Integer count = get(item);
        int counter = count.intValue();
        counter += increment;
        map.put(item, counter);
    }

    /**
     * <p>
     * Set the count of the specified item to a certain number.
     * </p>
     * 
     * @param item The item which count should be set, not <code>null</code>.
     * @param count The count which to set.
     */
    public void set(T item, int count) {
        Validate.notNull(map, "map must not be null");

        map.put(item, count);
    }

    /**
     * <p>
     * Get the count of the specified item.
     * </p>
     * 
     * @param item The item for which to get the count, not <code>null</code>.
     * @return The count of the specified item.
     */
    public int get(T item) {
        Validate.notNull(map, "map must not be null");

        Integer count = map.get(item);

        if (count == null) {
            count = 0;
        }

        return count;
    }

    /**
     * <p>
     * Get the number of unique items.
     * </p>
     * 
     * @return The number of unique items.
     */
    public int uniqueSize() {
        return map.size();
    }

    /**
     * <p>
     * Get the all unique items.
     * </p>
     * 
     * @return Unique items in this {@link CountMap}.
     */
    public Set<T> uniqueItems() {
        return map.keySet();
    }

    /**
     * <p>
     * Returns the sum of all counts. Where in contrast, {@link #uniqueSize()} returns the number of <i>unique</i>
     * items.
     * 
     * @return The number of items.
     */
    public int totalSize() {
        int totalSize = 0;
        for (Entry<T, Integer> entry : map.entrySet()) {
            totalSize += entry.getValue();
        }
        return totalSize;
    }

    /**
     * <p>
     * Get all objects that have more than a certain count.
     * </p>
     * 
     * @param count Objects must have a count greater than count.
     * @return A set of objects with a higher count than specified.
     */
    public Set<T> getObjectsWithHigherCountThan(int count) {
        Set<T> highCountSet = new HashSet<T>();
        for (Entry<T, Integer> entry : map.entrySet()) {
            if (entry.getValue() > count) {
                highCountSet.add(entry.getKey());
            }
        }

        return highCountSet;
    }

    /**
     * <p>
     * Get all objects that occur between and including minCount and maxCount of times.
     * </p>
     * 
     * @param minCount Objects must have a count greater or equal than minCount.
     * @param maxCount Objects must have a count less or equal than maxCount.
     * @return A set of objects with a higher count than specified.
     */
    public Set<T> getObjectsWithCountBetween(int minCount, int maxCount) {
        Set<T> validCountSet = new HashSet<T>();
        for (Entry<T, Integer> entry : map.entrySet()) {
            if (entry.getValue() >= minCount && entry.getValue() <= maxCount) {
                validCountSet.add(entry.getKey());
            }
        }

        return validCountSet;
    }

}
