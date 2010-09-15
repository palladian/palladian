package tud.iir.daterecognition;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import tud.iir.control.AllTests;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HTTPDate;
import tud.iir.daterecognition.dates.HeadDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.knowledge.RegExp;
import tud.iir.web.Crawler;

public class DateGetterHelperTest {

    @Test
    public void testGetURLDate() {
        final String url1 = "http://www.example.com/2010-06-30/example.html";
        final String url2 = "http://www.zeit.de/sport/2010-06/example";
        final String url3 = "http://www.nytimes.com2010_06_30/business/economy/30leonhardt.html?hp";
        final String url4 = "http://www.example.com/2010/06/example.html";
        final String url5 = "http://www.example.com/2010_06_30/example.html";
        final String url6 = "http://www.example.com/2010_06/example.html";
        final String url7 = "http://www.example.com/2010.06.30/example.html";
        final String url8 = "http://www.example.com/2010.06/example.html";
        final String url9 = "http://www.example.com/text/2010.06.30.html";
        final String url10 = "http://www.example.com/text/2010/othertext/06_30/example.html";
        final String url11 = "http://www.example.com/text/2010/othertext/06/30/example.html";
        final String url12 = "http://www.example.com/text/2010/othertext/06/30example.html";
        final String url13 = "http://www.example.com/text/2010/other/text/06_30example.html";
        final String url14 = "http://www.example.com/text/othertext/20100630example.html";
        final String url15 = "http://www.guardian.co.uk/world/2002/aug/06/iraq.johnhooper";

        // Cases with given day
        String time = "2010-06-30";
        assertEquals(url1, time, DateGetterHelper.getURLDate(url1).getNormalizedDate());
        assertEquals(url3, time, DateGetterHelper.getURLDate(url3).getNormalizedDate());
        assertEquals(url5, time, DateGetterHelper.getURLDate(url5).getNormalizedDate());
        assertEquals(url7, time, DateGetterHelper.getURLDate(url7).getNormalizedDate());
        assertEquals(url9, time, DateGetterHelper.getURLDate(url9).getNormalizedDate());
        assertEquals(url10, time, DateGetterHelper.getURLDate(url10).getNormalizedDate());
        assertEquals(url11, time, DateGetterHelper.getURLDate(url11).getNormalizedDate());
        assertEquals(url12, time, DateGetterHelper.getURLDate(url12).getNormalizedDate());
        assertEquals(url13, time, DateGetterHelper.getURLDate(url13).getNormalizedDate());
        assertEquals(url14, time, DateGetterHelper.getURLDate(url14).getNormalizedDate());
        assertEquals(url14, "2002-08-06", DateGetterHelper.getURLDate(url15).getNormalizedDate());

        // Cases without given day, so day will be set to 1st
        time = "2010-06";
        assertEquals(url2, time, DateGetterHelper.getURLDate(url2).getNormalizedDate());
        assertEquals(url4, time, DateGetterHelper.getURLDate(url4).getNormalizedDate());
        assertEquals(url6, time, DateGetterHelper.getURLDate(url6).getNormalizedDate());
        assertEquals(url8, time, DateGetterHelper.getURLDate(url8).getNormalizedDate());
    }

    @Test
    public void testGetDateFromString() {

        ExtractedDate date;
        String text;

        // ISO8601_YMD_T
        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("2010-07-02T19:07:49", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07:49", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-07-02T21:07:49+02:00", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-07-02 21:07:49+02:00", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-07-02T16:37:49-02:30", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-12-31 22:37-02:30", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2011-01-01 01:07", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-07-02T19", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19:07", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07Z", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19:07", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07:49Z", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07:49.123", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010/07/02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010/07/02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010.07.02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010.07.02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010_07_02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010_07_02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDate());

        // ISO_YMD
        text = "2010-06-05";
        date = DateGetterHelper.getDateFromString(text, RegExp.DATE_ISO8601_YMD);
        assertEquals(text, date.getNormalizedDate());

        // ISO_YM
        text = "2010-06";
        date = DateGetterHelper.getDateFromString(text, RegExp.DATE_ISO8601_YM);
        assertEquals(text, date.getNormalizedDate());

        // ISO8601_YWD
        text = "2010-07-22";
        date = DateGetterHelper.getDateFromString("2010-W29-5", RegExp.DATE_ISO8601_YWD);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());

