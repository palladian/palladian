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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.functional.Function;

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

    /**
     * <p>
     * Indicate sorting order.
     * </p>
     */
    public enum Order {
        ASCENDING, DESCENDING
    }

    /**
     * <p>
     * Iterator, which stops after the specified limit.
     * </p>
     * 
     * @author pk
     * 
     * @param <T>
     */
    private static final class LimitIterator<T> implements Iterator<T> {

        final Iterator<T> iterator;
        final int limit;
        int counter = 0;

        LimitIterator(Iterator<T> iterator, int limit) {
            this.iterator = iterator;
            this.limit = limit;
        }

        @Override
        public boolean hasNext() {
            if (counter >= limit) {
                return false;
            }
            return iterator.hasNext();
        }

        @Override
        public T next() {
            if (counter >= limit) {
                throw new NoSuchElementException();
            }
            T temp = iterator.next();
            counter++;
            return temp;
        }

        @Override
        public void remove() {
            iterator.remove();
        }

    }

    private CollectionHelper() {
        // prevent instantiation.
    }

    /**
     * <p>
     * Sort a {@link Map} by value.
     * </p>
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values, must implement {@link Comparable}.
     * @param map The {@link Map} to sort, not <code>null</code>.
     * @param ascending {@link Order#ASCENDING} or {@link Order#DESCENDING}, not <code>null</code>.
     * @return A sorted map.
     */
    public static <K, V extends Comparable<V>> Map<K, V> sortByValue(Map<K, V> map, Order order) {
        Validate.notNull(map, "map must not be null");
        Validate.notNull(order, "order must not be null");
        List<Entry<K, V>> list = new LinkedList<Entry<K, V>>(map.entrySet());
        Collections.sort(list, new EntryValueComparator<V>(order));

        Map<K, V> result = new LinkedHashMap<K, V>();
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
     * @param <V> Type of the values, must implement {@link Comparable}.
     * @param map The {@link Map} to sort.
     * @return A sorted map, in ascending order.
     */
    public static <K, V extends Comparable<V>> Map<K, V> sortByValue(Map<K, V> map) {
        return sortByValue(map, Order.ASCENDING);
    }

    /**
     * <p>
     * Sort a {@link HashMap} by length of the key string.
     * </p>
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param map The entry set.
     * @param ascending {@link Order#ASCENDING} or {@link Order#DESCENDING}.
     * @return A sorted map.
     * @deprecated {@link Map}s are <b>not</b> meant for this use case. Prefer using a {@link List} populated with
     *             {@link Pair}s, sorted as required.
     */
    @Deprecated
    public static <V extends Comparable<V>> Map<String, V> sortByStringKeyLength(Map<String, V> map, final Order order) {

        LinkedList<Entry<String, V>> list = new LinkedList<Entry<String, V>>(map.entrySet());

        Comparator<Entry<String, V>> comparator = new Comparator<Entry<String, V>>() {
            @Override
            public int compare(Entry<String, V> o1, Entry<String, V> o2) {
                int ret = new Integer(o1.getKey().length()).compareTo(o2.getKey().length());
                return order == Order.ASCENDING ? ret : -ret;
            }
        };
        Collections.sort(list, comparator);

        Map<String, V> result = new LinkedHashMap<String, V>();
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
     * @param array The array to print, not <code>null</code>.
     */
    public static void print(Object[] array) {
        Validate.notNull(array, "array must not be null");
        print(new ArrayIterator<Object>(array));
    }

    /**
     * <p>
     * Print a human readable, line separated output of a {@link Map}.
     * </p>
     * 
     * @param map The map to print, not <code>null</code>.
     */
    public static void print(Map<?, ?> map) {
        Validate.notNull(map, "map must not be null");
        print(map.entrySet());
    }

    /**
     * <p>
     * Print a human readable, line separated output of an {@link Iterator}.
     * </p>
     * 
     * @param iterator The iterator to print, not <code>null</code>.
     */
    public static void print(Iterator<?> iterator) {
        Validate.notNull(iterator, "iterator must not be null");
        int count = 0;
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
            count++;
        }
        System.out.println("#Entries: " + count);
    }

    /**
     * <p>
     * Print a human readable, line separated output of an {@link Iterable}.
     * </p>
     * 
     * @param iterable The iterable to print, not <code>null</code>.
     */
    public static void print(Iterable<?> iterable) {
        Validate.notNull(iterable, "iterable must not be null");
        print(iterable.iterator());
    }

//    /**
//     * <p>
//     * Concatenate two String arrays.
//     * </p>
//     * 
//     * @param array1
//     * @param array2
//     * @return The concatenated String array consisting of the first, then the second array's items.
//     */
//    public static String[] concat(String[] array1, String[] array2) {
//        String[] helpArray = new String[array1.length + array2.length];
//        System.arraycopy(array1, 0, helpArray, 0, array1.length);
//        System.arraycopy(array2, 0, helpArray, array1.length, array2.length);
//        return helpArray;
//    }

    /**
     * <p>
     * Create a new {@link HashMap}. This method allows omitting the type parameter when creating the HashMap:
     * <code>Map&lt;String, Integer&gt; map = CollectionHelper.newHashMap();</code>.
     * </p>
     * 
     * @return A new {@link HashMap}.
     */
    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<K, V>();
    }

    /**
     * <p>
     * Create a new {@link TreeMap}. This method allows omitting the type parameter when creating the TreeMap:
     * <code>Map&lt;String, Integer&gt; map = CollectionHelper.newTreeMap();</code>.
     * </p>
     * 
     * @return A new {@link TreeMap}.
     */
    public static <K, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap<K, V>();
    }

    /**
     * <p>
     * Create a new {@link LinkedHashMap}. This method allows omitting the type parameter when creating the
     * LinkedHashMap: <code>Map&lt;String, Integer&gt; map = CollectionHelper.newLinkedHashMap();</code>.
     * </p>
     * 
     * @return A new {@link LinkedHashMap}.
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<K, V>();
    }

    /**
     * <p>
     * Create a new {@link ArrayList}. This method allows omitting the type parameter when creating the ArrayList:
     * <code>List&lt;String&gt; list = CollectionHelper.newArrayList();</code>.
     * </p>
     * 
     * @return A new {@link ArrayList}.
     */
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<E>();
    }

    /**
     * <p>
     * Create a new {@link ArrayList} and fill it with the contents of the given {@link Iterable}.
     * </p>
     * 
     * @param iterable The {@link Iterable} providing the content for the {@link List}.
     * @return The {@link List} with items from the {@link Iterable}.
     */
    public static <E> ArrayList<E> newArrayList(Iterable<? extends E> iterable) {
        Validate.notNull(iterable, "iterable must not be null");
        return newArrayList(iterable.iterator());
    }

    /**
     * <p>
     * Create a new {@link ArrayList} and fill it with the content of the given {@link Iterator}.
     * </p>
     * 
     * @param iterator The {@link Iterator} providing the content for the {@link List}, not <code>null</code>.
     * @return The {@link List} with items from the {@link Iterator}.
     */
    public static <E> ArrayList<E> newArrayList(Iterator<? extends E> iterator) {
        Validate.notNull(iterator, "iterator must not be null");
        ArrayList<E> list = new ArrayList<E>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    /**
     * <p>
     * Create a new {@link ArrayList} and fill it with the given elements. In contrast to
     * {@link Arrays#asList(Object...)}, which creates an immutable {@link List} (and therefore should be used for
     * constant lists), the result can be further modified and further elements can be added, removed, ....
     * </p>
     * 
     * @param elements The elements to add to the {@link ArrayList}, not <code>null</code>.
     * @return A new {@link ArrayList} containing the given elements.
     */
    public static <E> ArrayList<E> newArrayList(E... elements) {
        Validate.notNull(elements, "elements must not be null");
        return new ArrayList<E>(Arrays.asList(elements));
    }

    /**
     * <p>
     * Create a new {@link LinkedList}. This method allows omitting the type parameter when creating the LinkedList:
     * <code>List&lt;String&gt; list = CollectionHelper.newLinkedList();</code>.
     * </p>
     * 
     * @return A new {@link LinkedList}.
     */
    public static <E> LinkedList<E> newLinkedList() {
        return new LinkedList<E>();
    }

    /**
     * <p>
     * Create a new {@link HashSet}. This method allows omitting the type parameter when creating the HashSet:
     * <code>Set&lt;String&gt; set = CollectionHelper.newHashSet();</code>.
     * </p>
     * 
     * @return A new {@link HashSet}.
     */
    public static <E> HashSet<E> newHashSet() {
        return new HashSet<E>();
    }

    /**
     * <p>
     * Create a new {@link HashSet} and fill it with the given elements.
     * </p>
     * 
     * @param elements The elements to add to the {@link HashSet}, not <code>null</code>.
     * @return A new {@link HashSet} containing the given elements.
     */
    public static <E> HashSet<E> newHashSet(E... elements) {
        Validate.notNull(elements, "elements must not be null");
        return new HashSet<E>(Arrays.asList(elements));
    }

    /**
     * <p>
     * Create a new {@link HashSet} and fill it with the content of the given {@link Iterable}.
     * </p>
     * 
     * @param iterable The {@link Iterable} providing the content for the {@link Set}, not <code>null</code>.
     * @return The {@link Set} with items from the {@link Iterator}.
     */
    public static <E> HashSet<E> newHashSet(Iterable<? extends E> elements) {
        Validate.notNull(elements, "elements must not be null");
        return newHashSet(elements.iterator());
    }

    /**
     * <p>
     * Create a new {@link HashSet} and fill it with the content of the given {@link Iterator}.
     * </p>
     * 
     * @param iterator The {@link Iterator} providing the content for the {@link Set}, not <code>null</code>.
     * @return The {@link Set} with items from the {@link Iterator}.
     */
    public static <E> HashSet<E> newHashSet(Iterator<? extends E> elements) {
        Validate.notNull(elements, "elements must not be null");
        HashSet<E> set = newHashSet();
        while (elements.hasNext()) {
            set.add(elements.next());
        }
        return set;
    }

    /**
     * <p>
     * Create a new {@link TreeSet}. This method allows omitting the type parameter when creating the TreeSet:
     * <code>Set&lt;String&gt; set = CollectionHelper.newTreeSet();</code>.
     * </p>
     * 
     * @return A new {@link TreeSet}.
     */
    public static <E> TreeSet<E> newTreeSet() {
        return new TreeSet<E>();
    }

    /**
     * <p>
     * Create a new {@link LinkedHashSet}. This method allows omitting the type parameter when creating the
     * LinkedHashSet: <code>Set&lt;String&gt; set = CollectionHelper.newLinkedHashSet();</code>.
     * </p>
     * 
     * @return A new {@link LinkedHashSet}.
     */
    public static <E> LinkedHashSet<E> newLinkedHashSet() {
        return new LinkedHashSet<E>();
    }

    /**
     * <p>
     * Remove all <code>null</code> elements in the supplied {@link Iterable}.
     * </p>
     * 
     * @param collection The iterable from which to remove <code>null</code> elements.
     * @return The number of items which were removed, <code>0</code> in case no items were removed.
     */
    public static <T> int removeNulls(Iterable<T> iterable) {
        Validate.notNull(iterable, "iterable must not be null");
        return remove(iterable, Filters.NOT_NULL);
    }

    /**
     * <p>
     * Apply a {@link Filter} to an {@link Iterable} and remove non-matching items; after applying this method, the
     * Iterable only contains the items which matched the filter.
     * </p>
     * 
     * @param iterable The Iterable to filter, not <code>null</code>.
     * @param filter The Filter to apply, not <code>null</code>.
     * @return The number of items which were removed, <code>0</code> in case no items were removed.
     */
    public static <T> int remove(Iterable<T> iterable, Filter<? super T> filter) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(filter, "filter must not be null");

        int removed = 0;
        Iterator<T> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            T item = iterator.next();
            if (!filter.accept(item)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    /**
     * <p>
     * Apply a {@link Filter} to an {@link Iterable} and return the filtered result as new {@link Collection}.
     * </p>
     * 
     * @param iterable The Iterable to filter, not <code>null</code>.
     * @param filter The filter to apply, not <code>null</code>.
     * @param output The output {@link Collection} in which to put the result. Usually an {@link ArrayList} or
     *            {@link HashSet}, not <code>null</code>.
     * @return The supplied output Collection with the items that passed the filter.
     */
    public static <T, C extends Collection<T>> C filter(Iterable<T> iterable, Filter<? super T> filter, C output) {
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

    /**
     * <p>
     * Apply a {@link Filter} to an {@link Iterable} and return the filtered result as new {@link List}.
     * </p>
     * 
     * @param list The Iterable to filter, not <code>null</code>.
     * @param filter The filter to apply, not <code>null</code>.
     * @return A List with the items that passed the filter.
     * @see #filter(Iterable, Filter, Collection)
     */
    public static <T> List<T> filterList(Iterable<T> iterable, Filter<? super T> filter) {
        return filter(iterable, filter, CollectionHelper.<T> newArrayList());
    }

    /**
     * <p>
     * Apply a {@link Filter} to an {@link Iterable} and return the filtered result as new {@link Set}.
     * </p>
     * 
     * @param list The Iterable to filter, not <code>null</code>.
     * @param filter The filter to apply, not <code>null</code>.
     * @return A Set with the items that passed the filter.
     * @see #filter(Iterable, Filter, Collection)
     */
    public static <T> Set<T> filterSet(Iterable<T> iterable, Filter<? super T> filter) {
        return filter(iterable, filter, CollectionHelper.<T> newHashSet());
    }

    /**
     * <p>
     * Apply a type filter to an {@link Iterable} and return the filtered result as new {@link Collection}. An example
     * scenario for this method might be a Collection of {@link Number}s, from which you only want to obtain
     * {@link Double} values.
     * </p>
     * 
     * @param iterable The Iterable to filter, not <code>null</code>.
     * @param type The type which should be filtered, not <code>null</code>.
     * @param output The output {@link Collection} in which to put the result. Usually an {@link ArrayList} or
     *            {@link HashSet}, not <code>null</code>.
     * @return The supplied output Collection with the items that passed the type filter.
     */
    public static <O, C extends Collection<O>> C filter(Iterable<?> iterable, Class<O> type, C output) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(type, "type must not be null");
        Validate.notNull(output, "output must not be null");

        for (Object item : iterable) {
            if (type.isInstance(item)) {
                output.add(type.cast(item));
            }
        }
        return output;
    }

    /**
     * <p>
     * Apply a {@link Filter} to an {@link Iterator}.
     * </p>
     * 
     * @param iterator The iterator to filter, not <code>null</code>.
     * @param filter The filter to apply, not <code>null</code>.
     * @return A new iterator, where the given filter is applied, thus eliminating the entries in the iterator, which
     *         are not accepted by the filter.
     */
    public static <T> Iterator<T> filter(Iterator<T> iterator, Filter<? super T> filter) {
        Validate.notNull(iterator, "iterator must not be null");
        Validate.notNull(filter, "filter must not be null");
        return new FilterIterator<T>(iterator, filter);
    }

    /**
     * <p>
     * Apply a {@link Filter} to an {@link Iterable}.
     * </p>
     * 
     * @param iterable The iterable to filter, not <code>null</code>.
     * @param filter The filter to apply, not <code>null</code>.
     * @return A new iterator, where the given filter is applied, thus eliminating the entries in the iterator, which
     *         are not accepted by the filter.
     */
    public static <T> Iterable<T> filter(final Iterable<T> iterable, final Filter<? super T> filter) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(filter, "filter must not be null");
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return filter(iterable.iterator(), filter);
            }
        };
    }

    /**
     * <p>
     * Get the first element in an {@link Iterable}.
     * </p>
     * 
     * @param list The Iterable from which to get the element, not <code>null</code>.
     * @return The first element, or <code>null</code> if the iterable was empty.
     */
    public static <T> T getFirst(Iterable<T> iterable) {
        Validate.notNull(iterable, "iterable must not be null");
        Iterator<T> iterator = iterable.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    /**
     * <p>
     * Get the first X elements in an {@link Iterable}.
     * </p>
     * 
     * @param list The Iterable from which to get the element, not <code>null</code>.
     * @param num The number of elements to retrieve. If the collection has less entries it will return only those.
     * @return The first X elements, or an empty list if the iterable was empty.
     * @deprecated Use {@link #limit(Iterable, int)} instead.
     */
    @Deprecated
    public static <T> List<T> getFirst(Iterable<T> iterable, int num) {
//        List<T> result = CollectionHelper.newArrayList();
//        for (T t : iterable) {
//            result.add(t);
//            if (result.size() == num) {
//                break;
//            }
//        }
//        return result;
        return newArrayList(limit(iterable, num));
    }

    /**
     * <p>
     * Get a sublist of elements of a {@link List}.
     * </p>
     * 
     * @param list The list from which to get the element, not <code>null</code>.
     * @param offset The number of elements to skip.
     * @param num The number of elements to retrieve. If the collection has less entries it will return only those.
     * @return The sublist.
     */
    public static <T> List<T> getSublist(List<T> list, int offset, int num) {
        Validate.notNull(list, "list must not be null");
        Validate.isTrue(offset >= 0, "offset must be greater/equal zero");
        Validate.isTrue(num >= 0, "num must be greater/equal zero");
        int o = Math.min(list.size(), offset);
        int n = Math.min(num, list.size() - o);
        return list.subList(o, o + n);
    }

    /**
     * <p>
     * Get the last element in a {@link List}.
     * </p>
     * 
     * @param list The List from which to get the element, not <code>null</code>.
     * @return The last element, or <code>null</code> if the list was empty.
     */
    public static <T> T getLast(List<T> list) {
        Validate.notNull(list, "list must not be null");
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    /**
     * <p>
     * SQL like group by functionality. The returned {@link MultiMap} contains the groups, a specified {@link Function}
     * supplies the values for grouping.
     * </p>
     * 
     * @param iterable The Iterable to group, not <code>null</code>.
     * @param function The Function which returns the value which is used for grouping, not <code>null</code>.
     * @return A MultiMap representing the groups.
     */
    public static <I, V> MultiMap<V, I> groupBy(Iterable<I> iterable, Function<? super I, V> function) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(function, "function must not be null");

        MultiMap<V, I> result = DefaultMultiMap.createWithList();
        for (I item : iterable) {
            result.add(function.compute(item), item);
        }
        return result;
    }

    /**
     * <p>
     * Convert contents of {@link Iterable}s to a different type. For example if, you have a {@link List} of Numbers and
     * want to convert them to Strings, supply a {@link Function} which applies the <code>toString()</code> method to
     * the Numbers (a predefined Function for this specific use case is available as {@link Function#TO_STRING_FUNCTION}
     * ).
     * </p>
     * 
     * <pre>
     * // list with numbers
     * List&lt;Integer&gt; numbers = Arrays.asList(0, 1, 1, 2, 3, 5);
     * // convert them to strings using the specified Function
     * List&lt;String&gt; strings = convert(numbers, new Function&lt;Number, String&gt;() {
     *     &#064;Override
     *     public String compute(Number input) {
     *         return input.toString();
     *     }
     * }, new ArrayList&lt;String&gt;());
     * </pre>
     * 
     * @param iterable The Iterable supplying the data to be converted, not <code>null</code>.
     * @param function The Function which converts the values in the iterable, not <code>null</code>.
     * @param output The output {@link Collection} in which to put the result. Usually an {@link ArrayList} or
     *            {@link HashSet}, not <code>null</code>.
     * @return The supplied output Collection with the converted items.
     * @see #convertList(Iterable, Function)
     * @see #convertSet(Iterable, Function)
     */
    public static <I, O, C extends Collection<O>> C convert(Iterable<I> iterable, Function<? super I, O> function,
            C output) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(function, "function must not be null");
        Validate.notNull(output, "output must not be null");

        for (I item : iterable) {
            output.add(function.compute(item));
        }
        return output;
    }

    /**
     * <p>
     * Convert contents of {@link Iterable}s to a different type and put them into a {@link Set}. For example if, you
     * have a {@link List} of Numbers and want to convert them to Strings, supply a {@link Function} which applies the
     * <code>toString()</code> method to the Numbers (a predefined Function for this specific use case is available as
     * {@link Function#TO_STRING_FUNCTION}).
     * </p>
     * 
     * @param iterable The Iterable supplying the data to be converted, not <code>null</code>.
     * @param function The Function which converts the values in the iterable, not <code>null</code>.
     * @return A {@link Set} with the field elements from the given objects.
     */
    public static <I, O> Set<O> convertSet(Iterable<I> iterable, Function<? super I, O> function) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(function, "function must not be null");
        return convert(iterable, function, new HashSet<O>());
    }

    /**
     * <p>
     * Convert contents of {@link Iterable}s to a different type and put them into a {@link List}. For example if, you
     * have a {@link List} of Numbers and want to convert them to Strings, supply a {@link Function} which applies the
     * <code>toString()</code> method to the Numbers (a predefined Function for this specific use case is available as
     * {@link Function#TO_STRING_FUNCTION}).
     * </p>
     * 
     * @param iterable The Iterable supplying the data to be converted, not <code>null</code>.
     * @param function The Function which converts the values in the iterable, not <code>null</code>.
     * @return A {@link List} with the field elements from the given objects.
     */
    public static <I, O> List<O> convertList(Iterable<I> iterable, Function<? super I, O> function) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(function, "function must not be null");
        return convert(iterable, function, new ArrayList<O>());
    }

    /**
     * <p>
     * Create a wrapper for a given {@link Iterator} which converts the iterator's items using a provided
     * {@link Function}.
     * </p>
     * 
     * @param iterator The iterator to wrap, not <code>null</code>.
     * @param function The {@link Function} which performs the conversion, not <code>null</code>.
     * @return An iterator wrapping the given iterator.
     */
    public static <I, O> Iterator<O> convert(final Iterator<I> iterator, final Function<? super I, O> function) {
        Validate.notNull(iterator, "iterator must not be null");
        Validate.notNull(function, "function must not be null");
        return new Iterator<O>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public O next() {
                return function.compute(iterator.next());
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    /**
     * <p>
     * Create a wrapper for a given {@link Iterable} which converts the iterable's items using a provided
     * {@link Function}.
     * </p>
     * 
     * @param iterator The iterator to wrap, not <code>null</code>.
     * @param function The {@link Function} which performs the conversion, not <code>null</code>.
     * @return An iterable wrapping the given iterable.
     */
    public static <I, O> Iterable<O> convert(final Iterable<I> iterable, final Function<? super I, O> function) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(function, "function must not be null");
        return new Iterable<O>() {
            @Override
            public Iterator<O> iterator() {
                return convert(iterable.iterator(), function);
            }
        };
    }

    /**
     * <p>
     * Join elements of a collection in a readable form.
     * </p>
     * 
     * @param entries The entries that should be joined.
     * @return The joined string.
     */
    public static String joinReadable(Collection<?> entries) {
        return joinReadable(entries, entries.size());
    }

    public static String joinReadable(Collection<?> entries, int numEntries) {
        String joinedText = StringUtils.join(entries.toArray(), ", ", 0, Math.min(entries.size(), numEntries));
        int lastIndex = joinedText.lastIndexOf(",");
        if (lastIndex > -1) {
            String joinedTextNew = joinedText.substring(0, lastIndex);
            if (entries.size() > 2) {
                joinedTextNew += ",";
            }
            joinedTextNew += " and" + joinedText.substring(lastIndex + 1);
            joinedText = joinedTextNew;
        }
        return joinedText;
    }

    /**
     * <p>
     * Get a value from a {@link Map} by trying multiple keys.
     * </p>
     * 
     * @param map The map, not <code>null</code>.
     * @param keys The keys.
     * @return The value if any of the keys matches, or <code>null</code>.
     */
    public static <K, V> V getTrying(Map<K, V> map, K... keys) {
        Validate.notNull(map, "map must not be null");
        Validate.notNull(keys, "keys must not be null");
        for (K key : keys) {
            V value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * <p>
     * Get the first non-null value from the given items.
     * </p>
     * 
     * @param items The items.
     * @return The first non-null item from the given, or <code>null</code> in case the only <code>null</code> or no
     *         values were given.
     */
    public static <T> T coalesce(T... items) {
        for (T item : items) {
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    /**
     * <p>
     * Limit the specified {@link Iterable} to the specified size, i.e. effectively get the first specified elements,
     * then stop.
     * </p>
     * 
     * @param iterable The iterable, not <code>null</code>.
     * @param limit The number of elements which can be retrieved from the given iterable, greater/equal zero.
     * @return An iterable which limits to the specified number of items, in case it contains more.
     */
    public static <T> Iterable<T> limit(final Iterable<T> iterable, final int limit) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.isTrue(limit >= 0, "limit must be greater/equal zero");
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return limit(iterable.iterator(), limit);
            }
        };
    }

    /**
     * <p>
     * Limit the specified {@link Iterator} to the specified size, i.e. effectively get the first specified elements,
     * then stop.
     * </p>
     * 
     * @param iterator The iterator, not <code>null</code>.
     * @param limit The number of elements which can be retrieved from the given iterator, greater/equal zero.
     * @return An iterator which limits to the specified number of items, in case it contains more.
     */
    public static <T> Iterator<T> limit(Iterator<T> iterator, int limit) {
        Validate.notNull(iterator, "iterator must not be null");
        Validate.isTrue(limit >= 0, "limit must be greater/equal zero");
        return new LimitIterator<T>(iterator, limit);
    }

    /**
     * <p>
     * Get a set with distinct values from all given collections.
     * </p>
     * 
     * @param collections The collections, not <code>null</code>.
     * @return A {@link Set} with distinct values from the given collections.
     */
    public static <T> Set<T> distinct(Collection<T>... collections) {
        Validate.notNull(collections, "collections must not be null");
        Set<T> distinct = newHashSet();
        for (Collection<T> collection : collections) {
            distinct.addAll(collection);
        }
        return distinct;
    }

    /**
     * <p>
     * Get a set with the intersection from two given sets (this method is faster than the common idiom
     * <code>Set intersection = new HashSet(setA); intersection.retainAll(setB);</code>).
     * </p>
     * 
     * @param setA The first set, not <code>null</code>.
     * @param setB The second set, not <code>null</code>.
     * @return A new set which contains only elements occurring in both given sets.
     */
    public static <T> Set<T> intersect(Set<? extends T> setA, Set<? extends T> setB) {
        Validate.notNull(setA, "setA must not be null");
        Validate.notNull(setB, "setB must not be null");
        // the most common variant to calculate an intersection is something like this:
        // Set intersection = new HashSet(setA); intersection.retainAll(setB);
        // however, if both sets have considerably different sizes, this can be optimized,
        // by iterating over the smaller set and checking whether the current element
        // occurs in the larger set:
        Set<? extends T> smallerSet = setA;
        Set<? extends T> largerSet = setB;
        if (smallerSet.size() > largerSet.size()) { // swap smaller/larger set if necessary
            smallerSet = setB;
            largerSet = setA;
        }
        Set<T> intersection = new HashSet<T>();
        for (T element : smallerSet) {
            if (largerSet.contains(element)) {
                intersection.add(element);
            }
        }
        return intersection;
    }

    /**
     * <p>
     * Shuffle the content of the given array.
     * </p>
     * 
     * @param array The array to shuffle, not <code>null</code>.
     */
    public static void shuffle(Object[] array) {
        Validate.notNull(array, "array must not be null");
        // http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
        Random rnd = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            Object item = array[index];
            array[index] = array[i];
            array[i] = item;
        }
    }

    /**
     * <p>
     * Check, if the provided {@link Filter} accepts all items from given {@link Iterable}.
     * </p>
     * 
     * @param iterable The iterable, not <code>null</code>.
     * @param filter The filter, not <code>null</code>.
     * @return <code>true</code> in case the filter accepted all items from the iterable, <code>false</code> otherwise.
     */
    public static <T> boolean acceptAll(Iterable<T> iterable, Filter<? super T> filter) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(filter, "filter must not be null");
        for (T item : iterable) {
            if (!filter.accept(item)) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * Count the number of elements in an {@link Iterator}.
     * </p>
     * 
     * @param iterator The iterator, not <code>null</code>.
     * @return The number of elements.
     */
    public static int count(Iterator<?> iterator) {
        Validate.notNull(iterator, "iterator must not be null");
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }
    
    /**
     * <p>
     * Make a given {@link Iterator} read-only. Invoking {@link Iterator#remove()} will trigger an
     * {@link UnsupportedOperationException}.
     * 
     * @param iterator The iterator, not <code>null</code>.
     * @return An iterator wrapping the given one, whithout the possibility for modifications.
     */
    public static <T> Iterator<T> unmodifiableIterator(final Iterator<T> iterator) {
        Validate.notNull(iterator, "iterator must not be null");
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Modifications are not allowed.");
            }
        };
    }

}
