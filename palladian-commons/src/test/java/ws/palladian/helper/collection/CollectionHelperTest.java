package ws.palladian.helper.collection;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Test;
import ws.palladian.helper.collection.CollectionHelper.Order;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.Assert.*;

/**
 * @author Philipp Katz
 */
public class CollectionHelperTest {

    private static class NameObject {
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
    public void testSortyMapByValue() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 5);
        map.put(3, 3);
        map.put(2, 4);
        map.put(5, 1);
        map.put(4, 2);
        Map<Integer, Integer> mapSortedByValue = CollectionHelper.sortByValue(map);
        Iterator<Entry<Integer, Integer>> iterator = mapSortedByValue.entrySet().iterator();
        assertEquals((Integer) 1, iterator.next().getValue());
        assertEquals((Integer) 2, iterator.next().getValue());
        assertEquals((Integer) 3, iterator.next().getValue());
        assertEquals((Integer) 2, iterator.next().getKey());
        assertEquals((Integer) 1, iterator.next().getKey());

        Map<Integer, Integer> mapSortedByValueDescending = CollectionHelper.sortByValue(map, Order.DESCENDING);
        iterator = mapSortedByValueDescending.entrySet().iterator();
        assertEquals((Integer) 5, iterator.next().getValue());
        assertEquals((Integer) 4, iterator.next().getValue());
        assertEquals((Integer) 3, iterator.next().getValue());
        assertEquals((Integer) 4, iterator.next().getKey());
        assertEquals((Integer) 5, iterator.next().getKey());

    }

    @Test
    public void testRoundRobinList() {
        RoundRobinList<String> rrl = new RoundRobinList<>();
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
        Collection<NameObject> set = new HashSet<>();
        set.add(new NameObject("A"));
        set.add(new NameObject("B"));

        Collection<String> names = CollectionHelper.convertSet(set, new Function<NameObject, String>() {
            @Override
            public String apply(NameObject item) {
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
            public Integer apply(NameObject item) {
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
        List<Integer> list = new ArrayList<>(Arrays.asList(null, 1, 2, 3, 4, null));
        int removed = CollectionHelper.removeNulls(list);
        assertEquals(2, removed);
        assertEquals(4, list.size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void sortByStringKeyLength() {

        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("A", "A");
        hashMap.put("BB", "B");
        hashMap.put("CCC", "C");

        assertEquals("CCC", CollectionHelper.sortByStringKeyLength(hashMap, Order.DESCENDING).entrySet().iterator().next().getKey());
        assertEquals("A", CollectionHelper.sortByStringKeyLength(hashMap, Order.ASCENDING).entrySet().iterator().next().getKey());

    }

    @Test
    public void testLimit() {
        List<String> items = new ArrayList<>(Arrays.asList("a", "b", "c"));

        assertEquals("a", CollectionHelper.getFirst(items));
        assertEquals("a,b", StringUtils.join(CollectionHelper.limit(items, 2), ","));
        assertEquals("a,b,c", StringUtils.join(CollectionHelper.limit(items, 4), ","));
    }

    @Test
    public void testEquals() {
        List<String> items = new ArrayList<>(Arrays.asList("a", "b", "c"));
        List<String> items1 = new ArrayList<>(Arrays.asList("c", "b", "a"));
        List<String> items2 = new ArrayList<>(Arrays.asList("d", "b", "a"));
        assertTrue(CollectionHelper.equals(items,items1));
        assertTrue(!CollectionHelper.equals(items,items2));
    }

    @Test
    public void testGetSublist() {
        List<String> items = new ArrayList<>(Arrays.asList("a", "b", "c"));

        assertEquals(1, CollectionHelper.getSublist(items, 1, 1).size());
        assertEquals(3, CollectionHelper.getSublist(items, 0, 3).size());
        assertEquals(0, CollectionHelper.getSublist(items, 3, 0).size());
        assertEquals(0, CollectionHelper.getSublist(items, 10, 13).size());
        assertEquals(3, CollectionHelper.getSublist(items, 0, 54).size());
    }

    @Test
    public void testGetSubset() {
        LinkedHashSet<String> items = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));

        assertEquals(1, CollectionHelper.getSubset(items, 1, 1).size());
        assertThat(CollectionHelper.getSubset(items, 1, 1), Matchers.hasItem("b"));
        assertEquals(3, CollectionHelper.getSubset(items, 0, 3).size());
        assertEquals(0, CollectionHelper.getSubset(items, 3, 0).size());
        assertEquals(0, CollectionHelper.getSubset(items, 10, 13).size());
        assertEquals(3, CollectionHelper.getSubset(items, 0, 54).size());
    }

    @Test
    public void testRemove() {
        List<String> items = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "a", "b", "c"));
        int filtered = CollectionHelper.remove(items, new Predicate<String>() {
            @Override
            public boolean test(String item) {
                return item.equals("a") || item.equals("b");
            }
        });
        assertEquals(3, filtered);
        assertEquals(4, items.size());
    }

    @Test
    public void testGroupBy() {
        List<String> items = Arrays.asList("one", "two", "three", "four", "five", "six");
        MultiMap<Integer, String> groupedResult = CollectionHelper.groupBy(items, new Function<String, Integer>() {
            @Override
            public Integer apply(String input) {
                return input.length();
            }
        });
        assertEquals(3, groupedResult.size());
        assertTrue(groupedResult.get(3).containsAll(Arrays.asList("one", "two", "six")));
        assertTrue(groupedResult.get(4).containsAll(Arrays.asList("four", "five")));
        assertTrue(groupedResult.get(5).containsAll(Arrays.asList("three")));
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

        iterator = CollectionHelper.limit(Collections.<Integer>emptyList().iterator(), 0);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testDistinct() {
        Set<String> values = CollectionHelper.distinct(Arrays.asList("a", "b", "c"), Arrays.asList("b", "c", "d"));
        assertEquals(4, values.size());
    }

    @Test
    public void testNewHashSet() {
        HashSet<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3, 2, 1));
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testNewArrayList() {
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 2, 1));
        assertEquals(5, list.size());
        assertTrue(list.equals(Arrays.asList(1, 2, 3, 2, 1)));
    }

    @Test
    public void testIntersect() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5, 6, 7));
        Set<Integer> intersection = CollectionHelper.intersect(set1, set2);
        assertEquals(3, intersection.size());
        assertTrue(intersection.containsAll(Arrays.asList(3, 4, 5)));
    }

    @Test
    public void testCreateIndexMap() {
        List<String> list = Arrays.asList("zero", "seven", "one", "one");
        Map<String, Integer> indexMap = CollectionHelper.createIndexMap(list);
        assertEquals(3, indexMap.size());
        assertEquals((Integer) 0, indexMap.get("zero"));
        assertEquals((Integer) 1, indexMap.get("seven"));
        assertEquals((Integer) 2, indexMap.get("one"));
    }

    @Test
    public void testGetLast() {
        LinkedHashSet<String> iterable = new LinkedHashSet<>(Arrays.asList("1", "2", "3", "4", "5"));
        assertEquals("5", CollectionHelper.getLast(iterable));
    }

}
