package ws.palladian.helper.date;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DateNormalizerTest {

    @Test
    public void testNormalizeDate() {
        assertEquals("1956-01-17", DateParser.parseDate("17.01.1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("17.1.1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("17.1.56").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("17/1/56").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("17/01/1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("17-01-1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("17 January, 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("17 January 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("17 Jan 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("17th January 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("17. January 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("17.Jan '56").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("17 JAN 56").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("January 17,1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("January 17,'56").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("January 17th, 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("Jan 17th, 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.parseDate("1956-01-17").getNormalizedDateString());
        assertEquals("1956-01-03", DateParser.parseDate("January 3, 1956").getNormalizedDateString());
        // System.out.println(DateParser.parseDate("10 Oct 2008 16:34:01 EST"));
        assertEquals("2007-03-12 23:13:05", DateParser.parseDate("Mon, 12 Mar 2007 23:13:05 GMT")
                .getNormalizedDateString());
        assertEquals("2008-10-13 01:28:26", DateParser.parseDate("Mon, 13 Oct 2008 01:28:26 GMT")
                .getNormalizedDateString());
        // System.out.println(DateParser.parseDate("10 Oct 2008 16:34:01 EST"));
        assertEquals("2008-10-10 21:34:01", DateParser.parseDate("10 Oct 2008 21:34:01 EST").getNormalizedDateString());
        // System.out.println(DateParser.parseDate("Mon, 27 Oct 2008 19:00 GMT"));
        // System.out.println(normalizeDate("10 Oct 2008 16:34:01 EST"));
        // System.out.println(DateParser.parseDate("Tue, 16 May 2006 15:04:54 +0900",true));
        assertEquals("2006-05-16 06:04:54", DateParser.parseDate("Tue, 16 May 2006 15:04:54 +0900")
                .getNormalizedDateString());
        assertEquals("2009-02-12 00:00:00", DateParser.parseDate("2009-02-12 00:00:00").getNormalizedDateString());
        assertEquals("2009-02-12", DateParser.parseDate("2009-02-12").getNormalizedDateString());

        // FIXME the following tests must work
        assertEquals("2009-02-12 00:56:22", DateParser.parseDate("Thu Feb 12 01:56:22 CET 2009")
                .getNormalizedDateString());
        assertEquals("2008-11-23", DateParser.parseDate("Sun, 23 Nov 2008").getNormalizedDateString());
        assertEquals("2008-10-27 19:00:00", DateParser.parseDate("Mon, 27 Oct 2008 19:00 GMT")
                .getNormalizedDateString());
    }
}
