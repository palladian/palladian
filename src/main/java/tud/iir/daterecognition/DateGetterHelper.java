package tud.iir.daterecognition;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tud.iir.knowledge.RegExp;
import tud.iir.web.Crawler;

/**
 * DateGetterHelper provides the techniques to find dates out of webpages. Also provides different helper methods.
 * 
 * @author Martin Gregor
 * 
 */
public final class DateGetterHelper {

    /**
     * Private Constructor.
     */
    private DateGetterHelper() {
        super();
    }

    /**
     * looks up for a date in the URL
     * 
     * @param url
     * @return a extracted Date
     */
    public static ExtractedDate getURLDate(final String url) {
        ExtractedDate date = null;
        final Object[] regExpArray = { RegExp.DATE_ISO8601_YMD, RegExp.DATE_ISO8601_YM, RegExp.DATE_ISO8601_YWD,
                RegExp.DATE_ISO8601_YW, RegExp.DATE_ISO8601_YD, RegExp.URL_DATE_D, RegExp.URL_DATE };
        int index = 0;
        while (date == null && index < regExpArray.length) {
            date = getDateFromString(url, (String[]) regExpArray[index]);
            index++;
        }
        /*
         * date = getDateFromString(url, RegExp.DATE_ISO8601_YMD);
         * if (date == null) {
         * date = getDateFromString(url, RegExp.DATE_ISO8601_YM);
         * if (date == null) {
         * date = getDateFromString(url, RegExp.DATE_ISO8601_YWD);
         * if (date == null) {
         * date = getDateFromString(url, RegExp.DATE_ISO8601_YW);
         * if (date == null) {
         * date = getDateFromString(url, RegExp.DATE_ISO8601_YD);
         * if (date == null) {
         * date = getDateFromString(url, RegExp.URL_DATE_D);
         * if (date == null) {
         * date = getDateFromString(url, RegExp.URL_DATE);
         * }
         * }
         * }
         * }
         * }
         * }
         */
        return date;
    }

    /**
     * Extracts date form HTTP-header, that is written in "Last-Modified"-tag.
     * 
     * @param url
     * @return The extracted Date.
     */
    public static ExtractedDate getHTTPHeaderDate(final String url) {
        final Crawler crawler = new Crawler();
        final Map<String, List<String>> headers = crawler.getHeaders(url);
        ExtractedDate date = null;
        final Object[] regExpArray = { RegExp.DATE_RFC_1036, RegExp.DATE_RFC_1123, RegExp.DATE_ANSI_C };

        if (headers.containsKey("Last-Modified")) {
            final List<String> dateList = headers.get("Last-Modified");
            final Iterator<String> dateListIterator = dateList.iterator();
            while (dateListIterator.hasNext()) {
                final String dateString = dateListIterator.next().toString();
                int index = 0;
                while (date == null && index < regExpArray.length) {
                    date = getDateFromString(dateString, (String[]) regExpArray[index]);
                    index++;
                }
                /*
                 * date = getDateFromString(dateString, RegExp.DATE_RFC_1036);
                 * if (date == null) {
                 * date = getDateFromString(dateString, RegExp.DATE_RFC_1123);
                 * if (date == null) {
                 * date = getDateFromString(dateString, RegExp.DATE_ANSI_C);
                 * }
                 * }
                 */
            }
        }
        return date;
    }

    /**
     * Finds out the separating symbol of date-string
     * 
     * @param date
     * @return
     */
    public static String getSeparator(final ExtractedDate date) {
        final String dateString = date.getDateString();
        return getSeparator(dateString);
    }

    /**
     * 
     * @param text a date, where year, month and day are separated by . / or _
     * @return the separating symbol
     */
    public static String getSeparator(final String text) {
        String separator = null;

        int index = text.indexOf('.');
        if (index == -1) {
            index = text.indexOf('/');
            if (index == -1) {
                index = text.indexOf('_');
                if (index == -1) {
                    index = text.indexOf('-');
                    if (index != -1) {
                        separator = "-";
                    }
                } else {
                    separator = "_";
                }
            } else {
                separator = "/";
            }
        } else {
            separator = "\\.";
        }
        return separator;
    }

    /**
     * 
     * @param string string, which is to be searched
     * @param regExp regular expression for search
     * @param offsetStart is slider for beginning substring (no negative values) - e.g. substring: "abcd" offsetStart=0:
     *            "abcd" offsetStart=1: "bcd" offsetStart=-1: "abcd"
     * @return found substring or null
     */
    public static ExtractedDate getDateFromString(final String text, final String[] regExp) {

        ExtractedDate date = null;
        Pattern pattern;
        Matcher matcher;

        pattern = Pattern.compile(regExp[0]);
        matcher = pattern.matcher(text);
        if (matcher.find()) {
            final int start = matcher.start();
            final int end = matcher.end();
            date = new ExtractedDate(text.substring(start, end), regExp[1]);

        }
        return date;
    }

