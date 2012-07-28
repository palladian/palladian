package ws.palladian.helper.normalization;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateHelper;

/**
 * The DateNormalizer normalizes dates.
 */
public class DateNormalizer {

    public static String normalizeDateFormat(Date date, String format) {
        // Locale.setDefault(Locale.ENGLISH);
        Calendar c = Calendar.getInstance(Locale.ENGLISH);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        c.setTime(date);
        String dateString = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DATE) + " "
        + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);
        return normalizeDateFormat(dateString, format);
    }

    private static String normalizeDateFormat(String dateString, String format) {
        String normalizedDate = "";

        // Locale.setDefault(Locale.ENGLISH);
        DateFormat dfm = new SimpleDateFormat(format, Locale.ENGLISH);

        try {
            Date rfcDate = dfm.parse(dateString);
            Calendar c = Calendar.getInstance();
            c.setTimeZone(TimeZone.getTimeZone("UTC"));
            c.setTime(rfcDate);

            String year = String.valueOf(c.get(Calendar.YEAR));
            String month = String.valueOf(c.get(Calendar.MONTH) + 1);
            String date = String.valueOf(c.get(Calendar.DATE));
            String hour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
            String minute = String.valueOf(c.get(Calendar.MINUTE));
            String second = String.valueOf(c.get(Calendar.SECOND));

            if (month.length() < 2) {
                month = "0" + month;
            }
            if (date.length() < 2) {
                date = "0" + date;
            }
            if (hour.length() < 2) {
                hour = "0" + hour;
            }
            if (minute.length() < 2) {
                minute = "0" + minute;
            }
            if (second.length() < 2) {
                second = "0" + second;
            }

            normalizedDate = year + "-" + month + "-" + date + " " + hour + ":" + minute + ":" + second;

        } catch (ParseException e) {
            Logger.getRootLogger().debug(format + " could not be parsed for " + dateString);
        }

        return normalizedDate;
    }

    /**
     * Normalize a given date to the format YYYY-MM-DD (UTC standard).
     * 
     * @param dateString The date string.
     * @return The normalized date in UTC standard.
     * @deprecated Replaced by date recognition techniques.
     */
    @Deprecated
    public static String normalizeDate(String dateString) {
        return normalizeDate(dateString, false);
    }

    /**
     * @deprecated Replaced by date recognition techniques.
     */
    @Deprecated
    public static String normalizeDate(String dateString, boolean fillTime) {
        String normalizedDate = "";

        // try to match RFC 2822 format (E, dd MMM yyyy HH:mm:ss Z)
        normalizedDate = normalizeDateFormat(dateString, "E, dd MMM yyyy HH:mm:ss Z");
        if (normalizedDate.length() > 0) {
            return normalizedDate;
        }

        // try RFC 2822 without seconds
        normalizedDate = normalizeDateFormat(dateString, "E, dd MMM yyyy HH:mm Z");
        if (normalizedDate.length() > 0) {
            return normalizedDate;
        }

        normalizedDate = normalizeDateFormat(dateString, "dd MMM yyyy HH:mm:ss Z");
        if (normalizedDate.length() > 0) {
            return normalizedDate;
        }

        normalizedDate = normalizeDateFormat(dateString, "E MMM dd HH:mm:ss Z yyyy");
        if (normalizedDate.length() > 0) {
            return normalizedDate;
        }

        // match possible date patterns to see which format the data is
        Pattern pattern;
        Matcher matcher;

        // match date1 pattern YYYY-MM-DD hh:mm:ss
        pattern = Pattern.compile(RegExp.DATE0);
        matcher = pattern.matcher(dateString);
        if (matcher.find()) {
            return dateString; // already correct format
        }

        // match date1 pattern YYYY-MM-DD
        pattern = Pattern.compile(RegExp.DATE1);
        matcher = pattern.matcher(dateString);
        if (matcher.find()) {
            if (fillTime) {
                dateString += " 00:00:00";
            }
            return dateString; // already correct format
        }

        // match date2 pattern DD.MM.YYYY, numbers are separated by . / or -
        pattern = Pattern.compile(RegExp.DATE2);
        matcher = pattern.matcher(dateString);
        if (matcher.find()) {
            String separator = "";

            int index = dateString.indexOf(".");
            if (index == -1) {
                index = dateString.indexOf("/");
                if (index == -1) {
                    index = dateString.indexOf("-");
                    if (index == -1) {
                        if (fillTime) {
                            dateString += " 00:00:00";
                        }
                        return dateString;
                    } else {
                        separator = "-";
                    }
                } else {
                    separator = "/";
                }
            } else {
                separator = "\\.";
            }

            String[] numbers = dateString.split(separator);
            if (numbers.length < 3) {
                return dateString;
            }

            String day = numbers[0].trim();
            String month = numbers[1].trim();
            String year = numbers[2].trim();

            if (day.length() < 2) {
                day = "0" + day;
            }
            if (month.length() < 2) {
                month = "0" + month;
            }
            if (year.length() < 4) {
                year = "19" + year;
            }

            dateString = year + "-" + month + "-" + day;
            if (fillTime) {
                dateString += " 00:00:00";
            }
            return dateString;
        }

        // match date3 pattern DD Monthname YYYY, values separated by space
        pattern = Pattern.compile(RegExp.DATE3);
        matcher = pattern.matcher(dateString);
        if (matcher.find()) {

            dateString = matcher.group();
            dateString = dateString.replace(".", " ");
            dateString = dateString.replace("  ", " ");

            String[] values = dateString.split(" ");
            if (values.length < 3) {
                return dateString;
            }

            String day = values[0].replace("th", "").trim();
            String month = String.format("%02d", DateHelper.monthNameToNumber(values[1].replace(",", "")));
            String year = values[2].replace("'", "").trim();

            if (day.length() < 2) {
                day = "0" + day;
            }
            if (year.length() < 4) {
                year = "19" + year;
            }

            dateString = year + "-" + month + "-" + day;
            if (fillTime) {
                dateString += " 00:00:00";
            }
            return dateString;
        }

        // match date3 pattern Monthname DD YYYY, values separated by space
        pattern = Pattern.compile(RegExp.DATE4);
        matcher = pattern.matcher(dateString);
        if (matcher.find()) {

            dateString = dateString.replace(",", " ");
            dateString = dateString.replace("  ", " ");

            String[] values = dateString.split(" ");
            if (values.length < 3) {
                return dateString;
            }

            String day = values[1].replace("th", "").trim();
            String month = String.format("%02d", DateHelper.monthNameToNumber(values[0]));
            String year = values[2].replace("'", "").trim();

            if (day.length() < 2) {
                day = "0" + day;
            }
            if (year.length() < 4) {
                year = "19" + year;
            }

            dateString = year + "-" + month + "-" + day;
            if (fillTime) {
                dateString += " 00:00:00";
            }
            return dateString;
        }

        Logger.getRootLogger().error("date " + dateString + " could not be normalized");


        return normalizedDate;
    }

    public static void main(String[] args) throws Exception {
        // System.out.println(normalizeDate("10 Oct 2008 16:34:01 EST"));
        // System.out.println(DateNormalizer.normalizeDateFormat("Thu Feb 12 01:56:22 CET 2009",
        // "yyyy-MM-dd HH:mm:ss"));
        System.out.println(DateNormalizer.normalizeDateFormat("Thu Feb 12 01:56:22 CET 2009",
        "E MMM dd HH:mm:ss Z yyyy"));

        // System.out.println(DateNormalizer.normalizeDateFormat(new
        // Date(System.currentTimeMillis()),"yyyy-MM-dd HH:mm:ss"));
        System.out.println(DateNormalizer.normalizeDate("Thu Feb 12 01:56:22 CET 2009", true));

        System.out.println(DateNormalizer.normalizeDate("03.05.2010", false));
        System.out.println(DateHelper.getTimestamp(DateNormalizer.normalizeDate("03.05.2010", false)));

    }
}