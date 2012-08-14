package ws.palladian.helper.date;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.helper.DateFormat;
import ws.palladian.helper.RegExp;

/**
 * <p>
 * This class contains the background magic which is carrying out the parsing of dates. For each {@link DateFormat}
 * specified in {@link RegExp}, dedicated parsing logic is implemented here. This class is not thread-safe and not
 * intended to be used by the outside world. Instances are created by the {@link DateParser} only and discarded
 * immediately, after an {@link ExtractedDate} has been constructed.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
final class DateParserLogic {

    final String originalDateString;
    final DateFormat format;

    int year;
    int month;
    int day;
    int hour;
    int minute;
    int second;
    String timeZone;

    /**
     * <p>
     * Initialize the date parse logic with the provided date string and the given format used for parsing.
     * </p>
     * 
     * @param dateString The date string to be parsed, not <code>null</code>.
     * @param dateFormat The format describing the structure of the date string, not <code>null</code>.
     */
    public DateParserLogic(String dateString, DateFormat dateFormat) {
        this.originalDateString = dateString;
        this.format = dateFormat;
        this.year = -1;
        this.month = -1;
        this.day = -1;
        this.hour = -1;
        this.minute = -1;
        this.second = -1;
        this.timeZone = null;
    }
    
    DateParserLogic() {
        this(null, null);
    }

    /**
     * <p>
     * Parse the supplied date string by using the given format.
     * </p>
     */
    void parse() {

        String dateString = originalDateString;
        String[] timeZoneSplit = splitTimeZone(dateString);
        if (timeZoneSplit != null) {
            dateString = timeZoneSplit[0];
            timeZone = timeZoneSplit[1];
        }

        if (format.equals(RegExp.DATE_ISO8601_YMD_T)) {
            String separator = "T";
            int index = dateString.indexOf(separator);
            if (index == -1) {
                separator = " ";
            }
            String[] temp = dateString.split(separator);
            setDateValues(temp[0].split(getSeparatorRegEx(temp[0])), 0, 1, 2);
            setTimeValues(temp[1]);
        } else if (format.equals(RegExp.DATE_ISO8601_YMD)) {
            setDateValues(dateString.split(getSeparatorRegEx(dateString)), 0, 1, 2);
        } else if (format.equals(RegExp.DATE_ISO8601_YM)) {
            setDateValues(dateString.split("-"), 0, 1, -1);
        } else if (format.equals(RegExp.DATE_ISO8601_YWD)) {
            setDateByWeekOfYear(dateString, true, true);
        } else if (format.equals(RegExp.DATE_ISO8601_YWD_T)) {
            String separator;
            if (dateString.contains("T")) {
                separator = "T";
            } else {
                separator = " ";
            }
            String[] dateParts = dateString.split(separator);
            setDateByWeekOfYear(dateParts[0], true, true);
            setTimeValues(dateParts[1]);
        } else if (format.equals(RegExp.DATE_ISO8601_YW)) {
            setDateByWeekOfYear(dateString, false, true);
        } else if (format.equals(RegExp.DATE_ISO8601_YD)) {
            setDateByDayOfYear(dateString, true);
        } else if (format.equals(RegExp.DATE_URL_D)) {
            setDateValues(dateString.split(getSeparatorRegEx(dateString)), 0, 1, 2);
        } else if (format.equals(RegExp.DATE_URL_MMMM_D)) {
            setDateValues(dateString.split("/"), 0, 1, 2);
        } else if (format.equals(RegExp.DATE_URL_SPLIT)) {
            String[] dateParts = dateString.split("/");
            int tempMonth = 0;
            try {
                year = normalizeYear(dateParts[0]);
                day = Integer.parseInt(dateParts[dateParts.length - 1]);
                tempMonth = -1;
            } catch (NumberFormatException e) {
                String lastField = dateParts[dateParts.length - 1];
                String[] tempDateParts = lastField.split(getSeparatorRegEx(lastField));
                month = Integer.parseInt(tempDateParts[0]);
                day = Integer.parseInt(tempDateParts[1]);
            }
            if (tempMonth == -1) {
                month = Integer.parseInt(dateParts[dateParts.length - 2]);
            }
        } else if (format.equals(RegExp.DATE_URL)) {
            setDateValues(dateString.split(getSeparatorRegEx(dateString)), 0, 1, -1);
        } else if (format.equals(RegExp.DATE_EU_D_MM_Y)) {
            String separator = getSeparatorRegEx(dateString);
            setDateValues(dateString.split(separator), 2, 1, 0);
        } else if (format.equals(RegExp.DATE_USA_MM_D_Y)) {
            setDateValues(dateString.split(getSeparatorRegEx(dateString)), 2, 0, 1);
        } else if (format.equals(RegExp.DATE_EU_D_MMMM_Y)) {
            dateString = dateString.replaceAll("\\.\\s?", " ");
            dateString = dateString.replaceAll("-", " ");
            String[] dateParts = dateString.split(" ");
            setDateValues(dateParts, 2, 1, 0);
        } else if (format.equals(RegExp.DATE_USA_MMMM_D_Y)) {
            dateString = dateString.replaceAll("\\,\\s|\\,|\\s", " ");
            // try {
                String[] parts = dateString.split(" ");
                if (parts.length == 2) {
                    String[] tempParts = new String[3];
                    tempParts[0] = parts[0].split("\\.")[0];
                    tempParts[1] = parts[0].split("\\.")[1];
                    tempParts[2] = parts[1];
                    parts = tempParts;
                }
                setDateValues(parts, 2, 0, 1);
            // } catch (Exception e) {
            // }
        } else if (format.equals(RegExp.DATE_USA_MMMM_D_Y_SEP)) {
            setDateValues(dateString.split("-"), 2, 0, 1);
        } else if (format.equals(RegExp.DATE_EUSA_MMMM_Y)) {
            setDateValues(dateString.split(" "), 1, 0, -1);
        } else if (format.equals(RegExp.DATE_EUSA_YYYY_MMM_D)) {
            setDateValues(dateString.split("-"), 0, 1, 2);
        } else if (format.equals(RegExp.DATE_EU_MM_Y)) {
            String separator = getSeparatorRegEx(dateString);
            setDateValues(dateString.split(separator), 1, 0, -1);
        } else if (format.equals(RegExp.DATE_EU_D_MM)) {
            String separator = getSeparatorRegEx(dateString);
            setDateValues(dateString.split(separator), -1, 1, 0);
        } else if (format.equals(RegExp.DATE_EU_D_MMMM)) {
            dateString = dateString.replaceAll("\\.", "");
            setDateValues(dateString.split(" "), -1, 1, 0);
        } else if (format.equals(RegExp.DATE_USA_MM_D)) {
            setDateValues(dateString.split("/"), -1, 0, 1);
        } else if (format.equals(RegExp.DATE_USA_MMMM_D)) {
            setDateValues(dateString.split(" "), -1, 0, 1);
        } else if (format.equals(RegExp.DATE_USA_MM_Y)) {
            setDateValues(dateString.split("/"), 1, 0, -1);
        } else if (format.equals(RegExp.DATE_ANSI_C)) {
            String[] dateParts = dateString.split(" ");
            setDateValues(dateParts, 4, 1, 2);
            setTimeValues(dateParts[3]);
        } else if (format.equals(RegExp.DATE_ANSI_C_TZ)) {
            String[] dateParts = dateString.split(" ");
            setDateValues(dateParts, 4, 1, 2);
            setTimeValues(dateParts[3] + dateParts[5]);
        } else if (format.equals(RegExp.DATE_RFC_1123)) {
            String[] dateParts = dateString.split(" ");
            setDateValues(dateParts, 3, 2, 1);
            setTimeValues(dateParts[4]);
        } else if (format.equals(RegExp.DATE_RFC_1036)) {
            String parts[] = dateString.split(" ");
            setDateValues(parts[1].split("-"), 2, 1, 0);
            setTimeValues(parts[2]);
        } else if (format.equals(RegExp.DATE_ISO8601_YMD_NO)) {
            year = Integer.parseInt(dateString.substring(0, 4));
            month = Integer.parseInt(dateString.substring(4, 6));
            day = Integer.parseInt(dateString.substring(6, 8));
        } else if (format.equals(RegExp.DATE_ISO8601_YWD_NO)) {
            setDateByWeekOfYear(dateString, true, false);
        } else if (format.equals(RegExp.DATE_ISO8601_YW_NO)) {
            setDateByWeekOfYear(dateString, false, false);
        } else if (format.equals(RegExp.DATE_ISO8601_YD_NO)) {
            setDateByDayOfYear(dateString, false);
        } else if (format.equals(RegExp.DATE_RFC_1123_UTC)) {
            String[] dateParts = dateString.split(" ");
            setDateValues(dateParts, 3, 2, 1);
            setTimeValues(dateParts[4] + dateParts[5]);
        } else if (format.equals(RegExp.DATE_RFC_1036_UTC)) {
            String parts[] = dateString.split(" ");
            setDateValues(parts[1].split("-"), 2, 1, 0);
            setTimeValues(parts[2] + parts[3]);
        } else if (format.equals(RegExp.DATE_EU_D_MM_Y_T)) {
            String meridiem = hasAmPm(dateString);
            if (meridiem != null) {
                dateString = removeAmPm(dateString, meridiem);
            }
            String[] parts = dateString.split(" ");
            String separator = getSeparatorRegEx(parts[0]);
            String[] date = parts[0].split(separator);
            setDateValues(date, 2, 1, 0);
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].contains("/")) {
                    builder.append(parts[i]);
                }
            }
            setTimeValues(builder.toString());
            set24h(meridiem);
        } else if (format.equals(RegExp.DATE_EU_D_MMMM_Y_T)) {
            String meridiem = hasAmPm(dateString);
            if (meridiem != null) {
                dateString = removeAmPm(dateString, meridiem);
            }
            if (dateString.contains("-")) {
                dateString = dateString.replaceAll("-", " ");
            }
            String[] parts = dateString.split(" ");
            setDateValues(parts, 2, 1, 0);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 3; i < parts.length; i++) {
                if (!parts[i].contains("/")) {
                    stringBuilder.append(parts[i]);
                }
            }
            setTimeValues(stringBuilder.toString());
            set24h(meridiem);
        } else if (format.equals(RegExp.DATE_USA_MM_D_Y_T)) {
            String meridiem = hasAmPm(dateString);
            if (meridiem != null) {
                dateString = removeAmPm(dateString, meridiem);
            }
            String[] parts = dateString.split(" ");
            String separator = getSeparatorRegEx(parts[0]);
            String[] date = parts[0].split(separator);
            setDateValues(date, 2, 0, 1);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].contains("/")) {
                    stringBuilder.append(parts[i]);
                }
            }
            setTimeValues(stringBuilder.toString());
            set24h(meridiem);
        } else if (format.equals(RegExp.DATE_USA_MMMM_D_Y_T)) {
            String meridiem = hasAmPm(dateString);
            if (meridiem != null) {
                dateString = removeAmPm(dateString, meridiem);
            }
            String[] parts = dateString.split(" ");
            setDateValues(parts, 2, 0, 1);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 3; i < parts.length; i++) {
                if (!parts[i].contains("/")) {
                    stringBuilder.append(parts[i]);
                }
            }
            setTimeValues(stringBuilder.toString());
            set24h(meridiem);
        } else if (format.equals(RegExp.DATE_CONTEXT_YYYY)) {
            year = Integer.valueOf(dateString);
        }
    }

    /**
     * <p>
     * Removes <code>AM</code> and <code>PM</code> from a date string and delete double whitespace.
     * </p>
     * 
     * @param dateString The date string to be cleared.
     * @param meridiem <code>AM</code> or <code>PM</code> to be removed from the string.
     * @return The string without the meridiem value, with double whitespace removed.
     */
    private static String removeAmPm(String dateString, String meridiem) {
        return dateString.replaceAll(meridiem, "").replaceAll("  ", " ");
    }

    /**
     * <p>
     * Checks for <code>AM</code> and <code>PM</code> in a string, and return the value if found.
     * </p>
     * 
     * @param dateString The date string to be checked.
     * @return <code>AM</code> or <code>PM</code> if found (or lower case), or <code>null</code>.
     */
    private static String hasAmPm(String dateString) {
        if (dateString.contains("am")) {
            return "am";
        }
        if (dateString.contains("AM")) {
            return "AM";
        }
        if (dateString.contains("pm")) {
            return "pm";
        }
        if (dateString.contains("PM")) {
            return "PM";
        }
        return null;
    }

    /**
     * <p>
     * Change the hour of the date to 24h system, if this date has an hour and the date string has <code>AM</code> or
     * <code>PM</code> values.
     * </p>
     * 
     * @param meridiem <code>AM</code> or <code>PM</code>
     */
    private void set24h(String meridiem) {
        if (hour == -1 || meridiem == null) {
            return;
        }
        if (meridiem.equalsIgnoreCase("pm") && 0 < hour && hour < 12) {
            hour += 12;
        } else if (meridiem.equalsIgnoreCase("am") && hour == 12) {
            hour = 0;
        }
    }

    /**
     * <p>
     * Calculate day, month, and year for a date given by week and day. If no day is given, the first day of the week
     * will be set, using ISO8601 standard (first week of a year has four or more days, first day of a week is Monday).
     * </p>
     * 
     * @param dateString The string with the date.
     * @param withDay <code>true</code> if the date string contains a day.
     * @param withSeparator <code>true</code> if date string contains a separator.
     */
    private void setDateByWeekOfYear(String dateString, boolean withDay, boolean withSeparator) {
        String[] dateParts;

        if (withSeparator) {
            dateParts = dateString.split("-");
        } else {
            dateParts = new String[3];
            dateParts[0] = dateString.substring(0, 4);
            dateParts[1] = dateString.substring(4, 7);
            if (withDay) {
                dateParts[2] = dateString.substring(7, 8);
            }
        }

        Calendar calendar = new GregorianCalendar();
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
        calendar.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(dateParts[1].substring(1)));
        if (withDay) {
            calendar.set(Calendar.DAY_OF_WEEK, Integer.parseInt(dateParts[2]));
        } else {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        }
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        if (withDay) {
            day = calendar.get(Calendar.DAY_OF_MONTH);
        }

    }

    /**
     * <p>
     * Calculate day, month, year for a date which is given by year and day of year, using ISO8601 standard (first week
     * of a year has four or more days, first day of a week is Monday).
     * </p>
     * 
     * @param dateString The string with the date.
     * @param withSeparator <code>true</code> if date string contains a separator.
     */
    private void setDateByDayOfYear(String dateString, boolean withSeparator) {
        Calendar calendar = new GregorianCalendar();
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        String tempYear;
        String tempDay;
        if (withSeparator) {
            String[] dateParts = dateString.split("-");
            tempYear = dateParts[0];
            tempDay = dateParts[1];
        } else {
            tempYear = dateString.substring(0, 4);
            tempDay = dateString.substring(4);
        }
        calendar.set(Calendar.YEAR, Integer.parseInt(tempYear));
        calendar.set(Calendar.DAY_OF_YEAR, Integer.parseInt(tempDay));

        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        day = calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * <p>
     * Set hour, minute, second, if string is in one of the following forms: <code>HH:MM:SS</code>, <code>HH:MM</code>
     * or <code>HH</code> and optionally contains a difference to UTC in one of the following formats:
     * <code>+|- HH:MM</code>, <code>+|- HH</code> or <code>Z</code> (stands for UTC timezone). <b>Important:</b> Call
     * after setting date parts like year, month and day!
     * </p>
     * 
     * @param timeString The string containing the time.
     */
    private void setTimeValues(String timeString) {
        String actualTime = timeString;
        String diffToUtc = null;
        if (actualTime.contains(".")) {
            actualTime = actualTime.replaceAll("\\.(\\d)*", "");
        }

        String separator = null;
        if (timeString.contains("Z")) {
            separator = "Z";
        } else if (timeString.contains("+")) {
            separator = "\\+";
        } else if (timeString.contains("-")) {
            separator = "-";
        }

        String cleanedTime = actualTime;
        if (separator != null) {
            cleanedTime = actualTime.split(separator)[0];
            if (!separator.equalsIgnoreCase("Z")) {
                diffToUtc = actualTime.split(separator)[1];
            }
        }
        setActualTimeValues(cleanedTime);
        if (diffToUtc != null) {
            setTimeDiff(diffToUtc, separator);
        }
    }

    /**
     * <p>
     * Updates the time with the given difference to UTC. E.g. when time is <code>14:00</code> and difference to UTC is
     * <code>+02:00</code>, the new time is set to <code>12:00</code>. If one of hour, day, month, or year is not set,
     * the difference to UTC cannot be calculated, because the following/previous date cannot be determined (e.g.: 1)
     * The year is unknown, what day is following <code>February 28th</code>? <code>February 29th</code> or
     * <code>March 1st</code>? 2) If day is unknown, what is following <code>23:59:59</code>?
     * </p>
     * 
     * @param time The string with the time in format <code>HH:MM</code> or <code>HH</code>.
     * @param sign The sign, either <code>+</code> or <code>-</code>.
     */
    // TODO wouldn't it be better, to store the time difference instead of modifying the actual time?
    void setTimeDiff(String time, String sign) {
        if (year == -1 || month == -1 || day == -1 || hour == -1) {
            return;
        }
        int tempHour;
        int tempMinute = 0;
        if (time.contains(":")) {
            String[] timeParts = time.split(":");
            tempHour = Integer.parseInt(timeParts[0]);
            tempMinute = Integer.parseInt(timeParts[1]);
        } else if (time.length() == 4) {
            tempHour = Integer.parseInt(time.substring(0, 2));
            tempMinute = Integer.parseInt(time.substring(2, 4));
        } else {
            tempHour = Integer.parseInt(time);
        }

        int tempMinute2 = 0;
        if (minute != -1) {
            tempMinute2 = minute;
        }

        Calendar calendar = new GregorianCalendar(year, month - 1, day, hour, tempMinute2);
        if (sign.equalsIgnoreCase("-")) {
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + tempHour);
            calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + tempMinute);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - tempHour);
            calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) - tempMinute);
        }
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        day = calendar.get(Calendar.DAY_OF_MONTH);
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (minute != -1 || tempMinute != 0) {
            minute = calendar.get(Calendar.MINUTE);
        }
    }

    /**
     * <p>
     * Set hour, minute, and second by extracting from time string. The date string must have any of the following
     * forms: HH:MM:SS, HH:MM, or HH. Only the given parts will be set, leaving the non given parts untouched.
     * </p>
     * 
     * @param timeString The string containing the time in any of the mentioned forms.
     */
    private void setActualTimeValues(String timeString) {
        if (timeString.isEmpty() || timeString.contains(":")) {
            String[] timeParts = timeString.trim().split(":");
            if (timeParts.length > 0 && !timeParts[0].isEmpty()) {
                hour = Integer.parseInt(timeParts[0]);
                if (timeParts.length > 1) {
                    minute = Integer.parseInt(timeParts[1]);
                    if (timeParts.length > 2) {
                        second = Integer.parseInt(timeParts[2]);
                    }
                }
            }
        } else {
            hour = Integer.parseInt(timeString);
        }
    }

    /**
     * <p>
     * Set year, month and day by from an array of values, given the explicit positions of each value in the array. A
     * value of <code>-1</code> signifies that the given value array contains no such value.
     * </p>
     * 
     * @param dateParts An array with date parts.
     * @param yearPos The position in the array containing the year, <code>-1</code> if array contains no year.
     * @param monthPos The position in the array containing the month, <code>-1</code> if array contains no month.
     * @param dayPos The position in the array containing the day, <code>-1</code> if array contains no day.
     */
    private void setDateValues(String[] dateParts, int yearPos, int monthPos, int dayPos) {
        if (yearPos != -1) {
            year = normalizeYear(dateParts[yearPos]);
        }
        if (monthPos != -1) {
            String monthString = dateParts[monthPos].replace(" ", "");
            if (monthString.matches("\\d+")) {
                month = Integer.parseInt(monthString);
            } else {
                month = DateHelper.monthNameToNumber(monthString);
            }
        }
        if (dayPos != -1) {
            day = Integer.parseInt(removeNoDigits(dateParts[dayPos]));
        }
    }

    /**
     * <p>
     * Normalizes a year string by removing apostrophes (e.g. '99) and and transforming it four digits (e.g. 1999).
     * </p>
     * 
     * @param year The string with the year to normalize.
     * @return A four digit year.
     */
    static int normalizeYear(String year) {
        return get4DigitYear(Integer.parseInt(removeNoDigits(year)));
    }

    /**
     * <p>
     * Sets the year in four digits format. For transformation, the current year is considered as a context. Examples:
     * </p>
     * 
     * <ul>
     * <li>year = 20; current year = 2010 -> year > 10 -> result = 1920</li>
     * <li>year = 7; current year = 2010 -> year < 10 -> result = 2007</li>
     * <li>year = 10; current year = 2010 -> year > 10 -> result = 2010</li>
     * <li>year = 99; current year = 2010 -> year > 10 -> result = 1999</li>
     * </ul>
     * 
     * @param date int value representing the year to transform to four digit representation.
     * @return The supplied value with four digits.
     */
    static int get4DigitYear(int year) {
        if (year > 100) {
            return year;
        }
        int currentYear = new GregorianCalendar().get(Calendar.YEAR);
        if (year > currentYear - 2000) {
            return year + 1900;
        } else {
            return year + 2000;
        }
    }

    /**
     * <p>
     * Removes the symbols <code>'</code> from year (e.g. '99), <code>,</code> and <code>st</code>, <code>nd</code>,
     * <code>rd</code>, <code>th</code> from day (e.g. 3rd, June).
     * </p>
     * 
     * @param dateString The string containing the date.
     * @return the The supplied date string with the described characters removed.
     */
    static String removeNoDigits(String dateString) {
        String result = dateString;
        int index = result.indexOf('\'');
        if (index != -1) {
            result = result.substring(index + 1, dateString.length());
        }
        index = result.indexOf(',');
        if (index != -1) {
            result = result.substring(0, index);
        }

        index = result.indexOf(".");
        if (index != -1) {
            result = result.substring(0, index);
        }

        index = result.indexOf("th");
        if (index == -1) {
            index = result.indexOf("st");
            if (index == -1) {
                index = result.indexOf("nd");
                if (index == -1) {
                    index = result.indexOf("rd");
                }
            }
        }
        if (index != -1) {
            result = result.substring(0, index);
        }

        // remove everything after a break
        result = result.replaceAll("\n.*", "");

        return result;
    }

    /**
     * <p>
     * Split time zone acronyms (as defined in {@link RegExp#TIMEZONE}) from the specified date string.
     * </p>
     * 
     * @param dateString The date string potentially containing a time zone acronym.
     * @return An array with two items, the first being the time part, the second the time zone, or <code>null</code>,
     *         if the date string did not contain any time zone information.
     */
    static String[] splitTimeZone(String dateString) {
        Pattern pattern = Pattern.compile(RegExp.TIMEZONE, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(dateString);

        if (matcher.find()) {
            String timeZonePart = matcher.group().trim();
            String timePart = matcher.replaceAll(" ").replace("  ", " ");
            return new String[] {timePart, timeZonePart};
        }

        return null;
    }

    /**
     * <p>
     * Get a regular expression for splitting the supplied date string.
     * </p>
     * 
     * @param dateString A date string, where year, month and day are separated by one of <code>.</code>,
     *            </code>/</code>, <code>_</code> or <code>-</code>.
     * @return A regex for splitting the supplied date string, or <code>null</code>, if none of the given separator
     *         characters could be found.
     */
    static String getSeparatorRegEx(String dateString) {
        if (dateString.contains(".")) {
            return "\\.";
        }
        if (dateString.contains("/")) {
            return "/";
        }
        if (dateString.contains("_")) {
            return "_";
        }
        if (dateString.contains("-")) {
            return "-";
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DateParserLogic [originalDateString=");
        builder.append(originalDateString);
        builder.append(", format=");
        builder.append(format);
        builder.append(", year=");
        builder.append(year);
        builder.append(", month=");
        builder.append(month);
        builder.append(", day=");
        builder.append(day);
        builder.append(", hour=");
        builder.append(hour);
        builder.append(", minute=");
        builder.append(minute);
        builder.append(", second=");
        builder.append(second);
        builder.append(", timeZone=");
        builder.append(timeZone);
        builder.append("]");
        return builder.toString();
    }

}
