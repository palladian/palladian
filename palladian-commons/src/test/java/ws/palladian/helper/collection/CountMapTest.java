package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CountMapTest {

    @Test
    public void testCountMap() {

        CountMap<String> countMap = CountMap.create();
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

        assertEquals(3, countMap.get("one"));
        assertEquals(5, countMap.get("two"));
        assertEquals(2, countMap.get("three"));
        assertEquals(0, countMap.get("four"));

        assertEquals(3, countMap.uniqueSize());
        assertEquals(10, countMap.totalSize());

    }

}
