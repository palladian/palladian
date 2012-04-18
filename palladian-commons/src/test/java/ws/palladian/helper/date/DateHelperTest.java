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

}