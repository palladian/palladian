package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MultiMapTest {
    
    @Test
    public void testMultiMap() {
        MultiMap<String, String> multiMap = new MultiMap<String, String>();
        multiMap.add("key1", "value1");
        multiMap.add("key1", "value2");
        multiMap.add("key1", "value3");
        multiMap.add("key2", "value1");
        multiMap.add("key3", "value5");
        multiMap.add("key3", "value6");
        
        assertEquals(3, multiMap.size());
        assertEquals(6, multiMap.allValues().size());
    }

}
