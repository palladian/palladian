package ws.palladian.helper;

import junit.framework.Assert;

import org.junit.Test;

import ws.palladian.helper.CountMap;

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

        Assert.assertEquals(3, (int) countMap.get("one"));
        Assert.assertEquals(5, (int) countMap.get("two"));
        Assert.assertEquals(2, (int) countMap.get("three"));

        Assert.assertEquals(3, (int) countMap.size());
        Assert.assertEquals(10, (int) countMap.totalSize());

    }

}
