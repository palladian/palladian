package ws.palladian.helper.date.dates;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DateParserTest {
    
    @Test
    public void testNormalizeYear() {
        assertEquals(1999, DateParser.normalizeYear("'99"));
        assertEquals(2003, DateParser.normalizeYear("'03"));
        assertEquals(2010, DateParser.normalizeYear("'10"));
        assertEquals(1915, DateParser.normalizeYear("'15"));
        assertEquals(1915, DateParser.normalizeYear("15"));
        assertEquals(1915, DateParser.normalizeYear("1915"));
        assertEquals(2012, DateParser.normalizeYear("2012\n1"));
    }

    @Test
    public void testRemoveNoDigits() {
        assertEquals("23", DateParser.removeNoDigits("23."));
        assertEquals("23", DateParser.removeNoDigits("'23."));
        assertEquals("23", DateParser.removeNoDigits("23,"));
        assertEquals("21", DateParser.removeNoDigits("21st"));
        assertEquals("22", DateParser.removeNoDigits("22nd"));
        assertEquals("23", DateParser.removeNoDigits("23rd"));
        assertEquals("24", DateParser.removeNoDigits("24th"));
    }

    @Test
    public void testGet4DigitYear() {
        assertEquals(1999, DateParser.get4DigitYear(99));
        assertEquals(2010, DateParser.get4DigitYear(10));
    }
    
    @Test
    public void testRemoveTimezone() {
        assertEquals("22:10 ", DateParser.removeTimezone("22:10  UTC")[0]);
        assertEquals("22:10 ", DateParser.removeTimezone("22:10 UTC")[0]);
        assertEquals("22:10 ", DateParser.removeTimezone("22:10 GMT")[0]);
    }
    
    @Test
    public void testGetSeparator() {
        assertEquals("\\.", DateParser.getSeparator("10.10.2010"));
        assertEquals("-", DateParser.getSeparator("10-10-2010"));
        assertEquals("_", DateParser.getSeparator("10_10_2010"));
        assertEquals("/", DateParser.getSeparator("10/10/2010"));
        assertEquals("-", DateParser.getSeparator("2010-05-06"));
        assertEquals("_", DateParser.getSeparator("2010_05_06"));
        assertEquals("\\.", DateParser.getSeparator("2010.05.06"));
        assertEquals("/", DateParser.getSeparator("2010/05/06"));
    }
    
    @Test
    public void testGetMonthNumber() {
        assertEquals("01", DateParser.getMonthNumber("Januar"));
        assertEquals("02", DateParser.getMonthNumber("Februar"));
        assertEquals("03", DateParser.getMonthNumber("März"));
        assertEquals("04", DateParser.getMonthNumber("April"));
        assertEquals("05", DateParser.getMonthNumber("Mai"));
        assertEquals("06", DateParser.getMonthNumber("Juni"));
        assertEquals("07", DateParser.getMonthNumber("Juli"));
        assertEquals("08", DateParser.getMonthNumber("August"));
        assertEquals("09", DateParser.getMonthNumber("September"));
        assertEquals("10", DateParser.getMonthNumber("Oktober"));
        assertEquals("11", DateParser.getMonthNumber("November"));
        assertEquals("12", DateParser.getMonthNumber("Dezember"));

        assertEquals("01", DateParser.getMonthNumber("January"));
        assertEquals("02", DateParser.getMonthNumber("February"));
        assertEquals("03", DateParser.getMonthNumber("March"));
        assertEquals("04", DateParser.getMonthNumber("April"));
        assertEquals("05", DateParser.getMonthNumber("May"));
        assertEquals("06", DateParser.getMonthNumber("June"));
        assertEquals("07", DateParser.getMonthNumber("July"));
        assertEquals("08", DateParser.getMonthNumber("August"));
        assertEquals("09", DateParser.getMonthNumber("September"));
        assertEquals("10", DateParser.getMonthNumber("October"));
        assertEquals("11", DateParser.getMonthNumber("November"));
        assertEquals("12", DateParser.getMonthNumber("December"));

        assertEquals("01", DateParser.getMonthNumber("Jan"));
        assertEquals("02", DateParser.getMonthNumber("Feb"));
        assertEquals("03", DateParser.getMonthNumber("Mär"));
        assertEquals("04", DateParser.getMonthNumber("Apr"));
        assertEquals("05", DateParser.getMonthNumber("Mai"));
        assertEquals("06", DateParser.getMonthNumber("Jun"));
        assertEquals("07", DateParser.getMonthNumber("Jul"));
        assertEquals("08", DateParser.getMonthNumber("Aug"));
        assertEquals("09", DateParser.getMonthNumber("Sep"));
        assertEquals("10", DateParser.getMonthNumber("Okt"));
        assertEquals("11", DateParser.getMonthNumber("Nov"));
        assertEquals("12", DateParser.getMonthNumber("Dez"));

        assertEquals("01", DateParser.getMonthNumber("Jan"));
        assertEquals("02", DateParser.getMonthNumber("Feb"));
        assertEquals("03", DateParser.getMonthNumber("Mar"));
        assertEquals("04", DateParser.getMonthNumber("Apr"));
        assertEquals("05", DateParser.getMonthNumber("May"));
        assertEquals("06", DateParser.getMonthNumber("Jun"));
        assertEquals("07", DateParser.getMonthNumber("Jul"));
        assertEquals("08", DateParser.getMonthNumber("Aug"));
        assertEquals("09", DateParser.getMonthNumber("Sep"));
        assertEquals("10", DateParser.getMonthNumber("Oct"));
        assertEquals("11", DateParser.getMonthNumber("Nov"));
        assertEquals("12", DateParser.getMonthNumber("Dec"));

    }


}
