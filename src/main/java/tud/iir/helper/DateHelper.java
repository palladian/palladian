package tud.iir.helper;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.RegExp;
import tud.iir.normalization.DateNormalizer;

// TODO Move to Extraction package
/**
 * This class helps to transform and help with dates.
 * 
 * @author David Urbansky
 */
public class DateHelper {

    // shortcuts to the number of milliseconds of certain time spans
    public static final long SECOND_MS = 1000;
    public static final long MINUTE_MS = 60 * SECOND_MS;
    public static final long HOUR_MS = 60 * MINUTE_MS;
    public static final long DAY_MS = 24 * HOUR_MS;
    public static final long WEEK_MS = 7 * DAY_MS;
    public static final long MONTH_MS = 30 * DAY_MS;
    public static final long YEAR_MS = 365 * DAY_MS;

    public static boolean containsDate(String searchString) {
        Pattern pat = null;
        try {
            pat = Pattern.compile(RegExp.getRegExp(Attribute.VALUE_DATE));
        } catch (PatternSyntaxException e) {
            org.apache.log4j.Logger.getRootLogger().error(
                    "PatternSyntaxException for " + searchString + " with regExp " + RegExp.getRegExp(Attribute.VALUE_DATE), e);
            return false;
        }
        Matcher m = pat.matcher(searchString);
        if (m.find()) {
            return true;
        }
        return false;
    }

    public static String getCurrentDatetime(String format) {
        return getDatetime(format, System.currentTimeMillis());
    }

    public static String getDatetime(String format, long timestamp) {
        Locale.setDefault(Locale.ENGLISH);
        // TimeZone.setDefault(TimeZone.getTimeZone("Etc/UTC"));
        DateFormat dfm = new SimpleDateFormat(format);
        return dfm.format(new Date(timestamp));
    }

    /**
     * Get the number of hours, minutes, seconds, or milliseconds that passed on the given day from midnight.
     * 
     * @param date The date of the day including time.
     * @param resolution The resolution (Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND or Calendar.MILLISECOND)
     * 
     * @return A positive number of the passed time.
     */
    public static long getTimeOfDay(Date date, int resolution) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE) + 60 * hours;
        int seconds = calendar.get(Calendar.SECOND) + 60 * minutes;
        int milliseconds = calendar.get(Calendar.MILLISECOND) + 1000 * seconds;

        switch (resolution) {
            case Calendar.HOUR:
                return hours;
            case Calendar.MINUTE:
                return minutes;
            case Calendar.SECOND:
                return seconds;
            case Calendar.MILLISECOND:
                return milliseconds;
        }

        return milliseconds;
    }

    public static long getTimeOfDay(long timestamp, int resolution) {
        return getTimeOfDay(new Date(timestamp), resolution);
    }

    /**
     * Return the current date as a string with the format "yyyy-MM-dd_HH-mm-ss".
     * 
     * @return The date as a string.
     */
    public static String getCurrentDatetime() {
        return getCurrentDatetime("yyyy-MM-dd_HH-mm-ss");
    }

    public static String monthNameToNumber(String monthName) {
        monthName = monthName.toLowerCase().trim();

        if (monthName.equals("january") || monthName.equals("jan")) {
            return "01";
        } else if (monthName.equals("february") || monthName.equals("feb")) {
            return "02";
        } else if (monthName.equals("march") || monthName.equals("mar")) {
            return "03";
        } else if (monthName.equals("april") || monthName.equals("apr")) {
            return "04";
        } else if (monthName.equals("may")) {
            return "05";
        } else if (monthName.equals("june") || monthName.equals("jun")) {
            return "06";
        } else if (monthName.equals("july") || monthName.equals("jul")) {
            return "07";
        } else if (monthName.equals("august") || monthName.equals("aug")) {
            return "08";
        } else if (monthName.equals("september") || monthName.equals("sep")) {
            return "09";
        } else if (monthName.equals("october") || monthName.equals("oct")) {
            return "10";
        } else if (monthName.equals("november") || monthName.equals("nov")) {
            return "11";
        } else if (monthName.equals("december") || monthName.equals("dec")) {
            return "12";
        }

        // no valid month name given
        return "";
    }

    /**
     * Returns the time that passed since the start time.
     * 
     * @param startTime A timestamp.
     * @return The passed time since the time of the timestamp. The format is Hh:Mm:Ss:YYYms.
     */
    public static String getRuntime(long startTime) {
        return getRuntime(startTime, System.currentTimeMillis(), false);
    }

    public static String getRuntime(long startTime, long stopTime) {
        return getRuntime(startTime, stopTime, false);
    }

    public static String getRuntime(long startTime, long stopTime, boolean output) {
        long seconds = (stopTime - startTime) / 1000;
        long hours = seconds / 3600;
        seconds = seconds % 3600;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        long millis = (stopTime - startTime) % 1000;

        StringBuilder sb = new StringBuilder();

        if (hours > 0) {
            sb.append(hours).append("h:");
        }
        if (hours > 0 || minutes > 0) {
            sb.append(minutes).append("m:");
        }
        if (hours > 0 || minutes > 0 || seconds > 0) {
            sb.append(seconds).append("s:");
        }

        sb.append(millis).append("ms");

        if (output) {
            System.out.println(":::: runtime: " + sb);
        }

        return sb.toString();
    }

    public static String getTimeString(long time) {
        return getRuntime(0, time);
    }

    /**
     * Create the UNIX timestamp for the given date (UTC).
     * 
     * @param normalizedDate A date in normalized form: yyyy-MM-dd [hh:mm:ss[.f]]
     * @return The UNIX timestamp for that date.
     */
    public static long getTimestamp(String date) throws Exception {

        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("Etc/UTC"));

        String normalizedDate = "";

            normalizedDate = DateNormalizer.normalizeDate(date, true);

            return Timestamp.valueOf(normalizedDate).getTime();
        }

    
    public static void main(String[] t) {
        System.out.println(DateHelper.getCurrentDatetime());
        System.out.println(getTimeString(-1));
        System.out.println(getCurrentDatetime("yyyy-MM-dd HH:mm:ss"));
        System.out.println(getCurrentDatetime());
        System.out.println(getDatetime("dd.MM.yyyy", 1274313600000l));
        /*
         * long t1 = System.currentTimeMillis(); for (int i = 0; i < 94353; i++) { System.out.println("."); } DateHelper.getRuntime(t1,true);
         */
    }

}