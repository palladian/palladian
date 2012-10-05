package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MultiMapTest {
    
    @Test
    public void testMultiMap() {
        MultiMap<String, String> multiMap = new MultiMap<String, String>();
        multiMap.put("key1", "value1");
        multiMap.put("key1", "value2");
        multiMap.put("key1", "value3");
        multiMap.put("key2", "value1");
        multiMap.put("key3", "value5");
        multiMap.put("key3", "value6");
        
        assertEquals(3, multiMap.size());
        assertEquals(6, multiMap.allValues().size());
    }

}
