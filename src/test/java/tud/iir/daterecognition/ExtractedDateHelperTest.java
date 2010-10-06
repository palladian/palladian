/**
 * 
 */
package tud.iir.daterecognition;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Martin Gregor
 * 
 */
public class ExtractedDateHelperTest {

    /**
     * Test method for {@link tud.iir.daterecognition.ExtractedDateHelper#getMonthNumber(java.lang.String)}.
     */
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

    /**
     * Test method for {@link tud.iir.daterecognition.ExtractedDateHelper#normalizeYear(java.lang.String)}.
     */
    @Test
    public void testNormalizeYear() {

        assertEquals(1999, ExtractedDateHelper.normalizeYear("'99"));
        assertEquals(2003, ExtractedDateHelper.normalizeYear("'03"));
        assertEquals(2010, ExtractedDateHelper.normalizeYear("'10"));
        assertEquals(1915, ExtractedDateHelper.normalizeYear("'15"));
        assertEquals(1915, ExtractedDateHelper.normalizeYear("15"));
        assertEquals(1915, ExtractedDateHelper.normalizeYear("1915"));

    }

    /**
     * Test method for {@link tud.iir.daterecognition.ExtractedDateHelper#removeNodigits(java.lang.String)}.
     */
    @Test
    public void testRemoveNodigits() {
        assertEquals("23", ExtractedDateHelper.removeNodigits("23."));
        assertEquals("23", ExtractedDateHelper.removeNodigits("'23."));
        assertEquals("23", ExtractedDateHelper.removeNodigits("23,"));
        assertEquals("21", ExtractedDateHelper.removeNodigits("21st"));
        assertEquals("22", ExtractedDateHelper.removeNodigits("22nd"));
        assertEquals("23", ExtractedDateHelper.removeNodigits("23rd"));
        assertEquals("24", ExtractedDateHelper.removeNodigits("24th"));
    }

    /**
     * Test method for {@link tud.iir.daterecognition.ExtractedDateHelper#get4DigitYear(int)}.
     */
    @Test
    public void testGet4DigitYear() {
        assertEquals(1999, ExtractedDateHelper.get4DigitYear(99));

        assertEquals(2010, ExtractedDateHelper.get4DigitYear(10));
    }

    /**
     * Test method for {@link tud.iir.daterecognition.ExtractedDateHelper#getSeparator(java.lang.String)}.
     */
    @Test
    public void testGetSeparator() {
        assertEquals("\\.", ExtractedDateHelper.getSeparator("10.10.2010"));
        assertEquals("-", ExtractedDateHelper.getSeparator("10-10-2010"));
        assertEquals("_", ExtractedDateHelper.getSeparator("10_10_2010"));
        assertEquals("/", ExtractedDateHelper.getSeparator("10/10/2010"));

    }

    /**
     * Test method for {@link tud.iir.daterecognition.ExtractedDateHelper#get2Digits(int)}.
     */
    @Test
    public void testGet2Digits() {
        assertEquals("00", ExtractedDateHelper.get2Digits(0));
        assertEquals("09", ExtractedDateHelper.get2Digits(9));
        assertEquals("10", ExtractedDateHelper.get2Digits(10));

    }

    /**
     * Test method for {@link tud.iir.daterecognition.ExtractedDateHelper#removeTimezone(java.lang.String)}.
     */
    @Test
    public void testRemoveTimezone() {
        assertEquals("22:10", ExtractedDateHelper.removeTimezone("22:10  UTC")[0]);
        assertEquals("22:10", ExtractedDateHelper.removeTimezone("22:10 UTC")[0]);
        assertEquals("22:10", ExtractedDateHelper.removeTimezone("22:10 GMT")[0]);
    }
}
