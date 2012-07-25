package ws.palladian.extraction.date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;

import ws.palladian.extraction.date.DateRaterHelper;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.ExtractedDateHelper;
import ws.palladian.helper.date.dates.DateParser;
import ws.palladian.helper.date.dates.ExtractedDate;

public class DateRaterHelperTest {

    @Test
    public void testIsDateInRange() {

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        ExtractedDate date = DateParser.parse("2010-01-01T12:30:30Z", RegExp.DATE_ISO8601_YMD_T.getFormat());
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = DateParser.parse("1990-11-13T00:00:00Z", RegExp.DATE_ISO8601_YMD_T.getFormat());
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = DateParser.parse(cal.get(Calendar.YEAR) + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.MONTH))
                + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.DAY_OF_MONTH)) + "T"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.HOUR_OF_DAY)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.MINUTE)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.SECOND)) + "Z", RegExp.DATE_ISO8601_YMD_T.getFormat());
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = DateParser.parse("1990-11-12T00:00:00Z", RegExp.DATE_ISO8601_YMD_T.getFormat());
        assertFalse(DateRaterHelper.isDateInRange(date));
        date = DateParser.parse("2090-11-12T00:00:00Z", RegExp.DATE_ISO8601_YMD_T.getFormat());
        assertFalse(DateRaterHelper.isDateInRange(date));
        date = DateParser.parse("Nov 8, 2007", RegExp.DATE_USA_MMMM_D_Y.getFormat());
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = DateParser.parse("3.9.2010", RegExp.DATE_EU_D_MM_Y.getFormat());
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = DateParser.parse("2010-09", RegExp.DATE_ISO8601_YM.getFormat());
        assertTrue(DateRaterHelper.isDateInRange(date));

    }
}
