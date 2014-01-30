package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;

public class FilterIteratorTest {

    private Iterator<Integer> iterator;

    @Before
    public void setUp() {
        Collection<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        // filter for even numbers
        iterator = new FilterIterator<Integer>(numbers.iterator(), new Filter<Number>() {
            @Override
            public boolean accept(Number value) {
                return value.intValue() % 2 == 0;
            }
        });
    }

    @Test
    public void testFilterIterator() {
        assertTrue(iterator.hasNext());
        assertEquals((Integer)2, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals((Integer)4, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals((Integer)6, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals((Integer)8, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals((Integer)10, iterator.next());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testFilterIterator2() {
        assertEquals((Integer)2, iterator.next());
        assertEquals((Integer)4, iterator.next());
        assertEquals((Integer)6, iterator.next());
        assertEquals((Integer)8, iterator.next());
        assertEquals((Integer)10, iterator.next());
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            fail();
        } catch (NoSuchElementException e) {
            assertEquals("No (more) elements", e.getMessage());
        }
    }

}
