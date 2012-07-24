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


}
