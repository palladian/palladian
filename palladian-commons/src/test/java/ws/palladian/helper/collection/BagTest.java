package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.io.FileHelper;

public class BagTest {

    // elements in the Bag for testing
    private static final String ELEMENT4 = "four";
    private static final String ELEMENT3 = "three";
    private static final String ELEMENT2 = "two";
    private static final String ELEMENT1 = "one";

    private Bag<String> bag1;
    private Bag<String> bag2;

    @Before
    public void setupTest() {
        bag1 = Bag.create();
        bag1.add(ELEMENT1);
        bag1.add(ELEMENT1);
        bag1.add(ELEMENT1);
        bag1.add(ELEMENT2);
        bag1.add(ELEMENT2);
        bag1.add(ELEMENT2);
        bag1.add(ELEMENT2);
        bag1.add(ELEMENT2);
        bag1.add(ELEMENT3);
        bag1.add(ELEMENT3);
        bag2 = Bag.create();
        bag2.add(ELEMENT1, 5);
        bag2.add(ELEMENT2, 3);
        bag2.add(ELEMENT3, 1);
        bag2.add(ELEMENT4, 7);
    }

    @Test
    public void testCount() {
        assertEquals(3, bag1.count(ELEMENT1));
        assertEquals(5, bag1.count(ELEMENT2));
        assertEquals(2, bag1.count(ELEMENT3));
        assertEquals(0, bag1.count(ELEMENT4));
    }

    @Test
    public void testSize() {
        assertEquals(3, bag1.unique().size());
        assertEquals(10, bag1.size());
    }

    @Test
    public void testMax() {
        assertEquals(ELEMENT2, bag1.getMax().getKey());
    }

    @Test
    public void testIteratorIteration() {
        Iterator<String> iterator = bag1.iterator();
        assertEquals(10, CollectionHelper.count(iterator));
    }

    @Test
    public void testIteratorRemove() {
        Iterator<String> iterator = bag1.iterator();
        // remove one ELEMENT1
        while (iterator.hasNext()) {
            String current = iterator.next();
            if (current.equals(ELEMENT1)) {
                iterator.remove();
                break;
            }
        }
        assertEquals(2, bag1.count(ELEMENT1));
    }

    @Test
    public void testCreateSorted() {
        Bag<String> sorted = bag1.createSorted(Order.DESCENDING);
        Iterator<Entry<String, Integer>> iterator = sorted.unique().iterator();
        Entry<String, Integer> entry = iterator.next();
        assertEquals(ELEMENT2, entry.getKey());
        assertEquals(5, (int)entry.getValue());
        entry = iterator.next();
        assertEquals(ELEMENT1, entry.getKey());
        assertEquals(3, (int)entry.getValue());
    }

    @Test
    public void testRemoveAll() {
        assertEquals(3, bag1.removeAll(ELEMENT1));
        assertEquals(0, bag1.count(ELEMENT1));
        assertEquals(7, bag1.size());
        assertEquals(2, bag1.unique().size());
    }

    @Test
    public void testSet() {
        bag1.set(ELEMENT3, 0);
        assertEquals(0, bag1.count(ELEMENT3));
        assertEquals(8, bag1.size());
        assertEquals(2, bag1.unique().size());
    }
    
    @Test
    public void testSet2() {
        bag1.set("four", 5);
        bag1.set("four", 10);
        bag1.set("four", 5);
        assertEquals(15, bag1.size());
    }

    @Test
    public void testBagAdd() {
        bag2.addAll(bag1);
        assertEquals(8, bag2.count(ELEMENT1));
        assertEquals(8, bag2.count(ELEMENT2));
        assertEquals(3, bag2.count(ELEMENT3));
        assertEquals(7, bag2.count(ELEMENT4));
    }

    @Test
    public void testBagRemoveAll() {
        bag2.removeAll(Arrays.asList(ELEMENT1, ELEMENT3));
        assertEquals(0, bag2.count(ELEMENT1));
        assertEquals(0, bag2.count(ELEMENT3));
        assertEquals(2, bag2.unique().size());
        assertEquals(10, bag2.size());
        assertEquals(3, bag2.count(ELEMENT2));
    }

    @Test
    public void testToMap() {
        Map<String, Integer> map = bag1.toMap();
        assertEquals(3, map.size());
        assertEquals(3, (int)map.get(ELEMENT1));
        assertEquals(5, (int)map.get(ELEMENT2));
        assertEquals(2, (int)map.get(ELEMENT3));
    }

    @Test
    public void testSerialization() throws IOException {
        File tempDir = FileHelper.getTempDir();
        File tempFile = new File(tempDir, "bagSerialization_" + System.currentTimeMillis() + ".ser");
        FileHelper.serialize(bag1, tempFile.getPath());
        Bag<String> deserialized = FileHelper.deserialize(tempFile.getPath());
        assertEquals(bag1, deserialized);
    }

    @Test
    public void testCopy() {
        Bag<String> copy = Bag.create(bag1);
        assertTrue(copy.equals(bag1));
    }

}