    /**
     * tries to find out what value is the year, month and day
     * 
     * @param array a array with three values unordered year, month and day
     * @return ordered array with year, month and day
     * 
     */
    public static ExtractedDate getDateparts(final ExtractedDate date) {

        return null;
    }

    /**
     * Adds a leading zero for numbers less then ten. <br>
     * E.g.: 3 ->"03"; 12 -> "12"; 386 -> "376" ...
     * 
     * @param number
     * @return a minimum two digit number
     */
    public static String get2Digits(final int number) {
        String numberString = String.valueOf(number);
        if (number < 10) {
            numberString = "0" + number;
        }
        return numberString;
    }

    /**
     * Sets the year in 4 digits format. <br>
     * E.g.: year = 12; current year = 2010 -> year > 10 -> 1912 <br>
     * year = 7; current year = 2010 -> year < 10 -> 2007 <br>
     * year = 10; current year = 2010 -> year > 10 -> 2010 <br>
     * year = 99; current year = 2010 -> year > 10 -> 1999
     * 
     * @param date
     * @return
     */
    public static int get4DigitYear(final int year) {
        int longYear = year;
        if (year < 100) {
            if (year > new GregorianCalendar().get(Calendar.YEAR) - 2000) {
                longYear = year + 1900;
            } else {
                longYear = year + 2000;
            }
        }
        return longYear;
    }

    /**
     * Removes the symbols "'" from Year '99 and "," from Day 03, June.
     * 
     * @param date
     * @return the entered date without the symbols
     */
    public static String removeNodigits(final String datePart) {
        String cleardString = datePart;
        int index;

        index = datePart.indexOf('\'');
        if (index == -1) {
            index = datePart.indexOf(',');
            if (index != -1) {
                cleardString = datePart.substring(0, index);
            }
        } else {

            cleardString = datePart.substring(index + 1, datePart.length());
        }

        return cleardString;
    }

    /**
     * Normalizes a year. Removes apostrophe (e.g. '99) and makes it four digit.
     * 
     * @param year
     * @return A four digit year.
     */
    public static int normalizeYear(final String year) {
        return get4DigitYear(Integer.parseInt(removeNodigits(year)));
    }

    /**
     * convert month-name in a number; January is 01..
     * 
     * @param month
     * @return month-number as string
     */
    public static String getMonthNumber(final String month) {
        String monthNumber = null;
        if (month.equalsIgnoreCase("january") || month.equalsIgnoreCase("januar") || month.equalsIgnoreCase("jan")) {
            monthNumber = "01";
        } else if (month.equalsIgnoreCase("february") || month.equalsIgnoreCase("februar")
                || month.equalsIgnoreCase("feb")) {
            monthNumber = "02";
        } else if (month.equalsIgnoreCase("march") || month.equalsIgnoreCase("märz") || month.equalsIgnoreCase("mar")) {
            monthNumber = "03";
        } else if (month.equalsIgnoreCase("april") || month.equalsIgnoreCase("apr")) {
            monthNumber = "04";
        } else if (month.equalsIgnoreCase("may") || month.equalsIgnoreCase("mai") || month.equalsIgnoreCase("may")) {
            monthNumber = "05";
        } else if (month.equalsIgnoreCase("june") || month.equalsIgnoreCase("juni") || month.equalsIgnoreCase("jun")) {
            monthNumber = "06";
        } else if (month.equalsIgnoreCase("july") || month.equalsIgnoreCase("juli") || month.equalsIgnoreCase("jul")) {
            monthNumber = "07";
        } else if (month.equalsIgnoreCase("august") || month.equalsIgnoreCase("aug")) {
            monthNumber = "08";
        } else if (month.equalsIgnoreCase("september") || month.equalsIgnoreCase("sep")) {
            monthNumber = "09";
        } else if (month.equalsIgnoreCase("october") || month.equalsIgnoreCase("oktober")
                || month.equalsIgnoreCase("oct")) {
            monthNumber = "10";
        } else if (month.equalsIgnoreCase("november") || month.equalsIgnoreCase("nov")) {
            monthNumber = "11";
        } else if (month.equalsIgnoreCase("december") || month.equalsIgnoreCase("dezember")
                || month.equalsIgnoreCase("dec")) {
            monthNumber = "12";
        }
        return monthNumber;
    }

}
