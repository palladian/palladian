package tud.iir.daterecognition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;

import tud.iir.control.AllTests;
import tud.iir.knowledge.RegExp;

public class DateEvaluatorHelperTest {

    @Test
    public void testIsDateInRange() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        ExtractedDate date = new ExtractedDate("2010-01-01T12:30:30Z", RegExp.DATE_ISO8601_YMD_T[1]);
        assertTrue(DateEvaluatorHelper.isDateInRange(date));
        date = new ExtractedDate("1990-11-13T00:00:00Z", RegExp.DATE_ISO8601_YMD_T[1]);
        assertTrue(DateEvaluatorHelper.isDateInRange(date));
        date = new ExtractedDate(cal.get(Calendar.YEAR) + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.MONTH))
                + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.DAY_OF_MONTH)) + "T"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.HOUR_OF_DAY)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.MINUTE)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.SECOND)) + "Z", RegExp.DATE_ISO8601_YMD_T[1]);
        assertTrue(DateEvaluatorHelper.isDateInRange(date));
        date = new ExtractedDate("1990-11-12T00:00:00Z", RegExp.DATE_ISO8601_YMD_T[1]);
        assertFalse(DateEvaluatorHelper.isDateInRange(date));
        date = new ExtractedDate("2090-11-12T00:00:00Z", RegExp.DATE_ISO8601_YMD_T[1]);
        assertFalse(DateEvaluatorHelper.isDateInRange(date));
    }

    @Test
    public void testFilter() {
        final String url = "data/test/webPages/dateExtraction/zeit1.htm";
        if (!AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllTrue();
            dateGetter.setTechArchive(false);
            dateGetter.setTechReference(false);
            date.addAll(dateGetter.getDate());
            ArrayList<ExtractedDate> filter = DateEvaluatorHelper.filter(date, ExtractedDate.TECH_HTML_CONT);
            assertEquals(6, filter.size());
            filter = DateEvaluatorHelper.filter(date, ExtractedDate.TECH_HTML_STRUC);
            assertEquals(4, filter.size());
        }
    }
}
