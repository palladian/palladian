package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class MultiMapTest {

    @Test
    public void testListMultiMap() {
        MultiMap<String, String> multiMap = DefaultMultiMap.createWithList();
        fillMap(multiMap);

        assertEquals(4, multiMap.size());
        assertEquals(8, multiMap.allValues().size());
        assertEquals(2, multiMap.get("key4").size());

        multiMap.addAll("key3", Arrays.asList("value7", "value8", "value9"));
        assertEquals(5, multiMap.get("key3").size());
    }

    @Test
    public void testSetMultiMap() {
        MultiMap<String, String> multiMap = DefaultMultiMap.createWithSet();
        fillMap(multiMap);
        assertEquals(4, multiMap.size());
        assertEquals(7, multiMap.allValues().size());
        assertEquals(1, multiMap.get("key4").size());
    }

    void fillMap(MultiMap<String, String> multiMap) {
        multiMap.add("key1", "value1");
        multiMap.add("key1", "value2");
        multiMap.add("key1", "value3");
        multiMap.add("key2", "value1");
        multiMap.add("key3", "value5");
        multiMap.add("key3", "value6");
        multiMap.add("key4", "value1");
        multiMap.add("key4", "value1");
    }

}
