package ws.palladian.helper.date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ws.palladian.helper.date.dates.DateExactness;

public class DateExactnessTest {
    
    @Test
    public void testIsInRange() {
        assertTrue(DateExactness.MINUTE.inRange(DateExactness.SECOND));
        assertTrue(DateExactness.SECOND.inRange(DateExactness.SECOND));
        assertFalse(DateExactness.SECOND.inRange(DateExactness.MINUTE));
    }

}
