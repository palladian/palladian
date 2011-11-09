package de.philippkatz.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;


public class CompositeIteratorTest {

    @Test
    public void testCompositeIterator() {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        CompositeIterator<Integer> iterator = new CompositeIterator<Integer>(list1.iterator(), list2.iterator());
        assertTrue(iterator.hasNext());
        assertEquals((Integer) 1, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals((Integer) 2, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals((Integer) 3, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals((Integer) 4, iterator.next());
        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail();
        } catch (NoSuchElementException e) {
            // must be thrown
        }

    }

}
