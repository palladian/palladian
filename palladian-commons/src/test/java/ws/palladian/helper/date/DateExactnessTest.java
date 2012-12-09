package ws.palladian.helper.date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DateExactnessTest {

    @Test
    public void testIsInRange() {
        assertTrue(DateExactness.SECOND.provides(DateExactness.MINUTE));
        assertTrue(DateExactness.SECOND.provides(DateExactness.SECOND));
        assertFalse(DateExactness.MINUTE.provides(DateExactness.SECOND));
    }

}
