package ws.palladian.helper.date;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.dates.DateType;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.nlp.StringHelper;

/**
 * This class contains methods, that help ExtractedDate to define the correct date values.
 * 
 * @author Martin Gregor
 * 
 */
public class ExtractedDateHelper {

    /**
     * Convert month-name in a number; January is 01..
     * TODO somewhat duplicate to {@link DateHelper#monthNameToNumber(String)}
     * 
     * @param month
     * @return month-number as string
     */
    public static String getMonthNumber(String monthString) {
        String month = monthString;
        month = month.replaceAll(",", "");
        month = month.replaceAll("\\.", "");
        month = month.replaceAll(" ", "");
        String monthNumber = null;
        if (month.equalsIgnoreCase("january") || month.equalsIgnoreCase("januar") || month.equalsIgnoreCase("jan")) {
            monthNumber = "01";
        } else if (month.equalsIgnoreCase("february") || month.equalsIgnoreCase("februar")
                || month.equalsIgnoreCase("feb")) {
            monthNumber = "02";
        } else if (month.equalsIgnoreCase("march") || month.equalsIgnoreCase("märz") || month.equalsIgnoreCase("mär")
                || month.equalsIgnoreCase("mar")) {
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
        } else if (month.equalsIgnoreCase("september") || month.equalsIgnoreCase("sep")
                || month.equalsIgnoreCase("sept")) {
            monthNumber = "09";
        } else if (month.equalsIgnoreCase("october") || month.equalsIgnoreCase("oktober")
                || month.equalsIgnoreCase("oct") || month.equalsIgnoreCase("okt")) {
            monthNumber = "10";
        } else if (month.equalsIgnoreCase("november") || month.equalsIgnoreCase("nov")) {
            monthNumber = "11";
        } else if (month.equalsIgnoreCase("december") || month.equalsIgnoreCase("dezember")
                || month.equalsIgnoreCase("dec") || month.equalsIgnoreCase("dez")) {
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
        if (index != -1) {
            cleardString = datePart.substring(index + 1, datePart.length());
        }
        index = datePart.indexOf(',');
        if (index != -1) {
            cleardString = datePart.substring(0, index);
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
        
        // remove everything after a break
        cleardString = cleardString.replaceAll("\n.*","");

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
        return createActualDate(null);

    }

    /**
     * Creates an ExtrextedDate with current timestamp as date.
     * 
     * @param local
     * @return
     */
    public static ExtractedDate createActualDate(Locale local) {
        Calendar cal;
        if (local != null) {
            cal = new GregorianCalendar(local);
        } else {
            cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        }

        // cal.getMonth + 1 = month, because in Calendar Jan has number 0.
        String dateString = cal.get(Calendar.YEAR) + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.MONTH) + 1)
                + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.DAY_OF_MONTH)) + "T"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.HOUR_OF_DAY)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.MINUTE)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.SECOND)) + "Z";
        String format = RegExp.DATE_ISO8601_YMD_T[1];

        ExtractedDate date = new ExtractedDate(dateString, format);
        return date;

    }
    
    /**
     * Creates an ExtrextedDate with given timestamp as date.
     * 
     * @param time timestamp in millisec
     * @return
     */
    public static ExtractedDate createDate(long time) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(time);
        

        // cal.getMonth + 1 = month, because in Calendar Jan has number 0.
        String dateString = cal.get(Calendar.YEAR) + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.MONTH) + 1)
                + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.DAY_OF_MONTH)) + "T"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.HOUR_OF_DAY)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.MINUTE)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.SECOND)) + "Z";
        String format = RegExp.DATE_ISO8601_YMD_T[1];

        ExtractedDate date = new ExtractedDate(dateString, format);
        return date;

    }

    /**
     * Removes timezone acronyms.
     * 
     * @param dateString
     * @return
     */
    public static String[] removeTimezone(String dateString) {
        String timezoneRegRex = RegExp.getTimezones();
        return StringHelper.removeFirstStringpart(dateString, timezoneRegRex);
    }

    /**
     * Returns a extracted date type in a human readable string.
     * 
     * @param typ
     * @return
     */
    public static String getTypString(DateType type) {
        String typeString;
        switch (type) {
            case ArchiveDate:
                typeString = "archive";
                break;
            case UrlDate:
                typeString = "URL";
                break;
            case MetaDate:
                typeString = "Meta";
                break;
            case StructureDate:
                typeString = "HTML structure";
                break;
            case ContentDate:
                typeString = "HTML content";
                break;
            case ReferenceDate:
                typeString = "reference";
                break;
            default:
                typeString = "other";
        }
        return typeString;

    }
}
