package ws.palladian.helper.date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

/** @formatter:off */
public class DateParserTest {

    /**
     * <p>
     * Test for finding dates with no explicitly given format.
     * </p>
     */
    @Test
    public void testFindDate1() {
        
        // others
        assertEquals("2010-07-02 19:07:49", DateParser.findDate("Tue, 02 Jul 2010 19:07:49 GMT").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateParser.findDate("7/23/2010 3:35:58 PM").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateParser.findDate("23.7.2010 3:35:58 PM").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateParser.findDate("July 23rd, 2010 3:35:58 PM").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateParser.findDate("23. Juli 2010 3:35:58 PM").getNormalizedDateString());
        assertEquals("2010-07-20 00:00:00", DateParser.findDate("Tue, 20 Jul 2010 00:00:00 Z").getNormalizedDateString());
        assertEquals("2010-07-23 16:49:18", DateParser.findDate("Fri, 23 JUL 2010 16:49:18 AEST").getNormalizedDateString());
        assertEquals("2010-07-24", DateParser.findDate("07/24/2010").getNormalizedDateString());
        assertEquals("2010-07-24", DateParser.findDate("Jul 24, 2010 EST").getNormalizedDateString());
        assertEquals("2010-07-25", DateParser.findDate("Sun, 25 Jul 2010").getNormalizedDateString());
        assertEquals("2010-03-07 22:53:50", DateParser.findDate("Sun 7 Mar 2010 10:53:50 PM GMT").getNormalizedDateString());
        assertEquals("2010-08-18 13:20", DateParser.findDate("Wednesday August 18, 2010, 8:20 PM GMT +07:00").getNormalizedDateString());
        assertEquals("2010-08-18 13:20", DateParser.findDate("Wednesday August 18, 2010 8:20 PM GMT +07:00").getNormalizedDateString());
        assertEquals("2010-08-18 13:20", DateParser.findDate("Wednesday August 18, 2010, 8:20 PM +07:00").getNormalizedDateString());
        assertEquals("2010-07-29", DateParser.findDate("29/07/2010").getNormalizedDateString());
        assertEquals("2010-09-07", DateParser.findDate("09/07/2010").getNormalizedDateString());
        assertEquals("2010-08-23", DateParser.findDate("Monday, August 23, 2010").getNormalizedDateString());
        assertEquals("2010-09-23", DateParser.findDate("Monday, Sep 23, 2010").getNormalizedDateString());

        assertEquals("2010-08-03", DateParser.findDate("2010-08-03").getNormalizedDateString());
        assertEquals("2002-08-06 03:08", DateParser.findDate("2002-08-06T03:08BST").getNormalizedDateString());
        assertEquals("2010-06", DateParser.findDate("June 2010").getNormalizedDateString());
        assertEquals("2010-08-31", DateParser.findDate("Aug 31 2010").getNormalizedDateString());
        assertEquals("2009-04-06 15:11", DateParser.findDate("April  6, 2009  3:11 PM").getNormalizedDateString());
        assertEquals("2006-08-04 14:52", DateParser.findDate("aug 4, 2006 / 14:52").getNormalizedDateString());
        assertEquals("2007-08-12", DateParser.findDate("2007-aug-12").getNormalizedDateString());
        assertEquals("2007-08-12", DateParser.findDate("2007-aug.-12").getNormalizedDateString());
        assertEquals("2007-08-12", DateParser.findDate("2007-August-12").getNormalizedDateString());
        assertEquals("2010-07-02", DateParser.findDate("2010/07/02").getNormalizedDateString());
        assertEquals("2007-08-12", DateParser.findDate("August 12, 2007").getNormalizedDateString());
        
        assertEquals("2011-04-18 16:16:00", DateParser.findDate("Mon, 18 Apr 2011 09:16:00 GMT-0700").getNormalizedDateString());
        
        // dates from feeds
        assertEquals("2011-01-28 15:45:15", DateParser.findDate("Fri, 28 Jan 2011 10:45:15 -0500").getNormalizedDateString());
        // assertEquals("2011-02-02 09:36", DateParser.findDate("Wed, 2, Feb 2011 9:36").getNormalizedDateString());
        assertEquals("2011-01-31", DateParser.findDate("2011-01-31").getNormalizedDateString());
        // assertEquals("2011-02-01 11:33:33", DateParser.findDate("Tue, Feb 01,2011 11:33:33PM").getNormalizedDateString());
        // assertEquals("2011-02-02 14:00:00", DateParser.findDate("Wed, 02 Feb 2011 09:00:00 EST").getNormalizedDateString());
        assertEquals("2011-02-01 15:15:56", DateParser.findDate("Tue, 01 February 2011 15:15:56").getNormalizedDateString());
        assertEquals("2011-02-02 10:05", DateParser.findDate("Wed, February 2, 2011 10:05 AM").getNormalizedDateString());
        assertEquals("2011-01-30", DateParser.findDate("1/30/11").getNormalizedDateString());
        assertEquals("2011-04-08 17:33:04", DateParser.findDate("2011-04-08T17:33:04.0026Z").getNormalizedDateString());
        assertEquals("2011-04-08 15:33:04", DateParser.findDate("2011-04-08 15:33:04.0026 GMT").getNormalizedDateString());
        // assertEquals("2010-09-14 21:30:00", DateParser.findDate("Tuesday, 14 Sept 2010 16:30:00 EST").getNormalizedDateString());
        assertEquals("2011-02-02 09:00:01", DateParser.findDate("Wed, 02 Feb 2011 2/2/2011 9:00:01 AM UT").getNormalizedDateString());
        // assertEquals("2011-01-25", DateParser.findDate("Jan 25,2011").getNormalizedDateString());
        assertEquals("2011-02-02 04:03:46", DateParser.findDate("Wednesday, February 02, 2011 4:03:46 AM GMT").getNormalizedDateString());
        assertEquals("2011-02-02 00:00:00", DateParser.findDate("Wed 2 Feb 2011 00:00:00 GMT").getNormalizedDateString());
        assertEquals("2011-01-26 14:25:02", DateParser.findDate("Wed, 26 Jan 2011 09:25:02 -0500").getNormalizedDateString());
        assertEquals("2010-11-30 17:25:47", DateParser.findDate("11/30/2010 5:25:47 PM").getNormalizedDateString());
        assertEquals("2011-01-31 11:48:00", DateParser.findDate("2011-01-31T11:48:00").getNormalizedDateString());
        assertEquals("2011-01-30 06:00:00", DateParser.findDate("Sun, 30 Jan 2011 00:00:00 -0600").getNormalizedDateString());
        assertEquals("2011-02-02 05:00:00", DateParser.findDate("2011-02-02T00:00:00.0000000-05:00").getNormalizedDateString());
        assertEquals("2011-02-02 14:12:43", DateParser.findDate("Wed, 02 Feb 2011 09:12:43 -0500").getNormalizedDateString());
        // assertEquals("2011-02-02 10:00", DateParser.findDate("2011-02-2T10:00").getNormalizedDateString());
        assertEquals("2011-02-02", DateParser.findDate("Feb 2, 2011").getNormalizedDateString());
        
        // old dates from DateNormalizerTest
        assertEquals("1956-01-17",DateParser.findDate("17.01.1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17.1.1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17.1.56").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17/1/56").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17/01/1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17-01-1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17 January, 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17 January 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17 Jan 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17th January 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17. January 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17. Jan '56").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17.Jan '56").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("17 JAN 56").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("January 17, 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("January 17,1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("January 17,'56").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("January 17th, 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("Jan 17th, 1956").getNormalizedDateString());
        assertEquals("1956-01-17", DateParser.findDate("1956-01-17").getNormalizedDateString());
        assertEquals("1956-01-03", DateParser.findDate("January 3, 1956").getNormalizedDateString());
        assertEquals("2007-03-12 23:13:05", DateParser.findDate("Mon, 12 Mar 2007 23:13:05 GMT").getNormalizedDateString());
        assertEquals("2008-10-13 01:28:26", DateParser.findDate("Mon, 13 Oct 2008 01:28:26 GMT").getNormalizedDateString());
        
