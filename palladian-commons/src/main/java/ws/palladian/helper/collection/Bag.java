package ws.palladian.helper.collection;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.lang3.Validate;
import ws.palladian.helper.collection.CollectionHelper.Order;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * <p>
 * A Bag is a collection which allows counting equal items, it behaves similar to a {@link Set}, but adds functionality
 * to retrieve the occurrence count of an item. It is also often referred to as a <a
 * href="http://en.wikipedia.org/wiki/Multiset">multiset</a>. A typical use case for this class is a bag of words model
 * of a text document, where the counts in this bag represent the frequencies of tokens within the document. The
 * implementation of this class uses a {@link Map} to store the items and their counts. This class mostly adheres to the
 * interface definition of a {@link Collection}. Keep in mind the following peculiarities:
 * <ul>
 * <li>No <code>null</code> entries are allowed.
 * <li>{@link #size()} represents the sum of all counts within this bag; in contrast, to retrieve the number of
 * <b>unique</b> items, use {@link Set#size()} on {@link #unique()}.
 * <li>{@link #iterator()} returns an {@link Iterator} which iterates all items (i.e. also duplicates) within this bag.
 * <li>To retrieve unique items (i.e. no duplicates), use {@link #unique()}.
 * <li>For items which are not present in this bag, {@link #count(Object)} returns a value of zero.
 * <li>Items, which counts are set to zero are removed from the bag.
 * <li>Items with biggest/smallest count can be retrieved using {@link #getMax()} and {@link #getMin()}.
 * <li>You can retrieve a copy of this map, where entries are sorted by their counts using {@link #createSorted(Order)}.
 * </ul>
 * </p>
 *
 * @param <T> The type of the items in this Bag.
 * @author Philipp Katz
 * @author David Urbansky
 */
public class Bag<T> extends AbstractCollection<T> implements Serializable {
    /** The serial version id. */
    private static final long serialVersionUID = 1l;

    /** The internal map keeping the data. */
    private transient Object2IntOpenHashMap<T> map;

    /** The sum of all counts in the map. */
    private transient int size;

    /**
     * Create a new Bag and add all counts from the given {@link Map}.
     *
     * @param map the map from which to add items, not <code>null</code>.
     * @return The Bag containing all items from the given map.
     * @deprecated This was a convenience constructor; starting with Java 1.7, prefer using the real constructor with diamonds.
     */
    @Deprecated
    public static <T> Bag<T> create(Map<? extends T, ? extends Integer> map) {
        Validate.notNull(map, "map must not be null");
        return new Bag<>(new Object2IntOpenHashMap<>(map));
    }

    /**
     * Creates an empty Bag.
     */
    public Bag() {
        this(new Object2IntOpenHashMap<>());
    }

    /**
     * Create a new Bag and add all counts from the given {@link Map}.
     *
     * @param map the map from which to add items, not <code>null</code>.
     */
    public Bag(Map<? extends T, ? extends Integer> map) {
        Validate.notNull(map, "map must not be null");
        this.map = new Object2IntOpenHashMap<>();
        for (Entry<? extends T, ? extends Integer> item : map.entrySet()) {
            add(item.getKey(), item.getValue());
        }
    }

    /**
     * Internal constructor, which does not copy the map. Only by
     * {@link #createSorted(Order)}.
     */
    private Bag(Object2IntOpenHashMap<T> map, int size) {
        this.map = map;
        this.size = size;
    }

    /**
     * Create a new Bag and add all items from the given {@link Iterable}.
     *
     * @param iterable The iterable from which to add items, not <code>null</code>.
     */
    public Bag(Iterable<? extends T> iterable) {
        this();
        Validate.notNull(iterable, "iterable must not be null");
        for (T item : iterable) {
            add(item);
        }
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
                throw FINISHED;
            }

            @Override
            public void remove() {
                if (currentEntry == null) {
                    throw new IllegalStateException();
                }
                int newValue = currentEntry.getValue() - 1;
                size--;
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
        return size;
    }

    // specific Bag methods

    /**
     * <p>
     * Increment the count of the specified item by a certain number.
     * </p>
     *
     * @param item      The item which count should be incremented, not <code>null</code>.
     * @param increment The count by which to increment (negative values decrement).
     * @return The item's new count, after adding.
     */
    public int add(T item, int increment) {
        Validate.notNull(item, "item must not be null");
        int newCount = count(item) + increment;
        map.put(item, newCount);
        size += increment;
        return newCount;
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
     * @param item  The item for which to set the count, not <code>null</code>.
     * @param count The count to set.
     * @return The old count, or zero in case the item was not present before.
     */
    public int set(T item, int count) {
        Validate.notNull(item, "item must not be null");
        Integer oldValue = (count == 0) ? map.remove(item) : map.put(item, count);
        if (oldValue != null) {
            size -= oldValue;
        }
        size += count;
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
     * @return A set with (unique) items in this Bag.
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
        return new Bag<>(new Object2IntOpenHashMap(sorted), size);
    }

    /**
     * <p>
     * Get a map with counts from this Bag.
     * </p>
     *
     * @return A map, where values represent the counts.
     */
    public Map<T, Integer> toMap() {
        return new HashMap<>(map);
    }

    // equals, hashCode

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Bag<?> other = (Bag<?>) obj;
        if (size != other.size) {
            return false;
        }
        return map.equals(other.map);
    }

    // toString

    @Override
    public String toString() {
        return map.toString();
    }

    // serialization code; in case you change the internals of this class, make sure, serialization still works

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(map.size());
        for (Entry<T, Integer> entry : map.entrySet()) {
            out.writeObject(entry.getKey());
            out.writeInt(entry.getValue());
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        map = new Object2IntOpenHashMap<>();
        int numEntries = in.readInt();
        for (int i = 0; i < numEntries; i++) {
            @SuppressWarnings("unchecked")
            T item = (T) in.readObject();
            int count = in.readInt();
            map.put(item, count);
            size += count;
        }
    }

}
