package ws.palladian.helper.date;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.dates.DateParser;
import ws.palladian.helper.date.dates.ExtractedDate;

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
     * Adds a leading zero for numbers less then ten. <br>
     * E.g.: 3 ->"03"; 12 -> "12"; 386 -> "376" ...
     * 
     * @param number
     * @return a minimum two digit number
     */
    public static String get2Digits(int number) {
        String numberString = String.valueOf(number);
        if (number < 10) {
            numberString = "0" + number;
        }
        return numberString;
    }

    /**
     * <p>
     * Creates an {@link ExtractedDate} with current date and time in UTC time zone. Thereby format
     * <code>YYYY-MM-DDTHH:MM:SSZ</code> is used.
     * </p>
     * 
     * @return An {@link ExtractedDate} initialized to current date and time.
     */
    public static ExtractedDate getCurrentDate() {
        return createCurrentDate(null);

    }

    /**
     * <p>
     * Creates an {@link ExtractedDate} with current date and time in specified time zone. Thereby format
     * <code>YYYY-MM-DDTHH:MM:SSZ</code> is used.
     * </p>
     * 
     * @param locale The locale specifying the time zone. <code>null</code> signifies to use UTC time zone.
     * @return An {@link ExtractedDate} initialized to current date and time in the specified time zone.
     */
    public static ExtractedDate createCurrentDate(Locale locale) {
        Calendar cal;
        if (locale != null) {
            cal = new GregorianCalendar(locale);
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

        // return new ExtractedDate(dateString, format);
        return DateParser.parse(dateString, format);

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

        //ExtractedDate date = new ExtractedDate(dateString, format);
        //return date;
        return DateParser.parse(dateString, format);

    }




}
