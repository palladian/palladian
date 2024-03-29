package ws.palladian.helper.date;

import ws.palladian.helper.constants.RegExp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * <p>
 * This class helps to transform and help with dates.
 * </p>
 *
 * @author David Urbansky
 * @author Sandro Reichert
 */
public class DateHelper {
    /**
     * Maximum allowed year for {@link #validateYear(Date)}.
     */
    private static final int MAX_YEAR = 9999;

    public static boolean containsDate(String searchString) {
        try {
            Pattern pattern = Pattern.compile(RegExp.DATE_ALL);
            return pattern.matcher(searchString).find();
        } catch (PatternSyntaxException e) {
            throw new IllegalStateException("Error compiling the date RegEx");
        }
    }

    public static String getCurrentDatetime(String format) {
        return getDatetime(format, System.currentTimeMillis());
    }

    public static String getDatetime(long timestamp) {
        return getDatetime("yyyy-MM-dd HH:mm:ss", timestamp);
    }

    public static String getDatetime(String format, long timestamp) {
        // 2013-09-19 : Removed LocalizeHelper.setLocaleEnglish() and LocalizeHelper.restoreLocale();
        // (the other methods were commented already). These are leftovers from Sandro and nobody
        // understands why they are necessary. I checked this method with and without the LocalizeHelper,
        // and for me, the results are equal. -- Philipp

        // LocalizeHelper.setLocaleEnglish(); // removed 2013-09-19
        // LocalizeHelper.setUTC();

        DateFormat dfm = new SimpleDateFormat(format, Locale.ENGLISH);
        String dateTime = dfm.format(new Date(timestamp));

        // LocalizeHelper.restoreLocale(); // removed 2013-09-19
        // LocalizeHelper.restoreTimeZone();
        return dateTime;
    }

    /**
     * <p>
     * Get the number of hours, minutes, seconds, or milliseconds that passed on the given day from midnight.
     * </p>
     *
     * @param date       The date of the day including time.
     * @param resolution The resolution (Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND or Calendar.MILLISECOND)
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
     * <p>
     * Return the current date as a string with the format "yyyy-MM-dd_HH-mm-ss".
     * </p>
     *
     * @return The date as a string.
     */
    public static String getCurrentDatetime() {
        return getCurrentDatetime("yyyy-MM-dd_HH-mm-ss");
    }

    /**
     * <p>
     * Convert the supplied month name to a number.
     * </p>
     *
     * @param monthName The month name to convert to number, not <code>null</code>.
     * @return The number for the month name, or <code>-1</code> if no month was recognized.
     */
    public static int monthNameToNumber(String monthName) {
        String month = monthName;
        month = month.replaceAll(",", "");
        month = month.replaceAll("\\.", "");
        month = month.replaceAll(" ", "");
        month = month.toLowerCase();
        int monthNumber = -1;
        switch (month) {
            case "january":
            case "januar":
            case "jan":
                monthNumber = 1;
                break;
            case "february":
            case "februar":
            case "feb":
                monthNumber = 2;
                break;
            case "march":
            case "märz":
            case "mär":
            case "mar":
                monthNumber = 3;
                break;
            case "april":
            case "apr":
                monthNumber = 4;
                break;
            case "may":
            case "mai":
                monthNumber = 5;
                break;
            case "june":
            case "juni":
            case "jun":
                monthNumber = 6;
                break;
            case "july":
            case "juli":
            case "jul":
                monthNumber = 7;
                break;
            case "august":
            case "aug":
                monthNumber = 8;
                break;
            case "september":
            case "sep":
            case "sept":
                monthNumber = 9;
                break;
            case "october":
            case "oktober":
            case "oct":
            case "okt":
                monthNumber = 10;
                break;
            case "november":
            case "nov":
                monthNumber = 11;
                break;
            case "december":
            case "dezember":
            case "dec":
            case "dez":
                monthNumber = 12;
                break;
        }
        return monthNumber;
    }

    /**
     * <p>
     * Returns the time that passed since the start time.
     * </p>
     *
     * @param startTime A timestamp.
     * @return The passed time since the time of the timestamp. The format is Hh:Mm:Ss:YYYms.
     */
    public static String formatDuration(long startTime) {
        return formatDuration(startTime, System.currentTimeMillis(), true);
    }

    public static String formatDuration(long startTime, long stopTime) {
        return formatDuration(startTime, stopTime, true);
    }

