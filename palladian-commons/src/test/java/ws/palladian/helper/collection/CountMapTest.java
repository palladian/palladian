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

        assertEquals(3, countMap.getCount("one"));
        assertEquals(5, countMap.getCount("two"));
        assertEquals(2, countMap.getCount("three"));
        assertEquals(0, countMap.getCount("four"));

        assertEquals(3, countMap.uniqueSize());
        assertEquals(10, countMap.totalSize());

        assertEquals("two", countMap.getHighest());

        CountMap<String> highest = countMap.getHighest(2);
        assertEquals(2, highest.uniqueSize());
        assertEquals(5, highest.getCount("two"));
        assertEquals(3, highest.getCount("one"));

        assertTrue(countMap.remove("one"));
        assertEquals(0, countMap.getCount("one"));
        assertEquals(7, countMap.totalSize());
        assertEquals(2, countMap.uniqueSize());
        
        countMap.set("three", 0);
        assertEquals(0, countMap.getCount("three"));
        assertEquals(5, countMap.totalSize());
        assertEquals(1, countMap.uniqueSize());

    }
    
    @Test
    public void testCountMapAdd() {
        CountMap<String> countMap1 = CountMap.create();
        countMap1.add("one", 5);
        countMap1.add("two", 3);
        countMap1.add("three", 1);
        
        CountMap<String> countMap2 = CountMap.create();
        countMap2.add("one", 10);
        countMap2.add("two", 5);
        countMap2.add("four", 7);
        
        countMap1.addAll(countMap2);
        
        assertEquals(15, countMap1.getCount("one"));
        assertEquals(8, countMap1.getCount("two"));
        assertEquals(1, countMap1.getCount("three"));
        assertEquals(7, countMap1.getCount("four"));
    }

}
