package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CountMapTest {

    @Test
    public void testCountMap() {

        CountMap countMap = new CountMap();
        countMap.increment("one");
        countMap.increment("one");
        countMap.increment("one");
        countMap.increment("two");
        countMap.increment("two");
        countMap.increment("two");
        countMap.increment("two");
        countMap.increment("two");
        countMap.increment("three");
        countMap.increment("three");

        assertEquals(3, (int) countMap.get("one"));
        assertEquals(5, (int) countMap.get("two"));
        assertEquals(2, (int) countMap.get("three"));

        assertEquals(3, countMap.size());
        assertEquals(10, countMap.totalSize());

    }

}
