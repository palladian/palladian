package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper.Order;

/**
 * <p>
 * A CountMap is a collection which allows counting equal items, it behaves similar to a {@link Set}, but counts the
 * occurrences of identical item. It is also often referred to as a "Bag". Typical use cases might be a "bag of words"
 * model for text document, for example.
 * </p>
 * 
 * @param <T> The type of the items in this CountMap.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @deprecated This class has been replaced with {@link Bag}, which provides a better API and behaves more "intuitive".
 */
@Deprecated
public class CountMap<T> implements Collection<T>, Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = -3624991964111312886L;

    private final Map<T, Integer> map = CollectionHelper.newHashMap();

    public Map<T, Integer> getSortedMap() {
        return CollectionHelper.sortByValue(map);
    }

    public Map<T, Integer> getSortedMapDescending() {
        return CollectionHelper.sortByValue(map, Order.DESCENDING);
    }

    /**
     * <p>
     * Create a new {@link CountMap}.
     * </p>
     */
    public static <T> CountMap<T> create() {
        return new CountMap<T>();
    }

    public static <T> CountMap<T> create(Collection<T> collection) {
        CountMap<T> countMap = new CountMap<T>();
        for (T item : collection) {
            countMap.add(item);
        }
        return countMap;
    }

//    /** Private constructor, use {@link #create()} instead. */
//    private CountMap() {
//
//    }

    /**
     * <p>
     * Increment the count of the specified item by one.
     * </p>
     * 
     * @param item The item which count that should be incremented, not <code>null</code>.
     */
    @Override
    public boolean add(T item) {
        Validate.notNull(item, "item must not be null");

        Integer count = getCount(item);
        int counter = count.intValue();
        counter++;
        map.put(item, counter);
        return true;
    }

    /**
     * <p>
     * Increment the count of the specified item by a certain number.
     * </p>
     * 
     * @param item The item which count should be incremented, not <code>null</code>.
     * @param increment The count by which to increment (negative values decrement).
     */
    public void add(T item, int increment) {
        Validate.notNull(item, "item must not be null");
        
        if (increment == 0) {
            return;
        }

        Integer count = getCount(item);
        int counter = count.intValue();
        counter += increment;
        map.put(item, counter);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        Validate.notNull(c, "c must not be null");
        
        for (T item : c) {
            add(item);
        }
        return true;
    }

    /**
     * <p>
     * Add all items from a {@link CountMap}. The counts are added, this means the count of an existing item is
     * incremented by the count of the item in the given CountMap.
     * </p>
     * 
     * @param c The CountMap from which to add items, not <code>null</code>.
     * @return <code>true</code> (XXX always :)
     */
    public boolean addAll(CountMap<? extends T> c) {
        Validate.notNull(c, "c must not be null");
        
        for (T item : c) {
            add(item, c.getCount(item));
        }
        return true;
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
        Validate.notNull(item, "item must not be null");
        if (count == 0) {
            map.remove(item);
        } else {
            map.put(item, count);
        }
    }

    /**
     * <p>
     * Get the count of the specified item.
     * </p>
     * 
     * @param item The item for which to get the count, not <code>null</code>.
     * @return The count of the specified item.
     */
    public int getCount(Object item) {
        Validate.notNull(item, "item must not be null");

        Integer count = map.get(item);
        return count == null ? 0 : count;
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
     * </p>
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

    public T getHighest() {
        int highest = 0;
        T result = null;
        for (T item : uniqueItems()) {
            int current = getCount(item);
            if (current > highest) {
                result = item;
                highest = current;
            }
        }
        return result;
    }

    public CountMap<T> getHighest(int num) {
        Map<T, Integer> descendingItems = getSortedMapDescending();
        CountMap<T> result = CountMap.create();
        for (Entry<T, Integer> entry : descendingItems.entrySet()) {
            result.add(entry.getKey(), entry.getValue());
            if (result.uniqueItems().size() == num) {
                break;
            }
        }
        return result;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return map.keySet().containsAll(c);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return map.keySet().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return map.keySet().retainAll(c);
    }

    @Override
    public int size() {
        return totalSize();
    }

    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    public <A> A[] toArray(A[] a) {
        return map.keySet().toArray(a);
    }

    public Set<Entry<T, Integer>> entrySet() {
        return map.entrySet();
    }

    public Set<T> keySet() {
        return map.keySet();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CountMap [");
        builder.append(map);
        builder.append("]");
        return builder.toString();
    }

}
