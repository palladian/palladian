package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CountMapTest {

    @Test
    public void testCountMap() {

        CountMap<String> countMap = CountMap.create();
        countMap.add("one");
        countMap.add("one");
        countMap.add("one");
        countMap.add("two");
        countMap.add("two");
        countMap.add("two");
        countMap.add("two");
        countMap.add("two");
        countMap.add("three");
        countMap.add("three");

        assertEquals(3, countMap.get("one"));
        assertEquals(5, countMap.get("two"));
        assertEquals(2, countMap.get("three"));
        assertEquals(0, countMap.get("four"));

        assertEquals(3, countMap.uniqueSize());
        assertEquals(10, countMap.totalSize());

        assertEquals("two", countMap.getHighest());

        CountMap<String> highest = countMap.getHighest(2);
        assertEquals(2, highest.uniqueSize());
        assertEquals(5, highest.get("two"));
        assertEquals(3, highest.get("one"));

        assertTrue(countMap.remove("one"));
        assertEquals(0, countMap.get("one"));
        assertEquals(7, countMap.totalSize());
        assertEquals(2, countMap.uniqueSize());
        
        countMap.set("three", 0);
        assertEquals(0, countMap.get("three"));
        assertEquals(5, countMap.totalSize());
        assertEquals(1, countMap.uniqueSize());

    }

}
