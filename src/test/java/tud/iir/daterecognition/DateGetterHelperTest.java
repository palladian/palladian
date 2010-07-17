package tud.iir.daterecognition;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import tud.iir.control.AllTests;
import tud.iir.knowledge.RegExp;

public class DateGetterHelperTest {

    private String url1;
    private String url2;
    private String url3;
    private String url4;
    private String url5;
    private String url6;
    private String url7;
    private String url8;
    private String url9;

    @Before
    public void setUp() throws Exception {
        url1 = "http://www.example.com/2010-06-30/example.html";
        url2 = "http://www.zeit.de/sport/2010-06/example";
        url3 = "http://www.nytimes.com/2010/06/30/business/economy/30leonhardt.html?hp";
        url4 = "http://www.example.com/2010/06/example.html";
        url5 = "http://www.example.com/2010_06_30/example.html";
        url6 = "http://www.example.com/2010_06/example.html";
        url7 = "http://www.example.com/2010.06.30/example.html";
        url8 = "http://www.example.com/2010.06/example.html";
        url9 = "http://www.example.com/text/2010.06.30.html";
    }

    @Test
    public void testGetURLDate() {

        // Cases with given day
        String time = "2010-06-30";
        assertEquals(url1, time, DateGetterHelper.getURLDate(url1).getNormalizedDate());
        assertEquals(url3, time, DateGetterHelper.getURLDate(url3).getNormalizedDate());
        assertEquals(url5, time, DateGetterHelper.getURLDate(url5).getNormalizedDate());
        assertEquals(url7, time, DateGetterHelper.getURLDate(url7).getNormalizedDate());
        assertEquals(url9, time, DateGetterHelper.getURLDate(url9).getNormalizedDate());

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

    }

    @Test
    public void testGetSeparator() {
        String date1 = "2010-05-06";
        String date2 = "2010_05_06";
        String date3 = "2010.05.06";
        String date4 = "2010/05/06";

        assertEquals("-", DateGetterHelper.getSeparator(date1));
        assertEquals("_", DateGetterHelper.getSeparator(date2));
        assertEquals("\\.", DateGetterHelper.getSeparator(date3));
        assertEquals("/", DateGetterHelper.getSeparator(date4));

    }

    @Test
    public void testGetDateparts() {
        String[] referenz1 = { "2010", "06", "30" };
        String[] referenz2 = { "93", "06", "14" };
        String[] referenz3 = { "10", "06", "30" };

        String[] date1 = { "2010", "06", "30" };
        String[] date2 = { "30", "2010", "06" };
        String[] date3 = { "06", "2010", "30" };
        String[] date4 = { "06", "30", "2010" };
    }

    @Test
    public void testGetHTTPHeaderDate() {

        if (!AllTests.ALL_TESTS) {
            ExtractedDate date = DateGetterHelper
                    .getHTTPHeaderDate("http://www.spreeblick.com/2010/07/08/william-shatner-hat-leonard-nimoys-fahrrad-geklaut/");
            System.out.println(date.getDateString());
        }

    }
}
