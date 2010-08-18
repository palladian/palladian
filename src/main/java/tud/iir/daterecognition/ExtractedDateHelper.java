package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;

import tud.iir.knowledge.RegExp;

public class ExtractedDateHelper {

    /**
     * Same as printeDateArray() with filter of techniques. These are found in ExtracedDate as static properties. <br>
     * And a format, found as second value of RegExp.
     * 
     * @param <T>
     * 
     * @param dates
     * @param filterTechnique
     * @param format
     */
    public static <T> void printDateArray(ArrayList<T> dates, int filterTechnique, String format) {
        ArrayList<T> temp = dates;
        if (filterTechnique != 0) {
            System.out.println("enter filter");
            temp = filterDatesByTechnique(temp, filterTechnique);
        }

        Iterator<T> dateIterator = temp.iterator();
        while (dateIterator.hasNext()) {

            T date = dateIterator.next();
            if (format == null || format == ((ExtractedDate) date).getFormat()) {
                System.out.println(date.toString());
                System.out
                        .println("------------------------------------------------------------------------------------------------");
            }
        }
    }

    /**
     * Same as printeDateArray() with filter of techniques. These are found in ExtracedDate as static properties.
     * 
     * @param <T>
     * 
     * @param dates
     * @param filterTechnique
     */
    public static <T> void printDateArray(ArrayList<T> dates, int filterTechnique) {
        printDateArray(dates, filterTechnique, null);
    }

    /**
     * System.out.println for each date in dates, with some properties.
     * 
     * @param <T>
     * 
     * @param dates
     */
    public static <T> void printDateArray(ArrayList<T> dates) {
        printDateArray(dates, 0);
    }

    /**
     * convert month-name in a number; January is 01..
     * 
     * @param month
     * @return month-number as string
     */
    public static String getMonthNumber(String month) {
        month.replaceAll(" ", "");
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

    /**
     * Normalizes a year. Removes apostrophe (e.g. '99) and makes it four digit.
     * 
     * @param year
     * @return A four digit year.
     */
    public static int normalizeYear(final String year) {
        return ExtractedDateHelper.get4DigitYear(Integer.parseInt(ExtractedDateHelper.removeNodigits(year)));
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

        index = cleardString.indexOf(".");
        if (index != -1) {
            cleardString = cleardString.substring(0, index);
        }

        index = cleardString.indexOf("th");
        if (index == -1) {
            index = cleardString.indexOf("st");
            if (index == -1) {
                index = cleardString.indexOf("nd");
                if (index == -1) {
                    index = cleardString.indexOf("rd");
                }
            }
        }
        if (index != -1) {
            cleardString = cleardString.substring(0, index);
        }

        return cleardString;
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
     * Filters a List of extracted dates by its extraction technique.
     * 
     * @param <T>
     * 
     * @param dates
     * @param filter Found in ExtractedDate as static property.
     * @return
     */
    public static <T> ArrayList<T> filterDatesByTechnique(ArrayList<T> dates, int filter) {
        ArrayList<T> returnDates = new ArrayList<T>();
        Iterator<T> iterator = dates.iterator();

        while (iterator.hasNext()) {
            T date = iterator.next();
            if (((ExtractedDate) date).getType() == filter || filter == 0) {
                returnDates.add(date);
            }
        }

        return returnDates;
    }

    /**
     * Returns an array of dates, where every date has the specified datepart.(Is not -1)<br>
     * year = 1;
     * month =2;
     * day=3;
     * hour = 4;
     * minute = 5;
     * second=6;
     * 
     * 
     * @param filter
     * @return
     */
    public static ArrayList<ExtractedDate> filterDateswithDatepart(ArrayList<ExtractedDate> dates, int filter) {
        ArrayList<ExtractedDate> returnDates = new ArrayList<ExtractedDate>();
        Iterator<ExtractedDate> iterator = dates.iterator();

        while (iterator.hasNext()) {
            ExtractedDate date = iterator.next();
            if (date.get(filter) != -1) {
                returnDates.add(date);
            }
        }
        return returnDates;
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
     * Crates a extracted date with actual date and time in UTC timezone. <br>
     * Thereby format YYYY-MM-DDTHH:MM:SSZ is used.
     * 
     * @return Extracted date.
     */
    public static ExtractedDate createActualDate() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

        String dateString = cal.get(Calendar.YEAR) + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.MONTH))
                + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.DAY_OF_MONTH)) + "T"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.HOUR_OF_DAY)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.MINUTE)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.SECOND)) + "Z";
        String format = RegExp.DATE_ISO8601_YMD_T[1];

        ExtractedDate date = new ExtractedDate(dateString, format);
        return date;
    }

    /**
     * Removes trailing whitespace at the end.
     * 
     * @param dateString String to be cleared.
     * @return Cleared string.
     */
    public static String removeLastWhitespace(String dateString) {
        StringBuffer temp = new StringBuffer(dateString);

        while (temp.charAt(temp.length() - 1) == ' ') {
            temp.deleteCharAt(temp.length() - 1);
        }
        return temp.toString();
    }

}
