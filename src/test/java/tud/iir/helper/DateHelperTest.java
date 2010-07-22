package tud.iir.helper;

import junit.framework.TestCase;

public class DateHelperTest extends TestCase {

    public DateHelperTest(String name) {
        super(name);
    }

    public void testGetTimeStamp() {

        assertEquals(1273795200000l, DateHelper.getTimestamp("2010-05-14 00:00:00"));
        assertEquals(1273795200000l, DateHelper.getTimestamp("2010-05-14"));

    }

    public void testGetDatetime() {
        assertEquals("20.05.2010", DateHelper.getDatetime("dd.MM.yyyy", 1274313600000l));
    }

    public void testGetTimeString() {
        assertEquals("0ms", DateHelper.getTimeString(0));
        assertEquals("76h:3m:43s:872ms", DateHelper.getTimeString(273823872));
        assertEquals("1m:0s:0ms", DateHelper.getTimeString(60000));
        assertEquals("1h:0m:0s:0ms", DateHelper.getTimeString(3600000));
    }

}