package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper.Order;

/**
 * <p>
 * A Bag is a collection which allows counting equal items, it behaves similar to a {@link Set}, but counts occurrences
 * of items. It is also often referred to as a <a href="http://en.wikipedia.org/wiki/Multiset">multiset</a>. A typical
 * use case for this class is a bag of words model of a text document, where the counts in this bag represent the
 * frequencies of tokens within the document. The implementation of this class uses a {@link Map} to store the items and
 * their counts. This class mostly adheres to the interface definition of a {@link Collection}. Keep in mind the
 * following peculiarities:
 * <ul>
 * <li>No <code>null</code> entries are allowed.
 * <li>{@link #size()} represents the sum of all counts within this bag; in contrast, to retrieve the number of unique
 * items, use {@link Set#size()} on {@link #unique()}.
 * <li>{@link #iterator()} returns an {@link Iterator} which iterates all items (i.e. also duplicates) within this bag.
 * <li>To retrieve unique items (i.e. no duplicates), use {@link #unique()}.
 * <li>For items which are not present in this bag, {@link #count(Object)} returns a value of zero.
 * <li>Items, which counts are set to zero are removed from the bag.
 * <li>Items with biggest/smallest count can be retrieved using {@link #getMax()} and {@link #getMin()}.
 * <li>You can retrieve copy of this map, where entries are sorted by their counts using {@link #createSorted(Order)}.
 * </ul>
 * </p>
 * 
 * @param <T> The type of the items in this Bag.
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class Bag<T> extends AbstractCollection<T> implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = 1l;

    /**
     * <p>
     * A factory for creating {@link Bag}s.
     * </p>
     * 
     * @author pk
     * 
     * @param <T>
     */
    public static final class BagFactory<T> implements Factory<Bag<T>> {
        @Override
        public Bag<T> create() {
            return Bag.create();
        }
    }

    /** The internal map keeping the data. */
    private final Map<T, Integer> map;

    /**
     * <p>
     * Create an empty Bag.
     * </p>
     * 
     * @return The Bag.
     */
    public static <T> Bag<T> create() {
        return new Bag<T>(new HashMap<T, Integer>());
    }

    /**
     * <p>
     * Create a new Bag and add all items from the given {@link Collection}.
     * </p>
     * 
     * @param collection The collection from which to add items, not <code>null</code>.
     * @return The Bag containing all items from the given collection.
     */
    public static <T> Bag<T> create(Collection<? extends T> collection) {
        Validate.notNull(collection, "collection must not be null");
        Bag<T> bag = create();
        bag.addAll(collection);
        return bag;
    }

    /**
     * <p>
     * Create a new Bag and add all counts from the given {@link Map}.
     * </p>
     * 
     * @param map the map from which to add items, not <code>null</code>.
     * @return The Bag containing all items from the given map.
     */
    public static <T> Bag<T> create(Map<? extends T, ? extends Integer> map) {
        Validate.notNull(map, "map must not be null");
        return create(new HashMap<T, Integer>(map));
    }

    /** Private constructor, instances are created through the static methods. */
    private Bag(Map<T, Integer> map) {
        this.map = map;
    }

    // java.util.AbstractCollection overrides

    @Override
    public boolean add(T item) {
        Validate.notNull(item, "item must not be null");
        add(item, 1);
        return true;
    }

    @Override
    public Iterator<T> iterator() {

        return new AbstractIterator<T>() {

            final Iterator<Entry<T, Integer>> entryIterator = map.entrySet().iterator();
            Entry<T, Integer> currentEntry = null;
            int currentCount;

            @Override
            protected T getNext() throws Finished {
                if (currentEntry != null && currentCount > 0) {
                    currentCount--;
                    return currentEntry.getKey();
                }
                if (entryIterator.hasNext()) {
                    currentEntry = entryIterator.next();
                    currentCount = currentEntry.getValue() - 1;
                    return currentEntry.getKey();
                }
                throw new Finished();
            }

            @Override
            public void remove() {
                if (currentEntry == null) {
                    throw new IllegalStateException();
                }
                int newValue = currentEntry.getValue() - 1;
                if (newValue == 0) {
                    entryIterator.remove();
                } else {
                    currentEntry.setValue(newValue);
                }
            }

        };
    }

    @Override
    public int size() {
        int size = 0;
        for (Integer value : map.values()) {
            size += value;
        }
        return size;
    }

    // specific Bag methods

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
        if (increment != 0) {
            Integer count = count(item);
            map.put(item, count += increment);
        }
    }

    /**
     * <p>
     * Remove all entries of the specified item from this Bag.
     * </p>
     * 
     * @param item The item to remove, not <code>null</code>.
     * @return The old count, or zero in case the item was not present before.
     */
    public int removeAll(T item) {
        return set(item, 0);
    }

    /**
     * <p>
     * Set the count of the specified item to a certain number.
     * </p>
     * 
     * @param item The item for which to set the count, not <code>null</code>.
     * @param count The count to set.
     * @return The old count, or zero in case the item was not present before.
     */
    public int set(T item, int count) {
        Validate.notNull(item, "item must not be null");
        Integer oldValue = (count == 0) ? map.remove(item) : map.put(item, count);
        return oldValue != null ? oldValue : 0;
    }

    /**
     * <p>
     * Get the count of the specified item.
     * </p>
     * 
     * @param item The item for which to get the count, not <code>null</code>.
     * @return The count of the specified item, or zero in case the item is not in this Bag.
     */
    public int count(T item) {
        Validate.notNull(item, "item must not be null");
        Integer count = map.get(item);
        return count == null ? 0 : count;
    }

    /**
     * @return A set with (unique) entries in this Bag.
     */
    public Set<Entry<T, Integer>> unique() {
        return map.entrySet();
    }

    /**
     * @return An iterator over the (unique) items in this Bag.
     */
    public Set<T> uniqueItems() {
        return map.keySet();
    }

    /**
     * @return The {@link Entry} with the highest count, or <code>null</code> in case no entry exists.
     */
    public Entry<T, Integer> getMax() {
        Entry<T, Integer> max = null;
        for (Entry<T, Integer> entry : unique()) {
            if (max == null || max.getValue() < entry.getValue()) {
                max = entry;
            }
        }
        return max;
    }

    /**
     * @return The {@link Entry} with the lowest count, or <code>null</code> in case no entry exists.
     */
    public Entry<T, Integer> getMin() {
        Entry<T, Integer> min = null;
        for (Entry<T, Integer> entry : unique()) {
            if (min == null || min.getValue() > entry.getValue()) {
                min = entry;
            }
        }
        return min;
    }

    /**
     * <p>
     * Create a copy of this map which is sorted by counts.
     * </p>
     * 
     * @param order The sort order, not <code>null</code>.
     * @return A copy of this map with entries sorted by counts.
     */
    public Bag<T> createSorted(Order order) {
        Validate.notNull(order, "order must not be null");
        Map<T, Integer> sorted = CollectionHelper.sortByValue(map, order);
        return new Bag<T>(sorted);
    }

    // equals, hashCode

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Bag<?> other = (Bag<?>)obj;
        if (map == null) {
            if (other.map != null)
                return false;
        } else if (!map.equals(other.map))
            return false;
        return true;
    }

    // toString

    @Override
    public String toString() {
        return "Bag " + map;
    }

}
