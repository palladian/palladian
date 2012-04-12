package de.philippkatz.helper;

import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;


public class SingleIteratorTest {

    @Test
    public void testSingleIterator() {
        Object object = new Object();
        SingleIterator<Object> iterator = new SingleIterator<Object>(object);
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(object, iterator.next());
        Assert.assertFalse(iterator.hasNext());
        try {
            iterator.next();
            Assert.fail();
        } catch (NoSuchElementException e) {

        }
    }

}
