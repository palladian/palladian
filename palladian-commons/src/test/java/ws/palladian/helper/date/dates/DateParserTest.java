package ws.palladian.helper.date.dates;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateHelper;

public class DateParserTest {

    @Test
    public void testGetDateFromString() {

        ExtractedDate date;
        String text;

        // ISO8601_YMD_T
        text = "2010-07-02 19:07:49";
        date = DateParser.getDateFromString("2010-07-02T19:07:49", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010-07-02 19:07:49", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010-07-02T21:07:49+02:00", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010-07-02 21:07:49+02:00", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010-07-02T16:37:49-02:30", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010-12-31 22:37-02:30", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2011-01-01 01:07", date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010-07-02T19", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19", date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010-07-02 19:07", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19:07", date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010-07-02 19:07Z", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19:07", date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010-07-02 19:07:49Z", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010-07-02 19:07:49.123", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010/07/02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010/07/02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010.07.02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010.07.02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010_07_02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010_07_02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010/07/02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());

        // ISO_YMD
        text = "2010-06-05";
        date = DateParser.getDateFromString(text, RegExp.DATE_ISO8601_YMD);
        assertEquals(text, date.getNormalizedDateString());

        // ISO_YM
        text = "2010-06";
        date = DateParser.getDateFromString(text, RegExp.DATE_ISO8601_YM);
        assertEquals(text, date.getNormalizedDateString());

        // ISO8601_YWD
        text = "2010-07-22";
        date = DateParser.getDateFromString("2010-W29-5", RegExp.DATE_ISO8601_YWD);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // ISO8601_YWD_T
        text = "2010-07-22 19:07:49";
        date = DateParser.getDateFromString("2010-W29-5T19:07:49.123", RegExp.DATE_ISO8601_YWD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // ISO8601_YW
        text = "2010-07";
        date = DateParser.getDateFromString("2010-W29", RegExp.DATE_ISO8601_YW);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // ISO8601_YD
        text = "2010-07-22";
        date = DateParser.getDateFromString("2010-203", RegExp.DATE_ISO8601_YD);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // URL_D
        text = "2010-06-30";
        date = DateParser.getDateFromString("2010.06.30", RegExp.DATE_URL_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010_06_30", RegExp.DATE_URL_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010/06/30/", RegExp.DATE_URL_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010/June/30/", RegExp.DATE_URL_MMMM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // URL
        text = "2010-06";
        date = DateParser.getDateFromString("2010.06", RegExp.DATE_URL);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010_06", RegExp.DATE_URL);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("2010/06", RegExp.DATE_URL);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // EU & USA
        text = "2010-07-25";
        date = DateParser.getDateFromString("25.07.2010", RegExp.DATE_EU_D_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("07/25/2010", RegExp.DATE_USA_MM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("25. Juli 2010", RegExp.DATE_EU_D_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("July 25, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("July 25th, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07-05";
        date = DateParser.getDateFromString("5.07.2010", RegExp.DATE_EU_D_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("7/5/2010", RegExp.DATE_USA_MM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("5. Juli 2010", RegExp.DATE_EU_D_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("July 5, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("July 5th, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07";
        date = DateParser.getDateFromString("July 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("Juli 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("07.2010", RegExp.DATE_EU_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("June 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(date.getDateString(), "2010-06", date.getNormalizedDateString());
        text = "0-07-25";
        date = DateParser.getDateFromString("25.07.", RegExp.DATE_EU_D_MM);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("25. Juli", RegExp.DATE_EU_D_MMMM);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("07/25", RegExp.DATE_USA_MM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("July 25th", RegExp.DATE_USA_MMMM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("July 25", RegExp.DATE_USA_MMMM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07";
        date = DateParser.getDateFromString("07/2010", RegExp.DATE_USA_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("09/8/10 04:56 PM", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals(date.getDateString(), "2010-09-08 16:56", date.getNormalizedDateString());
        date = DateParser.getDateFromString("August 10, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), "2010-08-10", date.getNormalizedDateString());

        // DATE_RFC & ANSI C
        text = "2010-07-02 19:07:49";
        date = DateParser.getDateFromString("Tue Jul 2 19:07:49 2010", RegExp.DATE_ANSI_C);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("Tue Jul 2 15:37:49 2010 -03:30", RegExp.DATE_ANSI_C_TZ);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("Tue, 02 Jul 2010 19:07:49 GMT", RegExp.DATE_RFC_1123);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("Tuesday, 02-Jul-10 19:07:49 GMT", RegExp.DATE_RFC_1036);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("Wed, 08 Sep 2010 08:09:15 EST", RegExp.DATE_RFC_1123);
        assertEquals("2010-09-08 08:09:15", date.getNormalizedDateString());

        // ISO without separator
        text = "2010-07-25";
        date = DateParser.getDateFromString("20100725", RegExp.DATE_ISO8601_YMD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07-22";
        date = DateParser.getDateFromString("2010W295", RegExp.DATE_ISO8601_YWD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07";
        date = DateParser.getDateFromString("2010W29", RegExp.DATE_ISO8601_YW_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07-22";
        date = DateParser.getDateFromString("2010203", RegExp.DATE_ISO8601_YD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // RFC + UTC
        text = "2010-07-02 19:07:49";
        date = DateParser.getDateFromString("Tue, 02 Jul 2010 20:07:49 +0100", RegExp.DATE_RFC_1123_UTC);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("Tuesday, 02-Jul-10 18:07:49 -0100", RegExp.DATE_RFC_1036_UTC);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("Tue, 02 Jul 2010 20:07:49 +01:00", RegExp.DATE_RFC_1123_UTC);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("Tuesday, 02-Jul-10 18:07:49 -01:00", RegExp.DATE_RFC_1036_UTC);
        assertEquals(text, date.getNormalizedDateString());

        // EU & USA time

        text = "2010-07-02 19:07:49";
        date = DateParser.getDateFromString("02.07.2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("02-07-2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("02/07/2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("02_07_2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("02. Juli 2010 20:07:49 +0100", RegExp.DATE_EU_D_MMMM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("07/02/2010 20:07:49 +0100", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("July 02nd, 2010 20:07:49 +0100", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateParser.getDateFromString("04.08.2006 / 14:52", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.getDateFromString("08/04/2006 / 14:52", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.getDateFromString("04 August 2006 / 14:52", RegExp.DATE_EU_D_MMMM_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.getDateFromString("aug 4, 2006 / 14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.getDateFromString("aug 4, 2006 14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.getDateFromString("aug 4, 2006  14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.getDateFromString("Saturday, September 20. 2008", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals("2008-09-20", date.getNormalizedDateString());
        date = DateParser.getDateFromString("11-12-2010 19:48:00", RegExp.DATE_USA_MM_D_Y_T_SEPARATOR);
        assertEquals("2010-11-12 19:48:00", date.getNormalizedDateString());

        // others
        text = "2010-07-02 19:07:49";
        assertEquals(text, DateParser.findDate("Tue, 02 Jul 2010 19:07:49 GMT").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateParser.findDate("7/23/2010 3:35:58 PM").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateParser.findDate("23.7.2010 3:35:58 PM").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateParser.findDate("July 23rd, 2010 3:35:58 PM").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateParser.findDate("23. Juli 2010 3:35:58 PM").getNormalizedDateString());
        assertEquals("2010-07-20 00:00:00", DateParser.findDate("Tue, 20 Jul 2010 00:00:00 Z")
                .getNormalizedDateString());
        assertEquals("2010-07-23 16:49:18", DateParser.findDate("Fri, 23 JUL 2010 16:49:18 AEST")
                .getNormalizedDateString());
        assertEquals("2010-07-24", DateParser.findDate("07/24/2010").getNormalizedDateString());
        assertEquals("2010-07-24", DateParser.findDate("Jul 24, 2010 EST").getNormalizedDateString());
        assertEquals("2010-07-25", DateParser.findDate("Sun, 25 Jul 2010").getNormalizedDateString());
        assertEquals("2010-03-07 22:53:50", DateParser.findDate("Sun 7 Mar 2010 10:53:50 PM GMT")
                .getNormalizedDateString());
        assertEquals("2010-08-18 13:20", DateParser.findDate("Wednesday August 18, 2010, 8:20 PM GMT +07:00")
                .getNormalizedDateString());
        assertEquals("2010-08-18 13:20", DateParser.findDate("Wednesday August 18, 2010 8:20 PM GMT +07:00")
                .getNormalizedDateString());
        assertEquals("2010-08-18 13:20", DateParser.findDate("Wednesday August 18, 2010, 8:20 PM +07:00")
                .getNormalizedDateString());
        assertEquals("2010-07-29", DateParser.findDate("29/07/2010").getNormalizedDateString());
        assertEquals("2010-09-07", DateParser.findDate("09/07/2010").getNormalizedDateString());
        assertEquals("2010-08-23", DateParser.findDate("Monday, August 23, 2010").getNormalizedDateString());
        assertEquals("2010-09-23", DateParser.findDate("Monday, Sep 23, 2010").getNormalizedDateString());

    }

    @Test
    public void testFindDate() {
        String text = "2010-08-03";
        assertEquals(text, DateParser.findDate("2010-08-03").getNormalizedDateString());
        assertEquals("2002-08-06 03:08", DateParser.findDate("2002-08-06T03:08BST").getNormalizedDateString());
        assertEquals("2010-06", DateParser.findDate("June 2010").getNormalizedDateString());
        assertEquals("2010-08-31", DateParser.findDate("Aug 31 2010").getNormalizedDateString());
        assertEquals("2009-04-06 15:11", DateParser.findDate("April  6, 2009  3:11 PM").getNormalizedDateString());
        ExtractedDate date = DateParser.findDate("aug 4, 2006 / 14:52");
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateParser.findDate("2007-aug-12");
        assertEquals("2007-08-12", date.getNormalizedDateString());
        date = DateParser.findDate("2007-aug.-12");
        assertEquals("2007-08-12", date.getNormalizedDateString());
        date = DateParser.findDate("2007-August-12");
        assertEquals("2007-08-12", date.getNormalizedDateString());
        date = DateParser.findDate("2010/07/02");
        assertEquals("2010-07-02", date.getNormalizedDateString());

    }

    @Test
    @Ignore
    public void testFindRelativeDate() {
        String text = "5 days ago";
        ExtractedDate relDate = DateParser.findRelativeDate(text);
        assertEquals("2010-11-26", relDate.getNormalizedDate(false));
        text = "114 days ago";
        relDate = DateParser.findRelativeDate(text);
        assertEquals("2010-08-09", relDate.getNormalizedDate(false));
        text = "4 month ago";
        relDate = DateParser.findRelativeDate(text);
        assertEquals("2010-08-03", relDate.getNormalizedDate(false));
        text = "12 month ago";
        relDate = DateParser.findRelativeDate(text);
        assertEquals("2009-12-06", relDate.getNormalizedDate(false));
        text = "1 year ago";
        relDate = DateParser.findRelativeDate(text);
        assertEquals("2009-12-01", relDate.getNormalizedDate(false));
        text = "11 years ago";
        relDate = DateParser.findRelativeDate(text);
        assertEquals("1999-12-04", relDate.getNormalizedDate(false));
        text = "1 minute ago";
        relDate = DateParser.findRelativeDate(text);
        assertEquals("2010-12-01", relDate.getNormalizedDate(false));

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
        assertEquals(1999, DateParserLogic.get4DigitYear(99));
        assertEquals(2010, DateParserLogic.get4DigitYear(10));
    }

    @Test
    public void testRemoveTimezone() {
        assertEquals("22:10 ", DateParserLogic.removeTimezone("22:10  UTC")[0]);
        assertEquals("22:10 ", DateParserLogic.removeTimezone("22:10 UTC")[0]);
        assertEquals("22:10 ", DateParserLogic.removeTimezone("22:10 GMT")[0]);
    }

    @Test
    public void testGetSeparator() {
        assertEquals("\\.", DateParserLogic.getSeparator("10.10.2010"));
        assertEquals("-", DateParserLogic.getSeparator("10-10-2010"));
        assertEquals("_", DateParserLogic.getSeparator("10_10_2010"));
        assertEquals("/", DateParserLogic.getSeparator("10/10/2010"));
        assertEquals("-", DateParserLogic.getSeparator("2010-05-06"));
        assertEquals("_", DateParserLogic.getSeparator("2010_05_06"));
        assertEquals("\\.", DateParserLogic.getSeparator("2010.05.06"));
        assertEquals("/", DateParserLogic.getSeparator("2010/05/06"));
    }

    @Test
    public void testmonthNameToNumber() {
        assertEquals("01", DateHelper.monthNameToNumber("Januar"));
        assertEquals("02", DateHelper.monthNameToNumber("Februar"));
        assertEquals("03", DateHelper.monthNameToNumber("März"));
        assertEquals("04", DateHelper.monthNameToNumber("April"));
        assertEquals("05", DateHelper.monthNameToNumber("Mai"));
        assertEquals("06", DateHelper.monthNameToNumber("Juni"));
        assertEquals("07", DateHelper.monthNameToNumber("Juli"));
        assertEquals("08", DateHelper.monthNameToNumber("August"));
        assertEquals("09", DateHelper.monthNameToNumber("September"));
        assertEquals("10", DateHelper.monthNameToNumber("Oktober"));
        assertEquals("11", DateHelper.monthNameToNumber("November"));
        assertEquals("12", DateHelper.monthNameToNumber("Dezember"));

        assertEquals("01", DateHelper.monthNameToNumber("January"));
        assertEquals("02", DateHelper.monthNameToNumber("February"));
        assertEquals("03", DateHelper.monthNameToNumber("March"));
        assertEquals("04", DateHelper.monthNameToNumber("April"));
        assertEquals("05", DateHelper.monthNameToNumber("May"));
        assertEquals("06", DateHelper.monthNameToNumber("June"));
        assertEquals("07", DateHelper.monthNameToNumber("July"));
        assertEquals("08", DateHelper.monthNameToNumber("August"));
        assertEquals("09", DateHelper.monthNameToNumber("September"));
        assertEquals("10", DateHelper.monthNameToNumber("October"));
        assertEquals("11", DateHelper.monthNameToNumber("November"));
        assertEquals("12", DateHelper.monthNameToNumber("December"));

        assertEquals("01", DateHelper.monthNameToNumber("Jan"));
        assertEquals("02", DateHelper.monthNameToNumber("Feb"));
        assertEquals("03", DateHelper.monthNameToNumber("Mär"));
        assertEquals("04", DateHelper.monthNameToNumber("Apr"));
        assertEquals("05", DateHelper.monthNameToNumber("Mai"));
        assertEquals("06", DateHelper.monthNameToNumber("Jun"));
        assertEquals("07", DateHelper.monthNameToNumber("Jul"));
        assertEquals("08", DateHelper.monthNameToNumber("Aug"));
        assertEquals("09", DateHelper.monthNameToNumber("Sep"));
        assertEquals("10", DateHelper.monthNameToNumber("Okt"));
        assertEquals("11", DateHelper.monthNameToNumber("Nov"));
        assertEquals("12", DateHelper.monthNameToNumber("Dez"));

        assertEquals("01", DateHelper.monthNameToNumber("Jan"));
        assertEquals("02", DateHelper.monthNameToNumber("Feb"));
        assertEquals("03", DateHelper.monthNameToNumber("Mar"));
        assertEquals("04", DateHelper.monthNameToNumber("Apr"));
        assertEquals("05", DateHelper.monthNameToNumber("May"));
        assertEquals("06", DateHelper.monthNameToNumber("Jun"));
        assertEquals("07", DateHelper.monthNameToNumber("Jul"));
        assertEquals("08", DateHelper.monthNameToNumber("Aug"));
        assertEquals("09", DateHelper.monthNameToNumber("Sep"));
        assertEquals("10", DateHelper.monthNameToNumber("Oct"));
        assertEquals("11", DateHelper.monthNameToNumber("Nov"));
        assertEquals("12", DateHelper.monthNameToNumber("Dec"));

    }

}