        // ISO8601_YWD_T
        text = "2010-07-22 19:07:49";
        date = DateGetterHelper.getDateFromString("2010-W29-5T19:07:49.123", RegExp.DATE_ISO8601_YWD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());

        // ISO8601_YW
        text = "2010-07";
        date = DateGetterHelper.getDateFromString("2010-W29", RegExp.DATE_ISO8601_YW);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());

        // ISO8601_YD
        text = "2010-07-22";
        date = DateGetterHelper.getDateFromString("2010-203", RegExp.DATE_ISO8601_YD);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());

        // URL_D
        text = "2010-06-30";
        date = DateGetterHelper.getDateFromString("2010.06.30", RegExp.DATE_URL_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010_06_30", RegExp.DATE_URL_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010/06/30", RegExp.DATE_URL_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010/June/30", RegExp.DATE_URL_MMMM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());

        // URL
        text = "2010-06";
        date = DateGetterHelper.getDateFromString("2010.06", RegExp.DATE_URL);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010_06", RegExp.DATE_URL);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010/06", RegExp.DATE_URL);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());

        // EU & USA
        text = "2010-07-25";
        date = DateGetterHelper.getDateFromString("25.07.2010", RegExp.DATE_EU_D_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("07/25/2010", RegExp.DATE_USA_MM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("25. Juli 2010", RegExp.DATE_EU_D_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("July 25, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("July 25th, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        text = "2010-07-05";
        date = DateGetterHelper.getDateFromString("5.07.2010", RegExp.DATE_EU_D_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("7/5/2010", RegExp.DATE_USA_MM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("5. Juli 2010", RegExp.DATE_EU_D_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("July 5, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("July 5th, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        text = "2010-07";
        date = DateGetterHelper.getDateFromString("July 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("Juli 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("07.2010", RegExp.DATE_EU_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("June 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(date.getDateString(), "2010-06", date.getNormalizedDate());
        text = "0-07-25";
        date = DateGetterHelper.getDateFromString("25.07.", RegExp.DATE_EU_D_MM);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("25. Juli", RegExp.DATE_EU_D_MMMM);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("07/25", RegExp.DATE_USA_MM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("July 25th", RegExp.DATE_USA_MMMM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("July 25", RegExp.DATE_USA_MMMM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        text = "2010-07";
        date = DateGetterHelper.getDateFromString("07/2010", RegExp.DATE_USA_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("09/8/10 04:56 PM", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals(date.getDateString(), "2010-09-08 16:56", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("August 10, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), "2010-08-10", date.getNormalizedDate());

        // DATE_RFC & ANSI C
        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("Tue Jul 2 19:07:49 2010", RegExp.DATE_ANSI_C);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("Tue Jul 2 15:37:49 2010 -03:30", RegExp.DATE_ANSI_C_TZ);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("Tue, 02 Jul 2010 19:07:49 GMT", RegExp.DATE_RFC_1123);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("Tuesday, 02-Jul-10 19:07:49 GMT", RegExp.DATE_RFC_1036);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("Wed, 08 Sep 2010 08:09:15 EST", RegExp.DATE_RFC_1123);
        assertEquals("2010-09-08 08:09:15", date.getNormalizedDate());

        // ISO without separator
        text = "2010-07-25";
        date = DateGetterHelper.getDateFromString("20100725", RegExp.DATE_ISO8601_YMD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        text = "2010-07-22";
        date = DateGetterHelper.getDateFromString("2010W295", RegExp.DATE_ISO8601_YWD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        text = "2010-07";
        date = DateGetterHelper.getDateFromString("2010W29", RegExp.DATE_ISO8601_YW_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        text = "2010-07-22";
        date = DateGetterHelper.getDateFromString("2010203", RegExp.DATE_ISO8601_YD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());

        // RFC + UTC
        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("Tue, 02 Jul 2010 20:07:49 +0100", RegExp.DATE_RFC_1123_UTC);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("Tuesday, 02-Jul-10 18:07:49 -0100", RegExp.DATE_RFC_1036_UTC);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("Tue, 02 Jul 2010 20:07:49 +01:00", RegExp.DATE_RFC_1123_UTC);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("Tuesday, 02-Jul-10 18:07:49 -01:00", RegExp.DATE_RFC_1036_UTC);
        assertEquals(text, date.getNormalizedDate());

        // EU & USA time

        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("02.07.2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("02-07-2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("02/07/2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("02_07_2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("02. Juli 2010 20:07:49 +0100", RegExp.DATE_EU_D_MMMM_Y_T);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("07/02/2010 20:07:49 +0100", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("July 02nd, 2010 20:07:49 +0100", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("04.08.2006 / 14:52", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("08/04/2006 / 14:52", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("04 August 2006 / 14:52", RegExp.DATE_EU_D_MMMM_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("aug 4, 2006 / 14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("aug 4, 2006 14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("aug 4, 2006  14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDate());

        // others
        text = "2010-07-02 19:07:49";
        assertEquals(text, DateGetterHelper.findDate("Tue, 02 Jul 2010 19:07:49 GMT").getNormalizedDate());
        assertEquals("2010-07-23 15:35:58", DateGetterHelper.findDate("7/23/2010 3:35:58 PM").getNormalizedDate());
        assertEquals("2010-07-23 15:35:58", DateGetterHelper.findDate("23.7.2010 3:35:58 PM").getNormalizedDate());
        assertEquals("2010-07-23 15:35:58", DateGetterHelper.findDate("July 23rd, 2010 3:35:58 PM").getNormalizedDate());
        assertEquals("2010-07-23 15:35:58", DateGetterHelper.findDate("23. Juli 2010 3:35:58 PM").getNormalizedDate());
        assertEquals("2010-07-20 00:00:00", DateGetterHelper.findDate("Tue, 20 Jul 2010 00:00:00 Z")
                .getNormalizedDate());
        assertEquals("2010-07-23 16:49:18", DateGetterHelper.findDate("Fri, 23 JUL 2010 16:49:18 AEST")
                .getNormalizedDate());
        assertEquals("2010-07-24", DateGetterHelper.findDate("07/24/2010").getNormalizedDate());
        assertEquals("2010-07-24", DateGetterHelper.findDate("Jul 24, 2010 EST").getNormalizedDate());
        assertEquals("2010-07-25", DateGetterHelper.findDate("Sun, 25 Jul 2010").getNormalizedDate());
        assertEquals("2010-03-07 22:53:50", DateGetterHelper.findDate("Sun 7 Mar 2010 10:53:50 PM GMT")
                .getNormalizedDate());
        assertEquals("2010-08-18 13:20", DateGetterHelper.findDate("Wednesday August 18, 2010, 8:20 PM GMT +07:00")
                .getNormalizedDate());
        assertEquals("2010-08-18 13:20", DateGetterHelper.findDate("Wednesday August 18, 2010 8:20 PM GMT +07:00")
                .getNormalizedDate());
        assertEquals("2010-08-18 13:20", DateGetterHelper.findDate("Wednesday August 18, 2010, 8:20 PM +07:00")
                .getNormalizedDate());
        assertEquals("2010-07-29", DateGetterHelper.findDate("29/07/2010").getNormalizedDate());
        assertEquals("2010-09-07", DateGetterHelper.findDate("09/07/2010").getNormalizedDate());
        assertEquals("2010-08-23", DateGetterHelper.findDate("Monday, August 23, 2010").getNormalizedDate());

    }

    @Test
    public void testFindAllDates() {
        assertEquals("2010-01", (DateGetterHelper.findALLDates("Januar 2010")).get(0).getNormalizedDate());
        assertEquals("2010-02", (DateGetterHelper.findALLDates("Februar 2010")).get(0).getNormalizedDate());
        assertEquals("2010-03", (DateGetterHelper.findALLDates("März 2010")).get(0).getNormalizedDate());
        assertEquals("2010-04", (DateGetterHelper.findALLDates("April 2010")).get(0).getNormalizedDate());
        assertEquals("2010-05", (DateGetterHelper.findALLDates("Mai 2010")).get(0).getNormalizedDate());
        assertEquals("2010-06", (DateGetterHelper.findALLDates("Juni 2010")).get(0).getNormalizedDate());
        assertEquals("2010-07", (DateGetterHelper.findALLDates("Juli 2010")).get(0).getNormalizedDate());
        System.out.println("August 2010");
        assertEquals("2010-08", (DateGetterHelper.findALLDates("August 2010")).get(0).getNormalizedDate());
        assertEquals("MMMM YYYY", (DateGetterHelper.findALLDates("August 2010")).get(0).getFormat());
        assertEquals("2010-09", (DateGetterHelper.findALLDates("September 2010")).get(0).getNormalizedDate());
        assertEquals("2010-10", (DateGetterHelper.findALLDates("Oktober 2010")).get(0).getNormalizedDate());
        assertEquals("2010-11", (DateGetterHelper.findALLDates("November 2010")).get(0).getNormalizedDate());
        assertEquals("2010-12", (DateGetterHelper.findALLDates("Dezember 2010")).get(0).getNormalizedDate());

    }

    @Test
    public void testFindDate() {
        String text = "2010-08-03";
        assertEquals(text, (DateGetterHelper.findDate("2010-08-03")).getNormalizedDate());
        assertEquals("2002-08-06 03:08", (DateGetterHelper.findDate("2002-08-06T03:08BST")).getNormalizedDate());
        assertEquals("2010-06", (DateGetterHelper.findDate("June 2010")).getNormalizedDate());
        assertEquals("2010-08-31", (DateGetterHelper.findDate("Aug 31 2010")).getNormalizedDate());
        assertEquals("2009-04-06 15:11", (DateGetterHelper.findDate("April  6, 2009  3:11 PM")).getNormalizedDate());
        ExtractedDate date = DateGetterHelper.findDate("aug 4, 2006 / 14:52");
        assertEquals("2006-08-04 14:52", date.getNormalizedDate());

    }

    @Test
    public void testGetSeparator() {
        final String date1 = "2010-05-06";
        final String date2 = "2010_05_06";
        final String date3 = "2010.05.06";
        final String date4 = "2010/05/06";

        assertEquals("-", ExtractedDateHelper.getSeparator(date1));
        assertEquals("_", ExtractedDateHelper.getSeparator(date2));
        assertEquals("\\.", ExtractedDateHelper.getSeparator(date3));
        assertEquals("/", ExtractedDateHelper.getSeparator(date4));

    }

    @Test
    public void testGetDateparts() {
        final String[] referenz1 = { "2010", "06", "30" };
        final String[] referenz2 = { "93", "06", "14" };
        final String[] referenz3 = { "10", "06", "30" };

        final String[] date1 = { "2010", "06", "30" };
        final String[] date2 = { "30", "2010", "06" };
        final String[] date3 = { "06", "2010", "30" };
        final String[] date4 = { "06", "30", "2010" };
    }

    @Test
    public void testGetHTTPHeaderDate() {
        System.out.println("testGetHTTPHeaderDate:");

        /*
         * ExtractedDate date = DateGetterHelper
         * .getHTTPHeaderDate("http://www.spreeblick.com/2010/07/08/william-shatner-hat-leonard-nimoys-fahrrad-geklaut/"
         * );
         */
        // String url = "http://www.zeit.de/politik/ausland/2010-09/russland-waldbraende-siedlungen";
        String url = "http://www.spreeblick.com/2010/07/08/william-shatner-hat-leonard-nimoys-fahrrad-geklaut/";
        ArrayList<HTTPDate> dates = DateGetterHelper.getHTTPHeaderDate(url);
        for (int i = 0; i < dates.size(); i++) {
            System.out.println(dates.get(i).getDateString());
        }

    }

    @Test
    public void testGetStructureDate() {
        String url = "data/test/webPages/webPageW3C.htm";
        String[] urlDates = { "2010-07-08T08:02:04-05:00", "2010-07-20T11:50:47-05:00", "2010-07-13T14:55:57-05:00",
                "2010-07-13T14:46:56-05:00", "2010-07-20", "2010-07-16", "2010-07-07" };
        if (!AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllFalse();
            dateGetter.setTechHTMLStruct(true);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            Iterator<ExtractedDate> dateIterator = date.iterator();
            int index = 0;
            while (dateIterator.hasNext()) {
                final ExtractedDate extractedDate = dateIterator.next();

                assertEquals(urlDates[index], extractedDate.getDateString());
                index++;
            }
        }
    }

    @Test
    public void testGetStructureDate2() {

        // String url = "http://www.aftonbladet.se/wendela/ledig/article3476060.ab";
        String url = "http://www.guardian.co.uk/world/2002/aug/06/iraq.johnhooper";

        if (!AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllFalse();
            dateGetter.setTechHTMLStruct(true);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            DateArrayHelper.printDateArray(date);
        }
    }

    @Test
    public void testGetContentDates() {
        // final String url = "data/test/webPages/dateExtraction/kullin.htm";
        // String url =
        // "http://www.gatorsports.com/article/20100823/ARTICLES/100829802/1136?Title=Meyer-has-concerns-with-season-fast-approaching";
        // String url = "http://www.truthdig.com/arts_culture/item/20071108_mark_sarvas_on_the_hot_zone/";
        String url = "http://www.scifisquad.com/2010/05/21/fridays-sci-fi-tv-its-a-spy-game-on-stargate-universe?icid=sphere_wpcom_tagsidebar/";
        if (!AllTests.ALL_TESTS) {

            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllFalse();
            dateGetter.setTechHTMLContent(true);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            DateArrayHelper.printDateArray(date);

        }
    }

    @Test
    public void testGetContentDates2() {
        final String url = "data/test/webPages/dateExtraction/Bangkok.htm";

        if (!AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllFalse();
            dateGetter.setTechHTMLContent(true);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            DateArrayHelper.printDateArray(date);

        }
    }

    @Test
    public void testGetDate() {
        String url = "data/test/webPages/dateExtraction/alltop.htm";
        url = "http://www.zeit.de/2010/36/Wirtschaft-Konjunktur-Deutschland";

        if (!AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllTrue();
            dateGetter.setTechReference(false);
            dateGetter.setTechArchive(false);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            DateArrayHelper.printDateArray(date, ExtractedDate.TECH_HTML_CONT);

        }
    }

    @Test
    public void testGetDate2() {
        final String url = "http://www.friendfeed.com/share?title=Google+displays+incorrect+dates+from+news+sites&link=http://www.kullin.net/2010/05/google-displays-incorrect-dates-from-news-sites/";

        if (!AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllFalse();
            dateGetter.setTechHTMLContent(true);
            dateGetter.setTechHTMLStruct(false);
            dateGetter.setTechReference(false);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            DateArrayHelper.printDateArray(date);

        }
    }

    @Ignore
    @Test
    public void testGetReferenceDates() {
        String url = "http://www.spiegel.de/index.html";
        // String url = "data/test/webPages/dateExtraction/kullin.htm";

        if (!AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllFalse();
            dateGetter.setTechReference(true);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            DateArrayHelper.printDateArray(date);

        }
    }

    @Test
    public void TestGetHeadDates2() {
        String url = "http://www.huffingtonpost.com/2010/09/08/mark-hurds-salary-at-orac_n_708676.html";

        Crawler c = new Crawler();
        c.setDocument(url);
        Document document = c.getDocument();
        ArrayList<HeadDate> headDates = DateGetterHelper.getHeadDates(document);
        DateArrayHelper.printDateArray(headDates);

    }

    @Test
    public void TestGetHeadDates() {
        String url = "data/test/webPages/dateExtraction/zeit2.htm";
        ArrayList<HeadDate> compareDates = new ArrayList<HeadDate>();
        compareDates.add(new HeadDate("2010-09-03T09:43:13.211280+00:00", RegExp.DATE_ISO8601_YMD_T[1]));
        compareDates.add(new HeadDate("2010-09-02T06:00:00+00:00", RegExp.DATE_ISO8601_YMD_T[1]));
        compareDates.add(new HeadDate("2010-09-03T09:44:12.597203+00:00", RegExp.DATE_ISO8601_YMD_T[1]));
        compareDates.add(new HeadDate("2010-09-03T09:41:54.059727+00:00", RegExp.DATE_ISO8601_YMD_T[1]));
        compareDates.add(new HeadDate("2010-09-03T09:43:13.211280+00:00", RegExp.DATE_ISO8601_YMD_T[1]));
        compareDates.add(new HeadDate("2010-09-02T06:00:00+00:00", RegExp.DATE_ISO8601_YMD_T[1]));

        Crawler c = new Crawler();
        c.setDocument(url);
        Document document = c.getDocument();
        ArrayList<HeadDate> headDates = DateGetterHelper.getHeadDates(document);
        assertEquals(6, headDates.size());
        for (int i = 0; i < headDates.size(); i++) {
            assertEquals(compareDates.get(i).getDateString(), headDates.get(i).getDateString());
        }

    }
}
