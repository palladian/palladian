package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.BitSet;
import java.util.Set;

import org.junit.Test;

public class BloomFilterTest {

    @Test
    public void testNumHashFunctions() {
        BloomFilter<String> filter = new BloomFilter<String>(10, 20);
        assertEquals(1.39, filter.getNumHashFunctions(), 0.01);
        filter = new BloomFilter<String>(10, 80);
        assertEquals(5.55, filter.getNumHashFunctions(), 0.01);
        filter = new BloomFilter<String>(10, 150);
        assertEquals(10.4, filter.getNumHashFunctions(), 0.01);
    }

    @Test
    public void testBloomFilter() {
        BloomFilter<String> filter = new BloomFilter<String>(0.01, 15);
        Set<String> items = CollectionHelper.newHashSet("apple", "blueberry", "cherry", "durian", "grape", "kiwi",
                "lemon", "melon");
        filter.addAll(items);
        assertTrue(CollectionHelper.acceptAll(items, filter));
        assertFalse(filter.accept("mango"));
        assertFalse(filter.accept("pineapple"));
    }
    
    @Test
    public void testBitsetContainsAll() {
        BitSet s1 = newBitSet(1, 2, 3, 4, 6);
        BitSet s2 = newBitSet(1, 2, 3, 4);
        BitSet s3 = newBitSet(1, 2, 3, 4, 5);

        assertTrue(BloomFilter.containsAll(s1, s2));
        assertFalse(BloomFilter.containsAll(s1, s3));
        
        
    }
    private static BitSet newBitSet(int... values) {
        BitSet bitSet = new BitSet();
        for (int value : values) {
            bitSet.set(value);
        }
        return bitSet;
    }

}
