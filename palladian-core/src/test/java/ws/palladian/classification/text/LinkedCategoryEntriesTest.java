package ws.palladian.classification.text;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

import ws.palladian.core.Category;

public class LinkedCategoryEntriesTest {
	@Test
	public void testSort() {
		LinkedCategoryEntries categoryEntries = new LinkedCategoryEntries();
		categoryEntries.increment("one", 10);
		categoryEntries.increment("two", 15);
		categoryEntries.increment("three", 5);
		categoryEntries.increment("four", 1);
		// System.out.println(categoryEntries);
		categoryEntries.sortByCount();
		// System.out.println(categoryEntries);
		Iterator<Category> iterator = categoryEntries.iterator();
		assertEquals("two", iterator.next().getName());
		assertEquals("one", iterator.next().getName());
		assertEquals("three", iterator.next().getName());
		assertEquals("four", iterator.next().getName());
	}

}
