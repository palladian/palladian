package ws.palladian.helper.date;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DateExactnessTest {

    @Test
    public void testIsInRange() {
        assertTrue(DateExactness.SECOND.provides(DateExactness.MINUTE));
        assertTrue(DateExactness.SECOND.provides(DateExactness.SECOND));
        assertFalse(DateExactness.MINUTE.provides(DateExactness.SECOND));
    }

}
