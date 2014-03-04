package ws.palladian.helper.collection;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

public class MruMapTest {
    @Test
    public void testMruMap() {
        Map<Integer, String> mruMap = new MruMap<Integer, String>(5, true);
        mruMap.put(1, "one");
        mruMap.put(2, "two");
        mruMap.put(3, "three");
        mruMap.put(4, "four");
        mruMap.put(5, "five");
        mruMap.get(1);
        mruMap.put(6, "six");
        assertTrue(mruMap.keySet().containsAll(Arrays.asList(1, 3, 4, 5, 6)));
    }

}
