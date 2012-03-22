package ws.palladian.helper.collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

// FIXME make this generic, or better use org.apache.commons.collections15.Bag<E> instead
public class CountMap extends HashMap<Object, Integer> {

    /** The serial version id. */
    private static final long serialVersionUID = -3624991964111312886L;

    public LinkedHashMap<Object, Integer> getSortedMap() {
        return CollectionHelper.sortByValue(this);
    }

    public LinkedHashMap<Object, Integer> getSortedMapDescending() {
        return CollectionHelper.sortByValue(this, false);
    }

    /**
     * Increment the entry with the key by one.
     * 
     * @param key The key of the value that should be incremented.
     */
    public void increment(Object key) {
        Integer count = get(key);
        int counter = count.intValue();
        counter++;
        put(key, counter);
    }

    /**
     * Increment the entry with the key by a certain number.
     * 
     * @param key The key of the value that should be incremented.
     * @param increment The number of increments.
     */
    public void increment(Object key, int increment) {
        Integer count = get(key);
        int counter = count.intValue();
        counter += increment;
        put(key, counter);
    }

    @Override
    public Integer get(Object key) {
        Integer count = super.get(key);

        if (count == null) {
            count = 0;
        }

        return count;
    }

    /**
     * Returns the sum of all counts in the CountMap. Where in contrast, {@link #size()} returns the number of
     * <i>unique</i> items in the CountMap.
     * 
     * @return
     */
    public int totalSize() {
        int totalSize = 0;
        for (Entry<Object, Integer> entry : entrySet()) {
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
    public Set<Object> getObjectsWithHigherCountThan(int count) {
        
        Set<Object> highCountSet = new HashSet<Object>();
        for (java.util.Map.Entry<Object, Integer> entry : entrySet()) {
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
    public <T> Set<T> getObjectsWithCountBetween(int minCount, int maxCount) {

        Set<T> validCountSet = new HashSet<T>();
        for (java.util.Map.Entry<Object, Integer> entry : entrySet()) {
            if (entry.getValue() >= minCount && entry.getValue() <= maxCount) {
                validCountSet.add((T)entry.getKey());
            }
        }

        return validCountSet;
    }

}
