package ws.palladian.helper.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * <p>
 * This class provides some helper methods for working with collections. <b>Important:</b> If you are looking for a
 * functionality which is not provided here, look in {@link Collections}, {@link Arrays} first, before adding new,
 * redundant methods here!
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class CollectionHelper {

    public static boolean ASCENDING = true;
    public static boolean DESCENDING = false;

    private CollectionHelper() {
        // prevent instantiation.
    }

    /**
     * <p>
     * Sort a {@link Map} by value.
     * </p>
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param map The {@link Map} to sort.
     * @param ascending {@link CollectionHelper#ASCENDING} or {@link CollectionHelper#DESCENDING}.
     * @return A sorted map.
     * @deprecated {@link Map}s are <b>not</b> meant for this use case. Prefer using a {@link List} populated with
     *             {@link Pair}s, sorted as required.
     */
    @Deprecated
    public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(Map<K, V> map, final boolean ascending) {

        LinkedList<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        if (ascending) {
            Collections.sort(list, EntryValueComparator.<K, V> ascending());
        } else {
            Collections.sort(list, EntryValueComparator.<K, V> descending());
        }

        LinkedHashMap<K, V> result = new LinkedHashMap<K, V>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * <p>
     * Sort a {@link Map} by value.
     * </p>
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param map The {@link Map} to sort.
     * @return A sorted map, in ascending order.
     * @deprecated {@link Map}s are <b>not</b> meant for this use case. Prefer using a {@link List} populated with
     *             {@link Pair}s, sorted as required.
     */
    @Deprecated
    public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(Map<K, V> map) {
        return sortByValue(map, CollectionHelper.ASCENDING);
    }

    /**
     * <p>
     * Sort a {@link HashMap} by length of the key string.
     * </p>
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param map The entry set.
     * @param ascending {@link CollectionHelper#ASCENDING} or {@link CollectionHelper#DESCENDING}.
     * @return A sorted map.
     * @deprecated {@link Map}s are <b>not</b> meant for this use case. Prefer using a {@link List} populated with
     *             {@link Pair}s, sorted as required.
     */
    @Deprecated
    public static <V extends Comparable<V>> LinkedHashMap<String, V> sortByStringKeyLength(Map<String, V> map,
            final boolean ascending) {

        LinkedList<Map.Entry<String, V>> list = new LinkedList<Map.Entry<String, V>>(map.entrySet());

        Comparator<Map.Entry<String, V>> comparator = new Comparator<Map.Entry<String, V>>() {
            @Override
            public int compare(Map.Entry<String, V> o1, Map.Entry<String, V> o2) {
                int ret = new Integer(o1.getKey().length()).compareTo(o2.getKey().length());
                return ascending ? ret : -ret;
            }
        };
        Collections.sort(list, comparator);

        LinkedHashMap<String, V> result = new LinkedHashMap<String, V>();
        for (Entry<String, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * <p>
     * Get a key given for a value (1 to 1 {@link Map}s).
     * </p>
     * 
     * @param value The value.
     * @return The key that matches the given value, or <code>null</code> if no such value.
     */
    public static <K, V> K getKeyByValue(Map<K, V> map, V value) {
        for (Entry<K, V> mapEntry : map.entrySet()) {
            if (mapEntry.getValue().equals(value)) {
                return mapEntry.getKey();
            }
        }
        return null;
    }

    /**
     * <p>
     * Print a human readable, line separated output of an Array.
     * </p>
     * 
     * @param array
     */
    public static void print(Object[] array) {
        for (Object o : array) {
            System.out.println(o);
        }
        System.out.println("#Entries: " + array.length);
    }

    /**
     * <p>
     * Print a human readable, line separated output of a {@link Map}.
     * </p>
     * 
     * @param <K>
     * @param <V>
     * @param map
     */
    public static <K, V> void print(Map<K, V> map) {
        print(map, -1);
    }

    public static <K, V> void print(Map<K, V> map, int limit) {
        int c = 0;
        Iterator<Map.Entry<K, V>> mapIterator = map.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<K, V> entry = mapIterator.next();
            System.out.println(entry.getKey() + " : " + entry.getValue());
            c++;
            if (c >= limit && limit > -1) {
                break;
            }
        }
        System.out.println("#Entries: " + map.entrySet().size());
    }

    /**
     * <p>
     * Get a human readable, line separated output of an {@link Iterable}.
     * </p>
     * 
     * @param iterable
     * @return
     */
    public static String getPrint(Iterable<?> iterable) {
        StringBuilder print = new StringBuilder();
        int count = 0;
        for (Object entry : iterable) {
            print.append(entry).append("\n");
            count++;
        }
        print.append("#Entries: ").append(count).append("\n");
        return print.toString();
    }

    /**
     * <p>
     * Print a human readable, line separated output of an {@link Iterable}.
     * </p>
     * 
     * @param iterable
     */
    public static void print(Iterable<?> iterable) {
        System.out.println(getPrint(iterable));
    }

    /**
     * <p>
     * Concatenate two String arrays.
     * </p>
     * 
     * @param array1
     * @param array2
     * @return The concatenated String array consisting of the first, then the second array's items.
     */
    public static String[] concat(String[] array1, String[] array2) {
        String[] helpArray = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, helpArray, 0, array1.length);
        System.arraycopy(array2, 0, helpArray, array1.length, array2.length);
        return helpArray;
    }

    /**
     * <p>
     * Create a new {@link HashMap}. This method allows omitting the type parameter when creating the HashMap:
     * <code>Map&lt;String, Integer&gt; map = CollectionHelper.newHashMap();</code>.
     * </p>
     * 
     * @return
     */
    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<K, V>();
    }

    /**
     * <p>
     * Create a new {@link ArrayList}. This method allows omitting the type parameter when creating the ArrayList:
     * <code>List&lt;String&gt; list = CollectionHelper.newArrayList();</code>.
     * </p>
     * 
     * @return
     */
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<E>();
    }

    /**
     * <p>
     * Create a new {@link HashSet}. This method allows omitting the type parameter when creating the HashSet:
     * <code>Set&lt;String&gt; set = CollectionHelper.newHashSet();</code>.
     * </p>
     * 
     * @return
     */
    public static <E> HashSet<E> newHashSet() {
        return new HashSet<E>();
    }

    /**
     * <p>
     * Remove all <code>null</code> elements in the supplied {@link Iterable}.
     * </p>
     * 
     * @param collection The iterable from which to remove <code>null</code> elements.
     * @return <code>true</code> if any elements were removed, else <code>false</code>.
     */
    public static <T> boolean removeNulls(Iterable<T> iterable) {
        Validate.notNull(iterable, "iterable must not be null");
        return filter(iterable, new Filter<T>() {
            @Override
            public boolean accept(T item) {
                return item != null;
            }
        });
    }

    /**
     * <p>
     * Apply a {@link Filter} to an {@link Iterable}; after applying this method, the Iterable only contains the items
     * which matched the filter.
     * </p>
     * 
     * @param iterable The Iterable to filter, not <code>null</code>.
     * @param filter The Filter to apply, not <code>null</code>.
     * @return <code>true</code> if any items were removed, else <code>false</code>.
     */
    public static <T> boolean filter(Iterable<T> iterable, Filter<T> filter) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(filter, "filter must not be null");

        boolean modified = false;
        Iterator<T> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            T item = iterator.next();
            if (!filter.accept(item)) {
                iterator.remove();
                modified = true;
            }
        }
        return modified;
    }

    public static <T> Collection<T> filter(Iterable<T> iterable, Filter<T> filter, Collection<T> output) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(filter, "filter must not be null");
        Validate.notNull(output, "output must not be null");

        for (T item : iterable) {
            if (filter.accept(item)) {
                output.add(item);
            }
        }
        return output;
    }

    public static <I, O, C extends Collection<O>> C filter(Iterable<I> iterable, Class<O> type, C output) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(type, "type must not be null");
        Validate.notNull(output, "output must not be null");

        for (I item : iterable) {
            if (type.isInstance(item)) {
                output.add(type.cast(item));
            }
        }
        return output;
    }

    /**
     * <p>
     * Get the first element in a {@link List}.
     * </p>
     * 
     * @param list The List from which to get the element, not <code>null</code>.
     * @return The first element, or <code>null</code> if List was empty.
     */
    public static <T> T getFirst(List<T> list) {
        Validate.notNull(list, "list must not be null");
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

}