        // TODO old DateNormalizer was converting TZ
        assertEquals("2008-10-10 16:34:01", DateParser.findDate("10 Oct 2008 16:34:01 EST").getNormalizedDateString());
        assertEquals("2008-10-27 19:00", DateParser.findDate("Mon, 27 Oct 2008 19:00 GMT").getNormalizedDateString());
        assertEquals("2008-10-10 16:34:01", DateParser.findDate("10 Oct 2008 16:34:01 EST").getNormalizedDateString());
        assertEquals("2009-02-12 01:56:22", DateParser.findDate("Thu Feb 12 01:56:22 CET 2009").getNormalizedDateString()); 
        
        assertEquals("2008-11-23", DateParser.findDate("Sun, 23 Nov 2008").getNormalizedDateString());
        assertEquals("2006-05-16 06:04:54", DateParser.findDate("Tue, 16 May 2006 15:04:54 +0900").getNormalizedDateString());
        assertEquals("2009-02-12 00:00:00", DateParser.findDate("2009-02-12 00:00:00").getNormalizedDateString());
        assertEquals("2009-02-12", DateParser.findDate("2009-02-12").getNormalizedDateString());
        assertEquals("2012-12-09 15:45", DateParser.findDate("2012-12-09T15:45GMT").getNormalizedDateString());
        
