package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper.Order;

public class BagTest {

    @Test
    public void testBag() {
        Bag<String> bag = Bag.create();
        bag.add("one");
        bag.add("one");
        bag.add("one");
        bag.add("two");
        bag.add("two");
        bag.add("two");
        bag.add("two");
        bag.add("two");
        bag.add("three");
        bag.add("three");

        assertEquals(3, bag.count("one"));
        assertEquals(5, bag.count("two"));
        assertEquals(2, bag.count("three"));
        assertEquals(0, bag.count("four"));

        assertEquals(3, bag.unique().size());
        assertEquals(10, bag.size());

        assertEquals("two", bag.getMax().getKey());

        Bag<String> sorted = bag.createSorted(Order.DESCENDING);
        Iterator<Entry<String, Integer>> iterator = sorted.unique().iterator();
        Entry<String, Integer> entry = iterator.next();
        assertEquals("two", entry.getKey());
        assertEquals(5, (int)entry.getValue());
        entry = iterator.next();
        assertEquals("one", entry.getKey());
        assertEquals(3, (int)entry.getValue());

        assertEquals(3, bag.removeAll("one"));
        assertEquals(0, bag.count("one"));
        assertEquals(7, bag.size());
        assertEquals(2, bag.unique().size());

        bag.set("three", 0);
        assertEquals(0, bag.count("three"));
        assertEquals(5, bag.size());
        assertEquals(1, bag.unique().size());
    }

    @Test
    public void testBagAdd() {
        Bag<String> bag1 = Bag.create();
        bag1.add("one", 5);
        bag1.add("two", 3);
        bag1.add("three", 1);

        Bag<String> bag2 = Bag.create();
        bag2.add("one", 10);
        bag2.add("two", 5);
        bag2.add("four", 7);

        bag1.addAll(bag2);

        assertEquals(15, bag1.count("one"));
        assertEquals(8, bag1.count("two"));
        assertEquals(1, bag1.count("three"));
        assertEquals(7, bag1.count("four"));
    }

    @Test
    public void testBagRemoveAll() {
        Bag<String> bag = Bag.create();
        bag.add("one", 5);
        bag.add("two", 3);
        bag.add("three", 1);
        bag.removeAll(Arrays.asList("one", "three"));
        assertEquals(1, bag.unique().size());
        assertEquals(3, bag.size());
        assertEquals(3, bag.count("two"));
    }

}
