package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class CaseInsensitiveMapTest {

    @Test
    public void testCaseInsensitiveMap() {
        CaseInsensitiveMap<Integer> map = new CaseInsensitiveMap<Integer>();
        map.put("apple", 1);
        map.put("banana", 5);
        map.put("ORANGE", 7);
        map.put("Banana", 3);

        assertEquals(1, (int)map.get("apple"));
        assertEquals(1, (int)map.get("Apple"));
        assertEquals(3, (int)map.get("Banana"));
        assertEquals(7, (int)map.get("orange"));

        // test copying an existing map
        Map<String, Integer> existingMap = new HashMap<String, Integer>();
        existingMap.put("apple", 1);
        existingMap.put("banana", 5);
        existingMap.put("ORANGE", 7);
        existingMap.put("Banana", 3);
        map = new CaseInsensitiveMap<Integer>(existingMap);

        assertEquals(1, (int)map.get("apple"));
        assertEquals(1, (int)map.get("Apple"));
        assertEquals(3, (int)map.get("Banana"));
        assertEquals(7, (int)map.get("orange"));

    }

}
