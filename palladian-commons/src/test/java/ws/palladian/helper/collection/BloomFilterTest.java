package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class BloomFilterTest {

    @Test
    public void testNumHashFunctions() {
        BloomFilter<String> filter = new BloomFilter<String>(10, 20);
        assertEquals(2, filter.getNumHashFunctions(), 0.01); // 1.39
        filter = new BloomFilter<String>(10, 80);
        assertEquals(6, filter.getNumHashFunctions(), 0.01); // 5.55
        filter = new BloomFilter<String>(10, 150);
        assertEquals(11, filter.getNumHashFunctions(), 0.01); // 10.4
    }

    @Test
    public void testBloomFilter() {
        BloomFilter<String> filter = new BloomFilter<String>(0.01, 8);
        Set<String> items = CollectionHelper.newHashSet("apple", "blueberry", "cherry", "durian", "grape", "kiwi",
                "lemon", "melon");
        filter.addAll(items);
        assertTrue(CollectionHelper.acceptAll(items, filter));
        assertFalse(filter.accept("mango"));
        assertFalse(filter.accept("pineapple"));
        // System.out.println(filter);
    }

}
