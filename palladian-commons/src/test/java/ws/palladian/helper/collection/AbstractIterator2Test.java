package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbstractIterator2Test {
	AbstractIterator2<Integer> iterator;

	@Before
	public void setUp() {
		iterator = new AbstractIterator2<Integer>() {
			int idx = 0;

			@Override
			protected Integer getNext() {
				if (idx >= 5) {
					return finished();
				}
				return idx++;
			}
		};

	}

	@Test(expected = UnsupportedOperationException.class)
	public void modificationsAreNotAllowed() {
		iterator.remove();
	}

	@Test
	public void throwsOnNextWhenNoMoreElements() {
		iterator.next();
		iterator.next();
		iterator.next();
		iterator.next();
		iterator.next();
		try {
			iterator.next();
			Assert.fail("Should throw NoSuchElementException when there are no more elements");
		} catch (NoSuchElementException e) {
		}
	}

	@Test
	public void allowsIteratingWithNext() {
		assertEquals(0, (int) iterator.next());
		assertEquals(1, (int) iterator.next());
		assertEquals(2, (int) iterator.next());
		assertEquals(3, (int) iterator.next());
		assertEquals(4, (int) iterator.next());
	}
	
	@Test
	public void allowsIteratingWithNextAndCheckingWithHasNext() {
		assertTrue(iterator.hasNext());
		assertEquals(0, (int) iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals(1, (int) iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals(2, (int) iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals(3, (int) iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals(4, (int) iterator.next());
		assertFalse(iterator.hasNext());
	}

	@Test
	public void hasNextReturnsTrueAsLongAsThereAreMoreElements() {
		assertTrue(iterator.hasNext());
		iterator.next();
		assertTrue(iterator.hasNext());
		iterator.next();
		assertTrue(iterator.hasNext());
		iterator.next();
		assertTrue(iterator.hasNext());
		iterator.next();
		assertTrue(iterator.hasNext());
		iterator.next();
	}

	@Test
	public void hasNextReturnsFalseWhenThereAreNoMoreElements() {
		iterator.next();
		iterator.next();
		iterator.next();
		iterator.next();
		iterator.next();
		assertFalse(iterator.hasNext());
	}

}
