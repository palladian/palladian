package ws.palladian.helper.date;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.helper.normalization.DateNormalizer;

public class DateNormalizerTest {

    @Test
    public void testNormalizeDate() {
        assertEquals(DateNormalizer.normalizeDate("17.01.1956"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("17.1.1956"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("17.1.56"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("17/1/56"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("17/01/1956"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("17-01-1956"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("17 January, 1956"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("17 January 1956"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("17 Jan 1956"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("17th January 1956"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("17. January 1956"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("17.Jan '56"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("17 JAN 56"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("January 17,1956"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("January 17,'56"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("January 17th, 1956"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("Jan 17th, 1956"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("1956-01-17"), "1956-01-17");
        assertEquals(DateNormalizer.normalizeDate("January 3, 1956"), "1956-01-03");
        // System.out.println(DateNormalizer.normalizeDate("10 Oct 2008 16:34:01 EST"));
        assertEquals(DateNormalizer.normalizeDate("Mon, 12 Mar 2007 23:13:05 GMT"), "2007-03-12 23:13:05");
        assertEquals(DateNormalizer.normalizeDate("Mon, 13 Oct 2008 01:28:26 GMT"), "2008-10-13 01:28:26");
        // System.out.println(DateNormalizer.normalizeDate("10 Oct 2008 16:34:01 EST"));
        assertEquals(DateNormalizer.normalizeDate("10 Oct 2008 16:34:01 EST"), "2008-10-10 21:34:01");
        // System.out.println(DateNormalizer.normalizeDate("Mon, 27 Oct 2008 19:00 GMT"));
        assertEquals(DateNormalizer.normalizeDate("Mon, 27 Oct 2008 19:00 GMT"), "2008-10-27 19:00:00");
        // System.out.println(normalizeDate("10 Oct 2008 16:34:01 EST"));
        assertEquals(DateNormalizer.normalizeDate("Sun, 23 Nov 2008", true), "2008-11-23 00:00:00");
        // System.out.println(DateNormalizer.normalizeDate("Tue, 16 May 2006 15:04:54 +0900",true));
        assertEquals("2006-05-16 06:04:54", DateNormalizer.normalizeDate("Tue, 16 May 2006 15:04:54 +0900", true));
        assertEquals("2009-02-12 00:56:22", DateNormalizer.normalizeDate("Thu Feb 12 01:56:22 CET 2009", true));
        assertEquals("2009-02-12 00:00:00", DateNormalizer.normalizeDate("2009-02-12 00:00:00", true));
        assertEquals("2009-02-12 00:00:00", DateNormalizer.normalizeDate("2009-02-12", true));
    }
}