    /**
     * <p>
     * Returns the time that passed since the start time.
     * </p>
     *
     * @param startTime A timestamp.
     * @param compact   Whether the output should be compact like "Hh:Mm:Ss:YYYms" or readable like
     *                  "3 hours and 32 minutes".
     * @return The passed time since the time of the timestamp.
     */
    public static String formatDuration(long startTime, long stopTime, boolean compact) {
        long seconds = (stopTime - startTime) / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        seconds = seconds % 3600;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        long millis = (stopTime - startTime) % 1000;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            if (compact) {
                sb.append(days).append("d:");
            } else {
                if (days > 1) {
                    sb.append(days).append(" days ");
                } else {
                    sb.append(days).append(" day ");
                }
            }
        }
        if (hours > 0 || days > 0) {
            if (compact) {
                sb.append(hours).append("h:");
            } else {
                if (hours > 1) {
                    sb.append(hours).append(" hours ");
                } else if (hours == 1) {
                    sb.append(hours).append(" hour ");
                }
            }
        }
        if (hours > 0 || minutes > 0) {
            if (compact) {
                sb.append(minutes).append("m:");
            } else {
                if (minutes > 1) {
                    sb.append(minutes).append(" minutes ");
                } else if (minutes == 1) {
                    sb.append(minutes).append(" minute ");
                }
            }
        }
        if (hours > 0 || minutes > 0 || seconds > 0) {
            if (compact) {
                sb.append(seconds).append("s:");
            } else {
                if (seconds > 1) {
                    sb.append(seconds).append(" seconds ");
                } else if (seconds == 1) {
                    sb.append(seconds).append(" second ");
                }
            }
        }

        if (compact) {
            sb.append(millis).append("ms");
        } else {
            if (millis > 1) {
                sb.append(millis).append(" milliseconds");
            } else if (millis == 1) {
                sb.append(millis).append(" millisecond");
            }
        }

        String timeString = sb.toString().trim();

        if (!compact) {
            timeString = timeString.replaceAll("\\s(?=\\d)", ", ");
            int li = timeString.lastIndexOf(", ");
            if (li > -1) {
                timeString = timeString.substring(0, li) + " and " + timeString.substring(li + 2);
            }
        }

        return timeString;
    }

    /**
     * <p>
     * Get interval in millisecond between two dates. Dates are not checked correct order: in case intervalStartTime >
     * intervalStopTime, a negative value is returned. In case date(s) are <code>null</code>, 0 is returned.
     * </p>
     *
     * @param intervalStartTime the older date.
     * @param intervalStopTime  the newer date.
     * @return interval in millisecond between two Dates. In case date(s) are <code>null</code>, 0 is returned.
     */
    public static long getIntervalLength(Date intervalStartTime, Date intervalStopTime) {
        long intervalLength = 0;
        if (intervalStartTime != null && intervalStopTime != null) {
            intervalLength = intervalStopTime.getTime() - intervalStartTime.getTime();
        }
        return intervalLength;
    }

    public static String getTimeString(long time) {
        return formatDuration(0, time, true);
    }

    /**
     * <p>
     * Checks whether a date's year exceeds the given maximum. Useful to store a date in a mysql database since the
     * maximum value of the DATETIME type is the year 9999.
     * </p>
     *
     * @param date date to check.
     * @return The given date if it's year <= maxYear or <code>null</code> if date == null or its year > maxYear.
     */
    public static Date validateYear(Date date) {
        Date validatedDate = date;
        if (date != null) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            int year = cal.get(Calendar.YEAR);
            if (year >= MAX_YEAR) {
                validatedDate = null;
            }
        }
        return validatedDate;
    }

    public static long getMillisecondsToNextDay() {
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.DATE, 1);
        instance.set(Calendar.HOUR_OF_DAY, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.MILLISECOND, 0);
        return instance.getTimeInMillis() - System.currentTimeMillis();
    }

    public static void main(String[] t) {
        System.out.println(getDatetime(2832837283728L));
        // without LocalizeHelper: 2059-10-08 13:14:43
        // with LocalizeHelper: 2059-10-08 13:14:43
        System.exit(666);

        System.out.println(DateHelper.formatDuration(0, 10805000, false));
        System.out.println(DateHelper.formatDuration(0, 10850000));
        System.out.println(DateHelper.formatDuration(0, 10878512));
        System.out.println(DateHelper.formatDuration(0, 10878512, false));
        System.out.println(DateHelper.getCurrentDatetime());
        System.out.println(getTimeString(-1));
        System.out.println(getCurrentDatetime("yyyy-MM-dd HH:mm:ss"));
        System.out.println(getCurrentDatetime());
        System.out.println(getDatetime("dd.MM.yyyy", 1274313600000L));
        /*
         * long t1 = System.currentTimeMillis(); for (int i = 0; i < 94353; i++) { System.out.println("."); }
         * DateHelper.getRuntime(t1,true);
         */
    }
}