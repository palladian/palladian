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
    public void testMonthNameToNumber() {
        assertEquals(1, DateHelper.monthNameToNumber("Januar"));
        assertEquals(2, DateHelper.monthNameToNumber("Februar"));
        assertEquals(3, DateHelper.monthNameToNumber("März"));
        assertEquals(4, DateHelper.monthNameToNumber("April"));
        assertEquals(5, DateHelper.monthNameToNumber("Mai"));
        assertEquals(6, DateHelper.monthNameToNumber("Juni"));
        assertEquals(7, DateHelper.monthNameToNumber("Juli"));
        assertEquals(8, DateHelper.monthNameToNumber("August"));
        assertEquals(9, DateHelper.monthNameToNumber("September"));
        assertEquals(10, DateHelper.monthNameToNumber("Oktober"));
        assertEquals(11, DateHelper.monthNameToNumber("November"));
        assertEquals(12, DateHelper.monthNameToNumber("Dezember"));

        assertEquals(1, DateHelper.monthNameToNumber("January"));
        assertEquals(2, DateHelper.monthNameToNumber("February"));
        assertEquals(3, DateHelper.monthNameToNumber("March"));
        assertEquals(4, DateHelper.monthNameToNumber("April"));
        assertEquals(5, DateHelper.monthNameToNumber("May"));
        assertEquals(6, DateHelper.monthNameToNumber("June"));
        assertEquals(7, DateHelper.monthNameToNumber("July"));
        assertEquals(8, DateHelper.monthNameToNumber("August"));
        assertEquals(9, DateHelper.monthNameToNumber("September"));
        assertEquals(10, DateHelper.monthNameToNumber("October"));
        assertEquals(11, DateHelper.monthNameToNumber("November"));
        assertEquals(12, DateHelper.monthNameToNumber("December"));

        assertEquals(1, DateHelper.monthNameToNumber("Jan"));
        assertEquals(2, DateHelper.monthNameToNumber("Feb"));
        assertEquals(3, DateHelper.monthNameToNumber("Mär"));
        assertEquals(4, DateHelper.monthNameToNumber("Apr"));
        assertEquals(5, DateHelper.monthNameToNumber("Mai"));
        assertEquals(6, DateHelper.monthNameToNumber("Jun"));
        assertEquals(7, DateHelper.monthNameToNumber("Jul"));
        assertEquals(8, DateHelper.monthNameToNumber("Aug"));
        assertEquals(9, DateHelper.monthNameToNumber("Sep"));
        assertEquals(10, DateHelper.monthNameToNumber("Okt"));
        assertEquals(11, DateHelper.monthNameToNumber("Nov"));
        assertEquals(12, DateHelper.monthNameToNumber("Dez"));

        assertEquals(1, DateHelper.monthNameToNumber("Jan"));
        assertEquals(2, DateHelper.monthNameToNumber("Feb"));
        assertEquals(3, DateHelper.monthNameToNumber("Mar"));
        assertEquals(4, DateHelper.monthNameToNumber("Apr"));
        assertEquals(5, DateHelper.monthNameToNumber("May"));
        assertEquals(6, DateHelper.monthNameToNumber("Jun"));
        assertEquals(7, DateHelper.monthNameToNumber("Jul"));
        assertEquals(8, DateHelper.monthNameToNumber("Aug"));
        assertEquals(9, DateHelper.monthNameToNumber("Sep"));
        assertEquals(10, DateHelper.monthNameToNumber("Oct"));
        assertEquals(11, DateHelper.monthNameToNumber("Nov"));
        assertEquals(12, DateHelper.monthNameToNumber("Dec"));

    }

}