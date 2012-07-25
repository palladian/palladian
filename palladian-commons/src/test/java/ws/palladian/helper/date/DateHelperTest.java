package ws.palladian.helper.date;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DateHelperTest {

    @Test
    public void testGetTimeStamp() throws Exception {
        assertEquals(1273795200000l, DateHelper.getTimestamp("2010-05-14 00:00:00"));
        assertEquals(1273795200000l, DateHelper.getTimestamp("2010-05-14"));
    }

    @Test
    public void testGetDatetime() {
        assertEquals("20.05.2010", DateHelper.getDatetime("dd.MM.yyyy", 1274313600000l));
    }

    @Test
    public void testGetTimeString() {
        assertEquals("0ms", DateHelper.getTimeString(0));
        assertEquals("3d:4h:3m:43s:872ms", DateHelper.getTimeString(273823872));
        assertEquals("1m:0s:0ms", DateHelper.getTimeString(60000));
        assertEquals("1h:0m:0s:0ms", DateHelper.getTimeString(3600000));
    }

    @Test
    public void testmonthNameToNumber() {
        assertEquals("01", DateHelper.monthNameToNumber("Januar"));
        assertEquals("02", DateHelper.monthNameToNumber("Februar"));
        assertEquals("03", DateHelper.monthNameToNumber("März"));
        assertEquals("04", DateHelper.monthNameToNumber("April"));
        assertEquals("05", DateHelper.monthNameToNumber("Mai"));
        assertEquals("06", DateHelper.monthNameToNumber("Juni"));
        assertEquals("07", DateHelper.monthNameToNumber("Juli"));
        assertEquals("08", DateHelper.monthNameToNumber("August"));
        assertEquals("09", DateHelper.monthNameToNumber("September"));
        assertEquals("10", DateHelper.monthNameToNumber("Oktober"));
        assertEquals("11", DateHelper.monthNameToNumber("November"));
        assertEquals("12", DateHelper.monthNameToNumber("Dezember"));

        assertEquals("01", DateHelper.monthNameToNumber("January"));
        assertEquals("02", DateHelper.monthNameToNumber("February"));
        assertEquals("03", DateHelper.monthNameToNumber("March"));
        assertEquals("04", DateHelper.monthNameToNumber("April"));
        assertEquals("05", DateHelper.monthNameToNumber("May"));
        assertEquals("06", DateHelper.monthNameToNumber("June"));
        assertEquals("07", DateHelper.monthNameToNumber("July"));
        assertEquals("08", DateHelper.monthNameToNumber("August"));
        assertEquals("09", DateHelper.monthNameToNumber("September"));
        assertEquals("10", DateHelper.monthNameToNumber("October"));
        assertEquals("11", DateHelper.monthNameToNumber("November"));
        assertEquals("12", DateHelper.monthNameToNumber("December"));

        assertEquals("01", DateHelper.monthNameToNumber("Jan"));
        assertEquals("02", DateHelper.monthNameToNumber("Feb"));
        assertEquals("03", DateHelper.monthNameToNumber("Mär"));
        assertEquals("04", DateHelper.monthNameToNumber("Apr"));
        assertEquals("05", DateHelper.monthNameToNumber("Mai"));
        assertEquals("06", DateHelper.monthNameToNumber("Jun"));
        assertEquals("07", DateHelper.monthNameToNumber("Jul"));
        assertEquals("08", DateHelper.monthNameToNumber("Aug"));
        assertEquals("09", DateHelper.monthNameToNumber("Sep"));
        assertEquals("10", DateHelper.monthNameToNumber("Okt"));
        assertEquals("11", DateHelper.monthNameToNumber("Nov"));
        assertEquals("12", DateHelper.monthNameToNumber("Dez"));

        assertEquals("01", DateHelper.monthNameToNumber("Jan"));
        assertEquals("02", DateHelper.monthNameToNumber("Feb"));
        assertEquals("03", DateHelper.monthNameToNumber("Mar"));
        assertEquals("04", DateHelper.monthNameToNumber("Apr"));
        assertEquals("05", DateHelper.monthNameToNumber("May"));
        assertEquals("06", DateHelper.monthNameToNumber("Jun"));
        assertEquals("07", DateHelper.monthNameToNumber("Jul"));
        assertEquals("08", DateHelper.monthNameToNumber("Aug"));
        assertEquals("09", DateHelper.monthNameToNumber("Sep"));
        assertEquals("10", DateHelper.monthNameToNumber("Oct"));
        assertEquals("11", DateHelper.monthNameToNumber("Nov"));
        assertEquals("12", DateHelper.monthNameToNumber("Dec"));

    }

}