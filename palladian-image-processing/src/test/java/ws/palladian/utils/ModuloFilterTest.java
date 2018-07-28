package ws.palladian.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ws.palladian.utils.ModuloFilter;

public class ModuloFilterTest {
	@Test
	public void testSplitFilter() {
		ModuloFilter filter = new ModuloFilter(true);
		assertTrue(filter.accept(new Object()));
		assertFalse(filter.accept(new Object()));
		assertTrue(filter.accept(new Object()));

		filter = new ModuloFilter(false);
		assertFalse(filter.accept(new Object()));
		assertTrue(filter.accept(new Object()));
		assertFalse(filter.accept(new Object()));
	}

}
