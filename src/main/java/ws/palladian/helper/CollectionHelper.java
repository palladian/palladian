package ws.palladian.helper;

import java.util.ArrayList;
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
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections15.Bag;

/**
 * This class adds some methods that make it easier to handle collections.
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
     * Sort a {@link Map} by value.
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param map The {@link Map} to sort.
     * @param ascending {@link CollectionHelper#ASCENDING} or {@link CollectionHelper#DESCENDING}.
     * @return A sorted map.
     */
    public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(Map<K, V> map, boolean ascending) {
        return sortByValue(map.entrySet(), ascending);
    }

    /**
     * Sort a {@link Map} by value.
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param map The {@link Map} to sort.
     * @return A sorted map, in ascending order.
     */
    public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(Map<K, V> map) {
        return sortByValue(map.entrySet());
    }

    /**
     * Sort a {@link HashMap} by value.
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param entrySet The entry set.
     * @return A sorted map, in ascending order.
     */
    public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(Set<Map.Entry<K, V>> entrySet) {
        return CollectionHelper.sortByValue(entrySet, CollectionHelper.ASCENDING);
    }

    /**
     * Sort a {@link HashMap} by value.
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param entrySet The entry set.
     * @param ascending {@link CollectionHelper#ASCENDING} or {@link CollectionHelper#DESCENDING}.
     * @return A sorted map.
     */
    public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(Set<Map.Entry<K, V>> entrySet,
            boolean ascending) {
        LinkedList<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(entrySet);

        Comparator<Map.Entry<K, V>> comparator;
        if (ascending) {
            comparator = new Comparator<Map.Entry<K, V>>() {
                @Override
                public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            };
        } else {
            comparator = new Comparator<Map.Entry<K, V>>() {
                @Override
                public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                    return o2.getValue().compareTo(o1.getValue());
                }
            };
        }

        Collections.sort(list, comparator);

        LinkedHashMap<K, V> result = new LinkedHashMap<K, V>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Get a key given a value (1 to 1 {@link HashMap}s)
     * 
     * @param value The value.
     * @return The key that matches the value.
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
     * Reverse the order in an {@link ArrayList}.
     * 
     * @param <T>
     * @param list
     * @return
     */
    public static <T> ArrayList<T> reverse(ArrayList<T> list) {
        ArrayList<T> reversedList = new ArrayList<T>();

        for (int i = list.size() - 1; i >= 0; --i) {
            reversedList.add(list.get(i));
        }

        return reversedList;
    }

    /**
     * Get a human readable, line separated output of an Array.
     * 
     * @param array
     * @return
     */
    public static String getPrint(Object[] array) {
        Set<Object> set = new HashSet<Object>();
        for (Object o : array) {
            set.add(o);
        }
        return getPrint(set);
    }

    /**
     * Print a human readable, line separated output of an Array.
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
     * Print a human readable, line separated output of a {@link Map}.
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
     * Check whether an array contains an entry.
     * 
     * @param array The array.
     * @param entry The entry that is checked against the array.
     * @return True, if the entry is contained in the array, false otherwise.
     */
    public static <T> boolean contains(T[] array, T entry) {
        for (T s : array) {
            if (s.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a human readable, line separated output of a {@link Collection}.
     * 
     * @param collection
     * @return
     */
    public static String getPrint(Collection<?> collection) {
        StringBuilder s = new StringBuilder();

        for (Object entry : collection) {
            s.append(entry).append("\n");
        }
        s.append("#Entries: ").append(collection.size()).append("\n");

        return s.toString();
    }

    /**
     * Print a human readable, line separated output of a {@link Collection}.
     * 
     * @param collection
     */
    public static void print(Collection<?> collection) {
        System.out.println(getPrint(collection));
    }

    /**
     * Convert a string array to a Set, skip empty strings.
     * 
     * @param array
     * @return
     */
    public static HashSet<String> toHashSet(String[] array) {
        HashSet<String> set = new HashSet<String>();
        for (String s : array) {
            if (s.length() > 0) {
                set.add(s);
            }
        }
        return set;
    }

    /**
     * Converts an array of primitive int to an Integer array.
     * 
     * @param intArray
     * @return
     */
    public static Integer[] toIntegerArray(int[] intArray) {
        Integer[] integerArray = new Integer[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            integerArray[i] = intArray[i];
        }
        return integerArray;
    }

    /**
     * Converts an array of Integer values to a primitive int array.
     * 
     * @param integerArray
     * @return
     */
    public static int[] toIntArray(Integer[] integerArray) {
        int[] intArray = new int[integerArray.length];
        for (int i = 0; i < integerArray.length; i++) {
            intArray[i] = integerArray[i];
        }
        return intArray;
    }

    /**
     * Converts a {@link List} to a {@link TreeSet}.
     * 
     * @param <T>
     * @param list
     * @return
     */
    public static <T> TreeSet<T> toTreeSet(List<T> list) {
        TreeSet<T> set = new TreeSet<T>();
        for (T item : list) {
            set.add(item);
        }
        return set;
    }

    /**
     * Converts a {@link Bag} to a {@link Map}, items values from the Bag as keys, and their counts as values.
     * 
     * @param <K>
     * @param bag
     * @return
     */
    public static <K> Map<K, Integer> toMap(Bag<K> bag) {
        Map<K, Integer> map = new HashMap<K, Integer>();
        Set<K> uniqueSet = bag.uniqueSet();
        for (K key : uniqueSet) {
            map.put(key, bag.getCount(key));
        }
        return map;
    }
    

    /**
     * Converts a List to a Map. The values of the Map represent the number of duplicate entries.
     * 
     * @param <K> Type of items.
     * @param values List with potentially duplicate items.
     * @return Map with items as keys, duplicate count as values.
     */
    public static <K> Map<K, Integer> toMap(List<K> values) {
        Map<K, Integer> map = new LinkedHashMap<K, Integer>();

        for (int i = 0; i < values.size(); i++) {
            if (!map.keySet().contains(values.get(i))) {
                map.put(values.get(i), 1);
            } else {
                map.put(values.get(i), map.get(values.get(i)) + 1);
            }
        }
        return map;
    }

}