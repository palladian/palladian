package ws.palladian.helper;

import java.util.Iterator;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.junit.Test;

import ws.palladian.helper.collection.TTLMap;

public class TTLMapTest {

    @Test
    public void testMapFunctionality() {

        TTLMap<Integer, String> map = new TTLMap<Integer, String>();

        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(4, "four");
        map.put(5, "six");

        Assert.assertEquals(5, map.size());

        Assert.assertEquals("two", map.put(2, "two"));
        Assert.assertEquals("six", map.put(5, "five"));

        map.remove(4);
        Assert.assertEquals(4, map.size());

        Iterator<Entry<Integer, String>> iterator = map.entrySet().iterator();
        Entry<Integer, String> next = iterator.next();
        Assert.assertEquals(next.getValue(), "one");

        iterator.remove();
        Assert.assertEquals(3, map.size());

    }

    @Test
    public void testTtlFunctionality() throws InterruptedException {

        TTLMap<Integer, String> map = new TTLMap<Integer, String>();
        map.setTimeToLive(1000);
        map.setCleanInterval(500);

        // insert 100 items
        fillMap(map, 0, 100);
        Assert.assertEquals(100, map.size());

        // wait 0.5 seconds, insert another 100 items
        Thread.sleep(500);
        fillMap(map, 100, 100);
        Assert.assertEquals(200, map.size());

        // wait 2 seconds, the map should now be empty
        Thread.sleep(2000);
        Assert.assertEquals(0, map.size());
        
        Assert.assertEquals(200, map.getCleanCounter());

    }

    private void fillMap(TTLMap<Integer, String> map, int firstEntry, int numEntries) {
        for (int i = firstEntry; i < firstEntry + numEntries; i++) {
            map.put(i, "value:" + i);
        }
    }


}