        // Last updated: 10:09:03 AM GMT(+03) Saturday, 12, November, 2011
        assertEquals("2011-11-12",DateParser.findDate("Saturday, 12, November, 2011").getNormalizedDateString());

    }

    /**
     * <p>
     * Test for finding dates with explicitly given format.
     * </p>
     */
    @Test
    public void testFindDate2() {

        ExtractedDate date;
        String text;

        // ISO8601_YMD_T
        text = "2010-07-02 19:07:49";
        date = DateParser.findDate("2010-07-02T19:07:49", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.findDate("2010-07-02 19:07:49", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.findDate("2010-07-02T21:07:49+02:00", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.findDate("2010-07-02 21:07:49+02:00", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.findDate("2010-07-02T16:37:49-02:30", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.findDate("2010-12-31 22:37-02:30", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2011-01-01 01:07", date.getNormalizedDateString());
        date = DateParser.findDate("2010-07-02T19", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19", date.getNormalizedDateString());
        date = DateParser.findDate("2010-07-02 19:07", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19:07", date.getNormalizedDateString());
        date = DateParser.findDate("2010-07-02 19:07Z", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19:07", date.getNormalizedDateString());
        date = DateParser.findDate("2010-07-02 19:07:49Z", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.findDate("2010-07-02 19:07:49.123", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.findDate("2010/07/02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.findDate("2010/07/02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());
        date = DateParser.findDate("2010.07.02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.findDate("2010.07.02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());
        date = DateParser.findDate("2010_07_02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.findDate("2010_07_02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());
        date = DateParser.findDate("2010/07/02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());

        // ISO_YMD
        text = "2010-06-05";
        date = DateParser.findDate(text, RegExp.DATE_ISO8601_YMD);
        assertEquals(text, date.getNormalizedDateString());

        // ISO_YM
        text = "2010-06";
        date = DateParser.findDate(text, RegExp.DATE_ISO8601_YM);
        assertEquals(text, date.getNormalizedDateString());

        // ISO8601_YWD
        text = "2010-07-22";
        date = DateParser.findDate("2010-W29-5", RegExp.DATE_ISO8601_YWD);
        assertEquals(text, date.getNormalizedDateString());

        // ISO8601_YWD_T
        text = "2010-07-22 19:07:49";
        date = DateParser.findDate("2010-W29-5T19:07:49.123", RegExp.DATE_ISO8601_YWD_T);
        assertEquals(text, date.getNormalizedDateString());

        // ISO8601_YW
        text = "2010-07";
        date = DateParser.findDate("2010-W29", RegExp.DATE_ISO8601_YW);
        assertEquals(text, date.getNormalizedDateString());

        // ISO8601_YD
        text = "2010-07-22";
        date = DateParser.findDate("2010-203", RegExp.DATE_ISO8601_YD);
        assertEquals(text, date.getNormalizedDateString());

        // URL_D
        text = "2010-06-30";
        date = DateParser.findDate("2010.06.30", RegExp.DATE_URL_D);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("2010_06_30", RegExp.DATE_URL_D);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("2010/06/30/", RegExp.DATE_URL_D);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("2010/June/30/", RegExp.DATE_URL_MMMM_D);
        assertEquals(text, date.getNormalizedDateString());

        // URL
        text = "2010-06";
        date = DateParser.findDate("2010.06", RegExp.DATE_URL);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("2010_06", RegExp.DATE_URL);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("2010/06", RegExp.DATE_URL);
        assertEquals(text, date.getNormalizedDateString());

        // EU & USA
        text = "2010-07-25";
        date = DateParser.findDate("25.07.2010", RegExp.DATE_EU_D_MM_Y);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("07/25/2010", RegExp.DATE_USA_MM_D_Y);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("25. Juli 2010", RegExp.DATE_EU_D_MMMM_Y);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("July 25, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("July 25th, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(text, date.getNormalizedDateString());
        text = "2010-07-05";
        date = DateParser.findDate("5.07.2010", RegExp.DATE_EU_D_MM_Y);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("7/5/2010", RegExp.DATE_USA_MM_D_Y);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("5. Juli 2010", RegExp.DATE_EU_D_MMMM_Y);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("July 5, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("July 5th, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(text, date.getNormalizedDateString());
        text = "2010-07";
        date = DateParser.findDate("July 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("Juli 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("07.2010", RegExp.DATE_EU_MM_Y);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("June 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals("2010-06", date.getNormalizedDateString());
        text = "0-07-25";
        date = DateParser.findDate("25.07.", RegExp.DATE_EU_D_MM);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("25. Juli", RegExp.DATE_EU_D_MMMM);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("07/25", RegExp.DATE_USA_MM_D);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("July 25th", RegExp.DATE_USA_MMMM_D);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("July 25", RegExp.DATE_USA_MMMM_D);
        assertEquals(text, date.getNormalizedDateString());
        text = "2010-07";
        date = DateParser.findDate("07/2010", RegExp.DATE_USA_MM_Y);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("09/8/10 04:56 PM", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals("2010-09-08 16:56", date.getNormalizedDateString());
        date = DateParser.findDate("August 10, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals("2010-08-10", date.getNormalizedDateString());

        // DATE_RFC & ANSI C
        text = "2010-07-02 19:07:49";
        date = DateParser.findDate("Tue Jul 2 19:07:49 2010", RegExp.DATE_ANSI_C);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("Tue Jul 2 15:37:49 2010 -03:30", RegExp.DATE_ANSI_C_TZ);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("Tue, 02 Jul 2010 19:07:49 GMT", RegExp.DATE_RFC_1123);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("Tuesday, 02-Jul-10 19:07:49 GMT", RegExp.DATE_RFC_1036);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("Wed, 08 Sep 2010 08:09:15 EST", RegExp.DATE_RFC_1123);
        assertEquals("2010-09-08 08:09:15", date.getNormalizedDateString());

        // ISO without separator
        text = "2010-07-25";
        date = DateParser.findDate("20100725", RegExp.DATE_ISO8601_YMD_NO);
        assertEquals(text, date.getNormalizedDateString());
        text = "2010-07-22";
        date = DateParser.findDate("2010W295", RegExp.DATE_ISO8601_YWD_NO);
        assertEquals(text, date.getNormalizedDateString());
        text = "2010-07";
        date = DateParser.findDate("2010W29", RegExp.DATE_ISO8601_YW_NO);
        assertEquals(text, date.getNormalizedDateString());
        text = "2010-07-22";
        date = DateParser.findDate("2010203", RegExp.DATE_ISO8601_YD_NO);
        assertEquals(text, date.getNormalizedDateString());

        // RFC + UTC
        text = "2010-07-02 19:07:49";
        date = DateParser.findDate("Tue, 02 Jul 2010 20:07:49 +0100", RegExp.DATE_RFC_1123_UTC);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("Tuesday, 02-Jul-10 18:07:49 -0100", RegExp.DATE_RFC_1036_UTC);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("Tue, 02 Jul 2010 20:07:49 +01:00", RegExp.DATE_RFC_1123_UTC);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("Tuesday, 02-Jul-10 18:07:49 -01:00", RegExp.DATE_RFC_1036_UTC);
        assertEquals(text, date.getNormalizedDateString());

        // EU & USA time

        text = "2010-07-02 19:07:49";
        date = DateParser.findDate("02.07.2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("02-07-2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("02/07/2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("02_07_2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("02. Juli 2010 20:07:49 +0100", RegExp.DATE_EU_D_MMMM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("07/02/2010 20:07:49 +0100", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("July 02nd, 2010 20:07:49 +0100", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.findDate("04.08.2006 / 14:52", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.findDate("08/04/2006 / 14:52", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.findDate("04 August 2006 / 14:52", RegExp.DATE_EU_D_MMMM_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.findDate("aug 4, 2006 / 14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.findDate("aug 4, 2006 14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.findDate("aug 4, 2006  14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.findDate("Saturday, September 20. 2008", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals("2008-09-20", date.getNormalizedDateString());
        date = DateParser.findDate("11-12-2010 19:48:00", RegExp.DATE_USA_MM_D_Y_T_SEPARATOR);
        assertEquals("2010-11-12 19:48:00", date.getNormalizedDateString());
        
        // Year
        assertEquals("2012", DateParser.findDate("it happened in 2012", RegExp.DATE_CONTEXT_YYYY).getNormalizedDateString());
        
        // date with line break
        assertEquals("2006-02-06", DateParser.findDate("06. Feb\n06", RegExp.DATE_EU_D_MMMM_Y).getNormalizedDateString());
    }
    
    @Test
    public void testFindDates() {
        assertEquals("2010-01", DateParser.findDates("Januar 2010").get(0).getNormalizedDateString());
        assertEquals("2010-02", DateParser.findDates("Februar 2010").get(0).getNormalizedDateString());
        assertEquals("2010-03", DateParser.findDates("März 2010").get(0).getNormalizedDateString());
        assertEquals("2010-04", DateParser.findDates("April 2010").get(0).getNormalizedDateString());
        assertEquals("2010-05", DateParser.findDates("Mai 2010").get(0).getNormalizedDateString());
        assertEquals("2010-06", DateParser.findDates("Juni 2010").get(0).getNormalizedDateString());
        assertEquals("2010-07", DateParser.findDates("Juli 2010").get(0).getNormalizedDateString());
        assertEquals("2010-08", DateParser.findDates("August 2010").get(0).getNormalizedDateString());
        assertEquals("MMMM YYYY", DateParser.findDates("August 2010").get(0).getFormat());
        assertEquals("2010-09", DateParser.findDates("September 2010").get(0).getNormalizedDateString());
        assertEquals("2010-10", DateParser.findDates("Oktober 2010").get(0).getNormalizedDateString());
        assertEquals("2010-11", DateParser.findDates("November 2010").get(0).getNormalizedDateString());
        assertEquals("2010-12", DateParser.findDates("Dezember 2010").get(0).getNormalizedDateString());
        String date = DateParser.findDates("SEPTEMBER 1, 2010").get(0).getNormalizedDateString();
        assertEquals("2010-09-01", date);
        date = DateParser.findDates(", 17/09/06 03:51:53").get(0).getNormalizedDateString();
        assertEquals("2006-09-17 03:51:53", date);
        date = DateParser.findDates("30.09.2010").get(0).getNormalizedDateString();
        assertEquals("2010-09-30", date);
        date = DateParser.findDates(", 08. Februar 2010, 17:15").get(0).getNormalizedDateString();
        assertEquals("2010-02-08", date);
        date = DateParser.findDates("Sept. 3, 2010").get(0).getNormalizedDateString();
        assertEquals("2010-09-03", date);
        date = DateParser.findDates("Last Modified: Wednesday, 11-Aug-2010 14:41:10 EDT").get(0)
                .getNormalizedDateString();
        assertEquals("2010-08-11 14:41:10", date);
        date = DateParser.findDates("JUNE 1, 2010").get(0).getNormalizedDateString();
        assertEquals("2010-06-01", date);
        assertEquals("2010-01", DateParser.findDates("jan. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-02", DateParser.findDates("Feb. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-03", DateParser.findDates("Mär. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-04", DateParser.findDates("Apr. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-05", DateParser.findDates("Mai. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-06", DateParser.findDates("Jun. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-07", DateParser.findDates("Jul. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-08", DateParser.findDates("Aug. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-09", DateParser.findDates("Sep. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-10", DateParser.findDates("Okt. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-11", DateParser.findDates("nov. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-12", DateParser.findDates("Dez. 2010").get(0).getNormalizedDateString());
        assertEquals("2007-12-06 17:37:45", DateParser.findDates("2007-12-06T17:37:45Z").get(0)
                .getNormalizedDateString());

        List<ExtractedDate> dates = DateParser.findDates("2007-12-06T17:37:45Z 2008-12-06T17:37:45Z");
        assertEquals(2, dates.size());
        assertEquals("2007-12-06 17:37:45", dates.get(0).getNormalizedDateString());
        assertEquals("2008-12-06 17:37:45", dates.get(1).getNormalizedDateString());

        dates = DateParser.findDates("Saturday, September 20, 2008");
        assertEquals(1, dates.size());
        assertEquals("2008-09-20", dates.get(0).getNormalizedDateString());

        dates = DateParser.findDates("Mon, 18 Apr 2011 09:16:00 GMT-0700");
        assertEquals(1, dates.size());
        assertEquals("2011-04-18 16:16:00", dates.get(0).getNormalizedDateString());

        dates = DateParser.findDates("Dienstag, 03. Mai 2011 um 05:13");
        assertEquals(1, dates.size());
        assertEquals("2011-05-03", dates.get(0).getNormalizedDateString());
    }

    @Test
    public void testFindRelativeDate() {
        long currentTime = 1291201200000l;
        ExtractedDate relDate = DateParser.findRelativeDate("5 days ago", currentTime);
        assertEquals("2010-11-26", relDate.getNormalizedDateString(false));
        relDate = DateParser.findRelativeDate("114 days ago", currentTime);
        assertEquals("2010-08-09", relDate.getNormalizedDateString(false));
        relDate = DateParser.findRelativeDate("4 month ago", currentTime);
        assertEquals("2010-08-03", relDate.getNormalizedDateString(false));
        relDate = DateParser.findRelativeDate("12 month ago", currentTime);
        assertEquals("2009-12-06", relDate.getNormalizedDateString(false));
        relDate = DateParser.findRelativeDate("1 year ago", currentTime);
        assertEquals("2009-12-01", relDate.getNormalizedDateString(false));
        relDate = DateParser.findRelativeDate("11 years ago", currentTime);
        assertEquals("1999-12-04", relDate.getNormalizedDateString(false));
        relDate = DateParser.findRelativeDate("1 minute ago", currentTime);
        assertEquals("2010-12-01", relDate.getNormalizedDateString(false));
        // TODO relDate = DateParser.findRelativeDate("yesterday", currentTime);
    }

    @Test
    public void testNormalizeYear() {
        assertEquals(1999, DateParserLogic.normalizeYear("'99"));
        assertEquals(2003, DateParserLogic.normalizeYear("'03"));
        assertEquals(2010, DateParserLogic.normalizeYear("'10"));
        assertEquals(1915, DateParserLogic.normalizeYear("'15"));
        assertEquals(1915, DateParserLogic.normalizeYear("15"));
        assertEquals(1915, DateParserLogic.normalizeYear("1915"));
        assertEquals(2012, DateParserLogic.normalizeYear("2012\n1"));
    }

    @Test
    public void testRemoveNoDigits() {
        assertEquals("23", DateParserLogic.removeNoDigits("23."));
        assertEquals("23", DateParserLogic.removeNoDigits("'23."));
        assertEquals("23", DateParserLogic.removeNoDigits("23,"));
        assertEquals("21", DateParserLogic.removeNoDigits("21st"));
        assertEquals("22", DateParserLogic.removeNoDigits("22nd"));
        assertEquals("23", DateParserLogic.removeNoDigits("23rd"));
        assertEquals("24", DateParserLogic.removeNoDigits("24th"));
    }

    @Test
    public void testGet4DigitYear() {
        assertEquals(1920, DateParserLogic.get4DigitYear(20));
        assertEquals(2007, DateParserLogic.get4DigitYear(7));
        assertEquals(2010, DateParserLogic.get4DigitYear(10));
        assertEquals(1999, DateParserLogic.get4DigitYear(99));
    }

    @Test
    public void testSplitTimeZone() {
        String[] result = DateParserLogic.splitTimeZone("22:10  UTC");
        assertEquals("22:10 ", result[0]);
        assertEquals("UTC", result[1]);
        
        result = DateParserLogic.splitTimeZone("22:10 UTC");
        assertEquals("22:10 ", result[0]);
        assertEquals("UTC", result[1]);
        
        result = DateParserLogic.splitTimeZone("22:10 GMT");
        assertEquals("22:10 ", result[0]);
        assertEquals("GMT", result[1]);
        
        result = DateParserLogic.splitTimeZone("Wed, 02 Feb 2011 09:00:00 EST");
        assertEquals("Wed, 02 Feb 2011 09:00:00 ", result[0]);
        assertEquals("EST", result[1]);
        
        result = DateParserLogic.splitTimeZone("22:10");
        assertNull(result);
    }

    @Test
    public void testGetSeparator() {
        assertEquals("\\.", DateParserLogic.getSeparatorRegEx("10.10.2010"));
        assertEquals("-", DateParserLogic.getSeparatorRegEx("10-10-2010"));
        assertEquals("_", DateParserLogic.getSeparatorRegEx("10_10_2010"));
        assertEquals("/", DateParserLogic.getSeparatorRegEx("10/10/2010"));
        assertEquals("-", DateParserLogic.getSeparatorRegEx("2010-05-06"));
        assertEquals("_", DateParserLogic.getSeparatorRegEx("2010_05_06"));
        assertEquals("\\.", DateParserLogic.getSeparatorRegEx("2010.05.06"));
        assertEquals("/", DateParserLogic.getSeparatorRegEx("2010/05/06"));
    }
    
    @Test
    public void testSetTimeDiff() {
        DateParserLogic dateParserLogic = new DateParserLogic();
        dateParserLogic.year = 2010;
        dateParserLogic.month = 10;
        dateParserLogic.day = 10;
        dateParserLogic.hour = 12;
        dateParserLogic.minute = 30;
        dateParserLogic.setTimeDiff("06:30", "-");
        assertEquals(dateParserLogic.hour, 19);
        assertEquals(dateParserLogic.minute, 0);
    }
    
    @Test
    @Ignore // make this faster!
    public void testExtractFromText() throws FileNotFoundException {
        final int count = 25;
        final StopWatch stopWatch = new StopWatch();
        String text = FileHelper.readFileToString(ResourceHelper.getResourcePath("/wikipedia_2011_Egyptian_revolution.txt"));
        for (int i = 0; i < count; i++) {
            ProgressHelper.printProgress(i, count, 1, stopWatch);
            DateParser.findDates(text);
        }
        DateParser.printHallOfShame();
    }

}
