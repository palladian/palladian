package ws.palladian.extraction.date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;

public class DateRaterHelperTest {

    @Test
    public void testIsDateInRange() {

        ExtractedDate date = DateParser.parseDate("2010-01-01T12:30:30Z", RegExp.DATE_ISO8601_YMD_T);
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = DateParser.parseDate("1990-11-13T00:00:00Z", RegExp.DATE_ISO8601_YMD_T);
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = new ExtractedDate();
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = DateParser.parseDate("1990-11-12T00:00:00Z", RegExp.DATE_ISO8601_YMD_T);
        assertFalse(DateRaterHelper.isDateInRange(date));
        date = DateParser.parseDate("2090-11-12T00:00:00Z", RegExp.DATE_ISO8601_YMD_T);
        assertFalse(DateRaterHelper.isDateInRange(date));
        date = DateParser.parseDate("Nov 8, 2007", RegExp.DATE_USA_MMMM_D_Y);
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = DateParser.parseDate("3.9.2010", RegExp.DATE_EU_D_MM_Y);
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = DateParser.parseDate("2010-09", RegExp.DATE_ISO8601_YM);
        assertTrue(DateRaterHelper.isDateInRange(date));

    }
}
