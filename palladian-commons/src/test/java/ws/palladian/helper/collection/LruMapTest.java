package ws.palladian.helper.collection;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class LruMapTest {
    @Test
    public void testMruMap_accessOrder() {
        Map<Integer, String> lruMap = LruMap.accessOrder(5);
        putGet(lruMap);
        assertTrue(lruMap.keySet().equals(new HashSet<>(Arrays.asList(1, 3, 4, 5, 6))));
    }

    @Test
    public void testMruMap_insertionOrder() {
        Map<Integer, String> lruMap = LruMap.insertionOrder(5);
        putGet(lruMap);
        assertTrue(lruMap.keySet().equals(new HashSet<>(Arrays.asList(2, 3, 4, 5, 6))));
    }

    private void putGet(Map<Integer, String> mruMap) {
        mruMap.put(1, "one");
        mruMap.put(2, "two");
        mruMap.put(3, "three");
        mruMap.put(4, "four");
        mruMap.put(5, "five");
        mruMap.get(1);
        mruMap.put(6, "six");
    }

}
