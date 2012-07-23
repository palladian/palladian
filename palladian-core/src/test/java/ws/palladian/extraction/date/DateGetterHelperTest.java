package ws.palladian.extraction.date;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.control.AllTests;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.date.ExtractedDateHelper;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.io.ResourceHelper;

public class DateGetterHelperTest {


    @Test
    public void testGetDateFromString() {

        ExtractedDate date;
        String text;

        // ISO8601_YMD_T
        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("2010-07-02T19:07:49", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07:49", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02T21:07:49+02:00", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02 21:07:49+02:00", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02T16:37:49-02:30", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-12-31 22:37-02:30", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2011-01-01 01:07", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02T19", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19:07", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07Z", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19:07", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07:49Z", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07:49.123", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010/07/02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010/07/02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010.07.02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010.07.02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010_07_02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010_07_02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010/07/02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());

        // ISO_YMD
        text = "2010-06-05";
        date = DateGetterHelper.getDateFromString(text, RegExp.DATE_ISO8601_YMD);
        assertEquals(text, date.getNormalizedDateString());

        // ISO_YM
        text = "2010-06";
        date = DateGetterHelper.getDateFromString(text, RegExp.DATE_ISO8601_YM);
        assertEquals(text, date.getNormalizedDateString());

        // ISO8601_YWD
        text = "2010-07-22";
        date = DateGetterHelper.getDateFromString("2010-W29-5", RegExp.DATE_ISO8601_YWD);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // ISO8601_YWD_T
        text = "2010-07-22 19:07:49";
        date = DateGetterHelper.getDateFromString("2010-W29-5T19:07:49.123", RegExp.DATE_ISO8601_YWD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // ISO8601_YW
        text = "2010-07";
        date = DateGetterHelper.getDateFromString("2010-W29", RegExp.DATE_ISO8601_YW);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // ISO8601_YD
        text = "2010-07-22";
        date = DateGetterHelper.getDateFromString("2010-203", RegExp.DATE_ISO8601_YD);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // URL_D
        text = "2010-06-30";
        date = DateGetterHelper.getDateFromString("2010.06.30", RegExp.DATE_URL_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010_06_30", RegExp.DATE_URL_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010/06/30/", RegExp.DATE_URL_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010/June/30/", RegExp.DATE_URL_MMMM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // URL
        text = "2010-06";
        date = DateGetterHelper.getDateFromString("2010.06", RegExp.DATE_URL);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010_06", RegExp.DATE_URL);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010/06", RegExp.DATE_URL);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // EU & USA
        text = "2010-07-25";
        date = DateGetterHelper.getDateFromString("25.07.2010", RegExp.DATE_EU_D_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("07/25/2010", RegExp.DATE_USA_MM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("25. Juli 2010", RegExp.DATE_EU_D_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 25, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 25th, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07-05";
        date = DateGetterHelper.getDateFromString("5.07.2010", RegExp.DATE_EU_D_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("7/5/2010", RegExp.DATE_USA_MM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("5. Juli 2010", RegExp.DATE_EU_D_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 5, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 5th, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07";
        date = DateGetterHelper.getDateFromString("July 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Juli 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("07.2010", RegExp.DATE_EU_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("June 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(date.getDateString(), "2010-06", date.getNormalizedDateString());
        text = "0-07-25";
        date = DateGetterHelper.getDateFromString("25.07.", RegExp.DATE_EU_D_MM);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("25. Juli", RegExp.DATE_EU_D_MMMM);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("07/25", RegExp.DATE_USA_MM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 25th", RegExp.DATE_USA_MMMM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 25", RegExp.DATE_USA_MMMM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07";
        date = DateGetterHelper.getDateFromString("07/2010", RegExp.DATE_USA_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("09/8/10 04:56 PM", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals(date.getDateString(), "2010-09-08 16:56", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("August 10, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), "2010-08-10", date.getNormalizedDateString());

        // DATE_RFC & ANSI C
        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("Tue Jul 2 19:07:49 2010", RegExp.DATE_ANSI_C);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Tue Jul 2 15:37:49 2010 -03:30", RegExp.DATE_ANSI_C_TZ);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Tue, 02 Jul 2010 19:07:49 GMT", RegExp.DATE_RFC_1123);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Tuesday, 02-Jul-10 19:07:49 GMT", RegExp.DATE_RFC_1036);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Wed, 08 Sep 2010 08:09:15 EST", RegExp.DATE_RFC_1123);
        assertEquals("2010-09-08 08:09:15", date.getNormalizedDateString());

        // ISO without separator
        text = "2010-07-25";
        date = DateGetterHelper.getDateFromString("20100725", RegExp.DATE_ISO8601_YMD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07-22";
        date = DateGetterHelper.getDateFromString("2010W295", RegExp.DATE_ISO8601_YWD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07";
        date = DateGetterHelper.getDateFromString("2010W29", RegExp.DATE_ISO8601_YW_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07-22";
        date = DateGetterHelper.getDateFromString("2010203", RegExp.DATE_ISO8601_YD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // RFC + UTC
        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("Tue, 02 Jul 2010 20:07:49 +0100", RegExp.DATE_RFC_1123_UTC);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Tuesday, 02-Jul-10 18:07:49 -0100", RegExp.DATE_RFC_1036_UTC);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Tue, 02 Jul 2010 20:07:49 +01:00", RegExp.DATE_RFC_1123_UTC);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Tuesday, 02-Jul-10 18:07:49 -01:00", RegExp.DATE_RFC_1036_UTC);
        assertEquals(text, date.getNormalizedDateString());

        // EU & USA time

        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("02.07.2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("02-07-2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("02/07/2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("02_07_2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("02. Juli 2010 20:07:49 +0100", RegExp.DATE_EU_D_MMMM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("07/02/2010 20:07:49 +0100", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 02nd, 2010 20:07:49 +0100", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("04.08.2006 / 14:52", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("08/04/2006 / 14:52", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("04 August 2006 / 14:52", RegExp.DATE_EU_D_MMMM_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("aug 4, 2006 / 14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("aug 4, 2006 14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("aug 4, 2006  14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Saturday, September 20. 2008", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals("2008-09-20", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("11-12-2010 19:48:00", RegExp.DATE_USA_MM_D_Y_T_SEPARATOR);
        assertEquals("2010-11-12 19:48:00", date.getNormalizedDateString());

        // others
        text = "2010-07-02 19:07:49";
        assertEquals(text, DateGetterHelper.findDate("Tue, 02 Jul 2010 19:07:49 GMT").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateGetterHelper.findDate("7/23/2010 3:35:58 PM").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateGetterHelper.findDate("23.7.2010 3:35:58 PM").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateGetterHelper.findDate("July 23rd, 2010 3:35:58 PM")
                .getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateGetterHelper.findDate("23. Juli 2010 3:35:58 PM")
                .getNormalizedDateString());
        assertEquals("2010-07-20 00:00:00", DateGetterHelper.findDate("Tue, 20 Jul 2010 00:00:00 Z")
                .getNormalizedDateString());
        assertEquals("2010-07-23 16:49:18", DateGetterHelper.findDate("Fri, 23 JUL 2010 16:49:18 AEST")
                .getNormalizedDateString());
        assertEquals("2010-07-24", DateGetterHelper.findDate("07/24/2010").getNormalizedDateString());
        assertEquals("2010-07-24", DateGetterHelper.findDate("Jul 24, 2010 EST").getNormalizedDateString());
        assertEquals("2010-07-25", DateGetterHelper.findDate("Sun, 25 Jul 2010").getNormalizedDateString());
        assertEquals("2010-03-07 22:53:50", DateGetterHelper.findDate("Sun 7 Mar 2010 10:53:50 PM GMT")
                .getNormalizedDateString());
        assertEquals("2010-08-18 13:20", DateGetterHelper.findDate("Wednesday August 18, 2010, 8:20 PM GMT +07:00")
                .getNormalizedDateString());
        assertEquals("2010-08-18 13:20", DateGetterHelper.findDate("Wednesday August 18, 2010 8:20 PM GMT +07:00")
                .getNormalizedDateString());
        assertEquals("2010-08-18 13:20", DateGetterHelper.findDate("Wednesday August 18, 2010, 8:20 PM +07:00")
                .getNormalizedDateString());
        assertEquals("2010-07-29", DateGetterHelper.findDate("29/07/2010").getNormalizedDateString());
        assertEquals("2010-09-07", DateGetterHelper.findDate("09/07/2010").getNormalizedDateString());
        assertEquals("2010-08-23", DateGetterHelper.findDate("Monday, August 23, 2010").getNormalizedDateString());
        assertEquals("2010-09-23", DateGetterHelper.findDate("Monday, Sep 23, 2010").getNormalizedDateString());

    }

    @Test
    public void testFindAllDates() {
        assertEquals("2010-01", DateGetterHelper.findAllDates("Januar 2010").get(0).getNormalizedDateString());
        assertEquals("2010-02", DateGetterHelper.findAllDates("Februar 2010").get(0).getNormalizedDateString());
        assertEquals("2010-03", DateGetterHelper.findAllDates("März 2010").get(0).getNormalizedDateString());
        assertEquals("2010-04", DateGetterHelper.findAllDates("April 2010").get(0).getNormalizedDateString());
        assertEquals("2010-05", DateGetterHelper.findAllDates("Mai 2010").get(0).getNormalizedDateString());
        assertEquals("2010-06", DateGetterHelper.findAllDates("Juni 2010").get(0).getNormalizedDateString());
        assertEquals("2010-07", DateGetterHelper.findAllDates("Juli 2010").get(0).getNormalizedDateString());
        assertEquals("2010-08", DateGetterHelper.findAllDates("August 2010").get(0).getNormalizedDateString());
        assertEquals("MMMM YYYY", DateGetterHelper.findAllDates("August 2010").get(0).getFormat());
        assertEquals("2010-09", DateGetterHelper.findAllDates("September 2010").get(0).getNormalizedDateString());
        assertEquals("2010-10", DateGetterHelper.findAllDates("Oktober 2010").get(0).getNormalizedDateString());
        assertEquals("2010-11", DateGetterHelper.findAllDates("November 2010").get(0).getNormalizedDateString());
        assertEquals("2010-12", DateGetterHelper.findAllDates("Dezember 2010").get(0).getNormalizedDateString());
        String date = DateGetterHelper.findAllDates("SEPTEMBER 1, 2010").get(0).getNormalizedDateString();
        assertEquals("2010-09-01", date);
        date = DateGetterHelper.findAllDates(", 17/09/06 03:51:53").get(0).getNormalizedDateString();
        assertEquals("2006-09-17 03:51:53", date);
        date = DateGetterHelper.findAllDates("30.09.2010").get(0).getNormalizedDateString();
        assertEquals("2010-09-30", date);
        date = DateGetterHelper.findAllDates(", 08. Februar 2010, 17:15").get(0).getNormalizedDateString();
        assertEquals("2010-02-08", date);
        date = DateGetterHelper.findAllDates("Sept. 3, 2010").get(0).getNormalizedDateString();
        assertEquals("2010-09-03", date);
        date = DateGetterHelper.findAllDates("Last Modified: Wednesday, 11-Aug-2010 14:41:10 EDT").get(0)
                .getNormalizedDateString();
        assertEquals("2010-08-11 14:41:10", date);
        date = DateGetterHelper.findAllDates("JUNE 1, 2010").get(0).getNormalizedDateString();
        assertEquals("2010-06-01", date);
        assertEquals("2010-01", DateGetterHelper.findAllDates("jan. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-02", DateGetterHelper.findAllDates("Feb. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-03", DateGetterHelper.findAllDates("Mär. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-04", DateGetterHelper.findAllDates("Apr. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-05", DateGetterHelper.findAllDates("Mai. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-06", DateGetterHelper.findAllDates("Jun. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-07", DateGetterHelper.findAllDates("Jul. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-08", DateGetterHelper.findAllDates("Aug. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-09", DateGetterHelper.findAllDates("Sep. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-10", DateGetterHelper.findAllDates("Okt. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-11", DateGetterHelper.findAllDates("nov. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-12", DateGetterHelper.findAllDates("Dez. 2010").get(0).getNormalizedDateString());
        assertEquals("2007-12-06 17:37:45", DateGetterHelper.findAllDates("2007-12-06T17:37:45Z").get(0)
                .getNormalizedDateString());
        assertEquals("2008-12-06 17:37:45",
                DateGetterHelper.findAllDates("2007-12-06T17:37:45Z 2008-12-06T17:37:45Z").get(1)
                        .getNormalizedDateString());
        assertEquals("2008-09-20", DateGetterHelper.findAllDates("Saturday, September 20, 2008").get(0)
                .getNormalizedDateString());
        assertEquals("2011-04-18 16:16:00", DateGetterHelper.findDate("Mon, 18 Apr 2011 09:16:00 GMT-0700").getNormalizedDateString());
        assertEquals("2011-05-03", (DateGetterHelper.findAllDates("Dienstag, 03. Mai 2011 um 05:13")).get(0)
                .getNormalizedDateString());
    }

    @Test
    public void testFindDate() {
        String text = "2010-08-03";
        assertEquals(text, DateGetterHelper.findDate("2010-08-03").getNormalizedDateString());
        assertEquals("2002-08-06 03:08", DateGetterHelper.findDate("2002-08-06T03:08BST").getNormalizedDateString());
        assertEquals("2010-06", DateGetterHelper.findDate("June 2010").getNormalizedDateString());
        assertEquals("2010-08-31", DateGetterHelper.findDate("Aug 31 2010").getNormalizedDateString());
        assertEquals("2009-04-06 15:11",
                DateGetterHelper.findDate("April  6, 2009  3:11 PM").getNormalizedDateString());
        ExtractedDate date = DateGetterHelper.findDate("aug 4, 2006 / 14:52");
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.findDate("2007-aug-12");
        assertEquals("2007-08-12", date.getNormalizedDateString());
        date = DateGetterHelper.findDate("2007-aug.-12");
        assertEquals("2007-08-12", date.getNormalizedDateString());
        date = DateGetterHelper.findDate("2007-August-12");
        assertEquals("2007-08-12", date.getNormalizedDateString());
        date = DateGetterHelper.findDate("2010/07/02");
        assertEquals("2010-07-02", date.getNormalizedDateString());

    }

    @Test
    public void testGetSeparator() {
        assertEquals("-", ExtractedDateHelper.getSeparator("2010-05-06"));
        assertEquals("_", ExtractedDateHelper.getSeparator("2010_05_06"));
        assertEquals("\\.", ExtractedDateHelper.getSeparator("2010.05.06"));
        assertEquals("/", ExtractedDateHelper.getSeparator("2010/05/06"));
    }






    @Test
    public void testGetContentDates() {
        if (AllTests.ALL_TESTS) {
            // final String url = "data/test/webPages/dateExtraction/kullin.htm";
            // String url =
            // "http://www.gatorsports.com/article/20100823/ARTICLES/100829802/1136?Title=Meyer-has-concerns-with-season-fast-approaching";
            // String url = "http://www.truthdig.com/arts_culture/item/20071108_mark_sarvas_on_the_hot_zone/";
            // String url =
            // "http://www.scifisquad.com/2010/05/21/fridays-sci-fi-tv-its-a-spy-game-on-stargate-universe?icid=sphere_wpcom_tagsidebar/";

            String url = "http://g4tv.com/games/pc/61502/star-wars-the-old-republic/index/";
            url = "data/evaluation/daterecognition/webpages/webpage_1292927985086.html";
            // String url =
            // "http://www.politicsdaily.com/2010/06/10/harry-reid-ads-tout-jobs-creation-spokesman-calls-sharron-angl/";
            if (AllTests.ALL_TESTS) {
                ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
                // date.addAll(DateGetterHelper
                // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
                // date.addAll(DateGetterHelper
                // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
                DateGetter dateGetter = new DateGetter(url);
                dateGetter.setTechHTMLContent(true);
                ArrayList<ExtractedDate> dates = dateGetter.getDate();
                date.addAll(dates);
                DateArrayHelper.printDateArray(date);

            }
        }
    }

    // @Ignore
    @Test
    public void testGetContentDates2() throws FileNotFoundException {
        final String url = ResourceHelper.getResourcePath("/webpages/dateExtraction/Bangkok.htm");

            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setTechHTMLContent(true);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            // DateArrayHelper.printDateArray(date);
    }

    // @Ignore
    @Test
    public void testGetDate() {
        String url = "src/test/resources/webPages/dateExtraction/alltop.htm";
        // url = "http://www.zeit.de/2010/36/Wirtschaft-Konjunktur-Deutschland";
        //url = "http://www.abanet.org/antitrust/committees/intell_property/standardsettingresources.html";
        if (AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            DateGetter dateGetter = new DateGetter(url);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            // DateArrayHelper.printDateArray(date, DateType.ContentDate);
        }
    }

    @Test
    public void testGetDate2() {
        final String url = "http://www.friendfeed.com/share?title=Google+displays+incorrect+dates+from+news+sites&link=http://www.kullin.net/2010/05/google-displays-incorrect-dates-from-news-sites/";

        if (AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setTechHTMLContent(true);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            // DateArrayHelper.printDateArray(date);

        }
    }





    @Test
    @Ignore
    public void testFindRelativeDate() {
        String text = "5 days ago";
        ExtractedDate relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("2010-11-26", relDate.getNormalizedDate(false));
        text = "114 days ago";
        relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("2010-08-09", relDate.getNormalizedDate(false));
        text = "4 month ago";
        relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("2010-08-03", relDate.getNormalizedDate(false));
        text = "12 month ago";
        relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("2009-12-06", relDate.getNormalizedDate(false));
        text = "1 year ago";
        relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("2009-12-01", relDate.getNormalizedDate(false));
        text = "11 years ago";
        relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("1999-12-04", relDate.getNormalizedDate(false));
        text = "1 minute ago";
        relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("2010-12-01", relDate.getNormalizedDate(false));

    }
}
