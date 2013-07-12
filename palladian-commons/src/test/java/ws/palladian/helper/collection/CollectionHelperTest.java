package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

/**
 * 
 * @author Philipp Katz
 */
public class CollectionHelperTest {

    @SuppressWarnings("deprecation")
    @Test
    public void testSortyMapByValue() {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        map.put(1, 5);
        map.put(3, 3);
        map.put(2, 4);
        map.put(5, 1);
        map.put(4, 2);
        LinkedHashMap<Integer, Integer> mapSortedByValue = CollectionHelper.sortByValue(map);
        Iterator<Entry<Integer, Integer>> iterator = mapSortedByValue.entrySet().iterator();
        assertEquals((Integer)1, iterator.next().getValue());
        assertEquals((Integer)2, iterator.next().getValue());
        assertEquals((Integer)3, iterator.next().getValue());
        assertEquals((Integer)2, iterator.next().getKey());
        assertEquals((Integer)1, iterator.next().getKey());

        LinkedHashMap<Integer, Integer> mapSortedByValueDescending = CollectionHelper.sortByValue(map,
                CollectionHelper.DESCENDING);
        iterator = mapSortedByValueDescending.entrySet().iterator();
        assertEquals((Integer)5, iterator.next().getValue());
        assertEquals((Integer)4, iterator.next().getValue());
        assertEquals((Integer)3, iterator.next().getValue());
        assertEquals((Integer)4, iterator.next().getKey());
        assertEquals((Integer)5, iterator.next().getKey());

    }

    @Test
    public void testFieldFilter() {

        // strings
        Collection<NameObject> set = new HashSet<NameObject>();
        set.add(new NameObject("A"));
        set.add(new NameObject("B"));

        Collection<String> names = CollectionHelper.getFields(set, new FieldFilter<NameObject, String>() {
            @Override
            public String getField(NameObject item) {
                return item.getName();
            }
        });
        // CollectionHelper.print(names);
        assertTrue(names.contains("A"));
        assertTrue(names.contains("B"));
        assertEquals(2, names.size());

        // integers
        set = new HashSet<NameObject>();
        set.add(new NameObject(1));
        set.add(new NameObject(2));

        Collection<Integer> ages = CollectionHelper.getFields(set, new FieldFilter<NameObject, Integer>() {
            @Override
            public Integer getField(NameObject item) {
                return item.getAge();
            }
        });
        // CollectionHelper.print(ages);
        assertTrue(ages.contains(1));
        assertTrue(ages.contains(2));
        assertEquals(2, names.size());

    }

    @Test
    public void removeNulls() {
        List<Integer> list = new ArrayList<Integer>(Arrays.asList(null, 1, 2, 3, 4, null));
        boolean removed = CollectionHelper.removeNulls(list);
        assertTrue(removed);
        assertEquals(4, list.size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void sortByStringKeyLength() {

        Map<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("A", "A");
        hashMap.put("BB", "B");
        hashMap.put("CCC", "C");

        // CollectionHelper.print(hashMap);
        // CollectionHelper.print(CollectionHelper.sortByStringKeyLength(hashMap, false));

        assertEquals("CCC", CollectionHelper.sortByStringKeyLength(hashMap, false).entrySet().iterator().next()
                .getKey());
        assertEquals("A", CollectionHelper.sortByStringKeyLength(hashMap, true).entrySet().iterator().next().getKey());

    }

    @Test
    public void testFilter() {
        List<String> items = new ArrayList<String>(Arrays.asList("a", "b", "c", "d", "a", "b", "c"));
        boolean filtered = CollectionHelper.filter(items, new Filter<String>() {
            @Override
            public boolean accept(String item) {
                return item.equals("a") || item.equals("b");
            }
        });
        assertTrue(filtered);
        assertEquals(4, items.size());
    }

    @Test
    public void testGroupBy() {
        List<String> items = Arrays.asList("one", "two", "three", "four", "five", "six");
        MultiMap<Integer, String> groupedResult = CollectionHelper.groupBy(items, new Function<String, Integer>() {
            @Override
            public Integer compute(String input) {
                return input.length();
            }
        });
        assertEquals(3, groupedResult.size());
        assertTrue(groupedResult.get(3).containsAll(Arrays.asList("one", "two", "six")));
        assertTrue(groupedResult.get(4).containsAll(Arrays.asList("four", "five")));
        assertTrue(groupedResult.get(5).containsAll(Arrays.asList("three")));
    }

    private class NameObject {
        private String name;
        private int age;

        public NameObject(String name) {
            super();
            this.name = name;
        }

        public NameObject(int age) {
            super();
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

    }
}
