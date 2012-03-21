package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

    // @Test
    // public void removeNullElementsTest() {
    // ArrayList<String> array = new ArrayList<String>();
    // String temp = null;
    // array.add(temp);
    // temp = "1";
    // array.add(temp);
    // temp = "2";
    // array.add(temp);
    // temp = null;
    // array.add(temp);
    // temp = "3";
    // array.add(temp);
    // temp = null;
    // array.add(temp);
    // temp = "4";
    // array.add(temp);
    // temp = null;
    // array.add(temp);
    // array = CollectionHelper.removeNullElements(array);
    // assertEquals(4, array.size());
    // for (int i = 0; i < array.size(); i++) {
    // assertEquals(i + 1, Integer.parseInt(array.get(i)));
    // }
    // }

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

}
