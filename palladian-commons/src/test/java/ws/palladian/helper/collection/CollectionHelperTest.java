package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

}
