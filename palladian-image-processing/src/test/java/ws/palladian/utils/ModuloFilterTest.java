package ws.palladian.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModuloFilterTest {
    @Test
    public void testSplitFilter() {
        ModuloFilter filter = new ModuloFilter(true);
        assertTrue(filter.test(new Object()));
        assertFalse(filter.test(new Object()));
        assertTrue(filter.test(new Object()));

        filter = new ModuloFilter(false);
        assertFalse(filter.test(new Object()));
        assertTrue(filter.test(new Object()));
        assertFalse(filter.test(new Object()));
    }

}
