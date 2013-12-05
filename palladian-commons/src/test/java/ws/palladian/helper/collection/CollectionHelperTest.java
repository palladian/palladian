package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper.Order;

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

        LinkedHashMap<Integer, Integer> mapSortedByValueDescending = CollectionHelper
                .sortByValue(map, Order.DESCENDING);
        iterator = mapSortedByValueDescending.entrySet().iterator();
        assertEquals((Integer)5, iterator.next().getValue());
        assertEquals((Integer)4, iterator.next().getValue());
        assertEquals((Integer)3, iterator.next().getValue());
        assertEquals((Integer)4, iterator.next().getKey());
        assertEquals((Integer)5, iterator.next().getKey());

    }

    @Test
    public void testRoundRobinList() {
        RoundRobinList<String> rrl = new RoundRobinList<String>();
        rrl.add("a");
        rrl.add("b");
        rrl.add("c");

        assertEquals("a", rrl.getNextItem());
        assertEquals("b", rrl.getNextItem());
        assertEquals("c", rrl.getNextItem());
        assertEquals("a", rrl.getNextItem());
        assertEquals("b", rrl.getNextItem());
        assertEquals(true, rrl.remove("a"));
        assertEquals("c", rrl.getNextItem());
    }

    @Test
    public void testJoinReadable() {
        assertEquals("a", CollectionHelper.joinReadable(Arrays.asList("a")));
        assertEquals("a and b", CollectionHelper.joinReadable(Arrays.asList("a", "b")));
        assertEquals("a, b, and c", CollectionHelper.joinReadable(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testFunction() {

        // strings
        Collection<NameObject> set = new HashSet<NameObject>();
        set.add(new NameObject("A"));
        set.add(new NameObject("B"));

        Collection<String> names = CollectionHelper.convertSet(set, new Function<NameObject, String>() {
            @Override
            public String compute(NameObject item) {
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

        Collection<Integer> ages = CollectionHelper.convertSet(set, new Function<NameObject, Integer>() {
            @Override
            public Integer compute(NameObject item) {
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

        assertEquals("CCC", CollectionHelper.sortByStringKeyLength(hashMap, Order.DESCENDING).entrySet().iterator()
                .next().getKey());
        assertEquals("A", CollectionHelper.sortByStringKeyLength(hashMap, Order.ASCENDING).entrySet().iterator().next()
                .getKey());

    }

    @Test
    public void testGetFirst() {
        List<String> items = new ArrayList<String>(Arrays.asList("a", "b", "c"));

        assertEquals("a", CollectionHelper.getFirst(items));
        assertEquals("a,b", StringUtils.join(CollectionHelper.getFirst(items, 2), ","));
        assertEquals("a,b,c", StringUtils.join(CollectionHelper.getFirst(items, 4), ","));
    }

    @Test
    public void testGetSublist() {
        List<String> items = new ArrayList<String>(Arrays.asList("a", "b", "c"));

        assertEquals(1, CollectionHelper.getSublist(items, 1, 1).size());
        assertEquals(3, CollectionHelper.getSublist(items, 0, 3).size());
        assertEquals(0, CollectionHelper.getSublist(items, 3, 0).size());
        assertEquals(0, CollectionHelper.getSublist(items, 10, 13).size());
        assertEquals(3, CollectionHelper.getSublist(items, 0, 54).size());
    }

    @Test
    public void testRemove() {
        List<String> items = new ArrayList<String>(Arrays.asList("a", "b", "c", "d", "a", "b", "c"));
        boolean filtered = CollectionHelper.remove(items, new Filter<String>() {
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
            this.name = name;
        }

        public NameObject(int age) {
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

    }

    @Test
    public void testLimitIterable() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Iterator<Integer> iterator = CollectionHelper.limit(list, 5).iterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        assertEquals(5, count);

        iterator = CollectionHelper.limit(list, 15).iterator();
        count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        assertEquals(10, count);

        iterator = CollectionHelper.limit(Collections.<Integer> emptyList().iterator(), 0);
        assertFalse(iterator.hasNext());
    }
}
