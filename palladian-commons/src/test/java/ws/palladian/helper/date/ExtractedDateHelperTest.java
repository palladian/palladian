package ws.palladian.helper.date;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Martin Gregor
 */
public class ExtractedDateHelperTest {

    @Test
    public void testGetMonthNumber() {
        assertEquals("01", ExtractedDateHelper.getMonthNumber("Januar"));
        assertEquals("02", ExtractedDateHelper.getMonthNumber("Februar"));
        assertEquals("03", ExtractedDateHelper.getMonthNumber("März"));
        assertEquals("04", ExtractedDateHelper.getMonthNumber("April"));
        assertEquals("05", ExtractedDateHelper.getMonthNumber("Mai"));
        assertEquals("06", ExtractedDateHelper.getMonthNumber("Juni"));
        assertEquals("07", ExtractedDateHelper.getMonthNumber("Juli"));
        assertEquals("08", ExtractedDateHelper.getMonthNumber("August"));
        assertEquals("09", ExtractedDateHelper.getMonthNumber("September"));
        assertEquals("10", ExtractedDateHelper.getMonthNumber("Oktober"));
        assertEquals("11", ExtractedDateHelper.getMonthNumber("November"));
        assertEquals("12", ExtractedDateHelper.getMonthNumber("Dezember"));

        assertEquals("01", ExtractedDateHelper.getMonthNumber("January"));
        assertEquals("02", ExtractedDateHelper.getMonthNumber("February"));
        assertEquals("03", ExtractedDateHelper.getMonthNumber("March"));
        assertEquals("04", ExtractedDateHelper.getMonthNumber("April"));
        assertEquals("05", ExtractedDateHelper.getMonthNumber("May"));
        assertEquals("06", ExtractedDateHelper.getMonthNumber("June"));
        assertEquals("07", ExtractedDateHelper.getMonthNumber("July"));
        assertEquals("08", ExtractedDateHelper.getMonthNumber("August"));
        assertEquals("09", ExtractedDateHelper.getMonthNumber("September"));
        assertEquals("10", ExtractedDateHelper.getMonthNumber("October"));
        assertEquals("11", ExtractedDateHelper.getMonthNumber("November"));
        assertEquals("12", ExtractedDateHelper.getMonthNumber("December"));

        assertEquals("01", ExtractedDateHelper.getMonthNumber("Jan"));
        assertEquals("02", ExtractedDateHelper.getMonthNumber("Feb"));
        assertEquals("03", ExtractedDateHelper.getMonthNumber("Mär"));
        assertEquals("04", ExtractedDateHelper.getMonthNumber("Apr"));
        assertEquals("05", ExtractedDateHelper.getMonthNumber("Mai"));
        assertEquals("06", ExtractedDateHelper.getMonthNumber("Jun"));
        assertEquals("07", ExtractedDateHelper.getMonthNumber("Jul"));
        assertEquals("08", ExtractedDateHelper.getMonthNumber("Aug"));
        assertEquals("09", ExtractedDateHelper.getMonthNumber("Sep"));
        assertEquals("10", ExtractedDateHelper.getMonthNumber("Okt"));
        assertEquals("11", ExtractedDateHelper.getMonthNumber("Nov"));
        assertEquals("12", ExtractedDateHelper.getMonthNumber("Dez"));

        assertEquals("01", ExtractedDateHelper.getMonthNumber("Jan"));
        assertEquals("02", ExtractedDateHelper.getMonthNumber("Feb"));
        assertEquals("03", ExtractedDateHelper.getMonthNumber("Mar"));
        assertEquals("04", ExtractedDateHelper.getMonthNumber("Apr"));
        assertEquals("05", ExtractedDateHelper.getMonthNumber("May"));
        assertEquals("06", ExtractedDateHelper.getMonthNumber("Jun"));
        assertEquals("07", ExtractedDateHelper.getMonthNumber("Jul"));
        assertEquals("08", ExtractedDateHelper.getMonthNumber("Aug"));
        assertEquals("09", ExtractedDateHelper.getMonthNumber("Sep"));
        assertEquals("10", ExtractedDateHelper.getMonthNumber("Oct"));
        assertEquals("11", ExtractedDateHelper.getMonthNumber("Nov"));
        assertEquals("12", ExtractedDateHelper.getMonthNumber("Dec"));

    }

    @Test
    public void testGet2Digits() {
        assertEquals("00", ExtractedDateHelper.get2Digits(0));
        assertEquals("09", ExtractedDateHelper.get2Digits(9));
        assertEquals("10", ExtractedDateHelper.get2Digits(10));
    }

}
