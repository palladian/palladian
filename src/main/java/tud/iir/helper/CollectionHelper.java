package tud.iir.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class adds some methods that make it easier to handle collections.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class CollectionHelper {

    public static boolean ASCENDING = true;
    public static boolean DESCENDING = false;

    /**
     * Sort a hashmap by value.
     * 
     * @param <S> The key.
     * @param <T> The value.
     * @param entrySet The entry set.
     * @return The sorted map.
     */
    public static <S, T> LinkedHashMap<S, T> sortByValue(Set<Map.Entry<S, T>> entrySet) {
        return CollectionHelper.sortByValue(entrySet, true);
    }

    /**
     * Sort a hashmap by value.
     * 
     * @param <S> The key.
     * @param <T> The value.
     * @param entrySet The entry set.
     * @param ascending Whether to sort ascending or descending.
     * @return The sorted map.
     */
    public static <S, T> LinkedHashMap<S, T> sortByValue(Set<Map.Entry<S, T>> entrySet, boolean ascending) {
        LinkedList<Map.Entry<S, T>> list = new LinkedList<Map.Entry<S, T>>(entrySet);

        Comparator<Map.Entry<S, T>> comparator;
        if (ascending) {
            comparator = new Comparator<Map.Entry<S, T>>() {
                @SuppressWarnings("unchecked")
                public int compare(Map.Entry<S, T> o1, Map.Entry<S, T> o2) {
                    return ((Comparable<T>) ((o1)).getValue()).compareTo(((o2)).getValue());
                }
            };
        } else {
            comparator = new Comparator<Map.Entry<S, T>>() {
                @SuppressWarnings("unchecked")
                public int compare(Map.Entry<S, T> o1, Map.Entry<S, T> o2) {
                    return ((Comparable<T>) ((o2)).getValue()).compareTo(((o1)).getValue());
                }
            };
        }

        Collections.sort(list, comparator);

        LinkedHashMap<S, T> result = new LinkedHashMap<S, T>();
        for (Iterator<Map.Entry<S, T>> it = list.iterator(); it.hasNext();) {
            Map.Entry<S, T> entry = it.next();
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Get a key given a value (1 to 1 HashMaps)
     * 
     * @param value The value.
     * @return The key that matches the value.
     */
    public static <S> Object getKeyByValue(Map<S, S> map, Object value) {
        for (Entry<S, S> mapEntry : map.entrySet()) {
            if (mapEntry.getValue().equals(value))
                return mapEntry.getKey();
        }

        return null;
    }

    public static <T> ArrayList<T> reverse(ArrayList<T> list) {
        ArrayList<T> reversedList = new ArrayList<T>();

        for (int i = list.size() - 1; i >= 0; --i) {
            reversedList.add(list.get(i));
        }

        return reversedList;
    }

    public static String getPrint(Object[] array) {
        Set<Object> set = new HashSet<Object>();
        for (Object o : array) {
            set.add(o);
        }
        return getPrint(set);
    }

    // public static String getPrint(Set set) {
    // StringBuilder s = new StringBuilder();
    //
    // for (Object entry : set) {
    // s.append(entry).append("\n");
    // }
    // s.append("#Entries: ").append(set.size()).append("\n");
    //
    // return s.toString();
    // }

    public static void print(Object[] array) {
        for (Object o : array) {
            System.out.println(o);
        }
        System.out.println("#Entries: " + array.length);
    }

    // public static void print(Set set) {
    // System.out.println(getPrint(set));
    // }
    // public static void print(List list) {
    // Iterator listIterator = list.iterator();
    // while (listIterator.hasNext()) {
    // Object entry = listIterator.next();
    // System.out.println(entry);
    // }
    // System.out.println("#Entries: "+list.size());
    // }
    @SuppressWarnings("unchecked")
    public static void print(Map map) {
        Iterator<Map.Entry> mapIterator = map.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry entry = mapIterator.next();
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        System.out.println("#Entries: " + map.entrySet().size());
    }

    /**
     * Check whether a string array contains a string.
     * 
     * @param array The string array.
     * @param entry The string entry that is checked against the array.
     * @return True, if the entry is contained in the array, false otherwise.
     */
    public static boolean contains(String[] array, String entry) {
        for (String s : array) {
            if (s.equals(entry))
                return true;
        }
        return false;
    }

    public static String getPrint(Collection<?> collection) {
        StringBuilder s = new StringBuilder();

        for (Object entry : collection) {
            s.append(entry).append("\n");
        }
        s.append("#Entries: ").append(collection.size()).append("\n");

        return s.toString();
    }

    public static void print(Collection<?> collection) {
        System.out.println(getPrint(collection));
    }

    public static HashSet<String> toHashSet(String[] array) {
        HashSet<String> set = new HashSet<String>();
        for (String s : array) {
            if (s.length() > 0) {
                set.add(s);
            }
        }
        return set;
    }

    // ///////////////////////////////////////////////////////
    // (un)boxing for arrays from primitive to Object types
    // TODO can we make this generic somehow?
    // ///////////////////////////////////////////////////////

    public static Integer[] toIntegerArray(int[] intArray) {
        Integer[] integerArray = new Integer[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            integerArray[i] = intArray[i];
        }
        return integerArray;
    }

    public static int[] toIntArray(Integer[] integerArray) {
        int[] intArray = new int[integerArray.length];
        for (int i = 0; i < integerArray.length; i++) {
            intArray[i] = integerArray[i];
        }
        return intArray;
    }

}