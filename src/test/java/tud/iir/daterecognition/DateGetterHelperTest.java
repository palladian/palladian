package tud.iir.daterecognition;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Test;

import tud.iir.control.AllTests;
import tud.iir.knowledge.RegExp;

public class DateGetterHelperTest {

    @Test
    public void testGetURLDate() throws Exception {
        final String url1 = "http://www.example.com/2010-06-30/example.html";
        final String url2 = "http://www.zeit.de/sport/2010-06/example";
        final String url3 = "http://www.nytimes.com/2010/06/30/business/economy/30leonhardt.html?hp";
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

        // Cases without given day, so day will be set to 1st
        time = "2010-06";
        assertEquals(url2, time, DateGetterHelper.getURLDate(url2).getNormalizedDate());
        assertEquals(url4, time, DateGetterHelper.getURLDate(url4).getNormalizedDate());
        assertEquals(url6, time, DateGetterHelper.getURLDate(url6).getNormalizedDate());
        assertEquals(url8, time, DateGetterHelper.getURLDate(url8).getNormalizedDate());
    }

    @Test
    public void testGetDateFromString() throws Exception {

        ExtractedDate date;
        String text = "2010-06-05";
        date = DateGetterHelper.getDateFromString(text, RegExp.DATE_ISO8601_YMD);
        assertEquals(text, date.getNormalizedDate());

        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("Tue, 02 Jul 2010 19:07:49 GMT", RegExp.DATE_RFC_1123);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("Tuesday, 02-Jul-10 19:07:49 GMT", RegExp.DATE_RFC_1036);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("Tue Jul 2 19:07:49 2010", RegExp.DATE_ANSI_C);
        assertEquals(text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("Tue Jul 2 15:37:49 2010 -03:30", RegExp.DATE_ANSI_C_TZ);
        assertEquals(text, date.getNormalizedDate());
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
        date = DateGetterHelper.getDateFromString("2010-W29-5", RegExp.DATE_ISO8601_YWD);

        text = "2010-07-22";
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-W29-5T19:07:49.123", RegExp.DATE_ISO8601_YWD_T);
        assertEquals(date.getDateString(), text + " 19:07:49", date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010W295", RegExp.DATE_ISO8601_YWD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-203", RegExp.DATE_ISO8601_YD);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010203", RegExp.DATE_ISO8601_YD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010-W29", RegExp.DATE_ISO8601_YW);

        text = "2010-07";
        assertEquals(date.getDateString(), text, date.getNormalizedDate());
        date = DateGetterHelper.getDateFromString("2010W29", RegExp.DATE_ISO8601_YW_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDate());

    }

    @Test
    public void testGetSeparator() {
        final String date1 = "2010-05-06";
        final String date2 = "2010_05_06";
        final String date3 = "2010.05.06";
        final String date4 = "2010/05/06";

        assertEquals("-", DateGetterHelper.getSeparator(date1));
        assertEquals("_", DateGetterHelper.getSeparator(date2));
        assertEquals("\\.", DateGetterHelper.getSeparator(date3));
        assertEquals("/", DateGetterHelper.getSeparator(date4));

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
        if (!AllTests.ALL_TESTS) {
            ExtractedDate date = DateGetterHelper
                    .getHTTPHeaderDate("http://www.spreeblick.com/2010/07/08/william-shatner-hat-leonard-nimoys-fahrrad-geklaut/");
            System.out.println(date.getDateString());
        }

    }

    @Test
    public void testGetStructureDate() {
        final String url = "data/test/webPages/webPageW3C.htm";
        final String[] urlDates = { "2010-07-08T08:02:04-05:00", "2010-07-20T11:50:47-05:00",
                "2010-07-13T14:55:57-05:00", "2010-07-13T14:46:56-05:00", "2010-07-20", "2010-07-16", "2010-07-07" };
        if (!AllTests.ALL_TESTS) {
            final ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
            date.addAll(DateGetterHelper.getStructureDate(url));
            final Iterator<ExtractedDate> dateIterator = date.iterator();
            int index = 0;
            while (dateIterator.hasNext()) {
                final ExtractedDate extractedDate = dateIterator.next();
                assertEquals(urlDates[index], extractedDate.getDateString());
                index++;
            }
        }
    }
}
