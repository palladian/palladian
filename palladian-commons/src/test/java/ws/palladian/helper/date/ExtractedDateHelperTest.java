package ws.palladian.helper.date;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Martin Gregor
 */
public class ExtractedDateHelperTest {

    @Test
    public void testGet2Digits() {
        assertEquals("00", ExtractedDateHelper.get2Digits(0));
        assertEquals("09", ExtractedDateHelper.get2Digits(9));
        assertEquals("10", ExtractedDateHelper.get2Digits(10));
    }

}
