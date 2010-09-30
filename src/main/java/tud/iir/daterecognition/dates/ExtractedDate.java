package tud.iir.daterecognition.dates;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tud.iir.daterecognition.ExtractedDateHelper;
import tud.iir.knowledge.RegExp;

/**
 * Represents a date, found in a webpage. <br>
 * A object will be created with a date-string and a possible format. <br>
 * It can be asked for year, month, day and time. If some values can not be constructed the value will be -1.
 * 
 * @author Martin Gregor*
 */
public class ExtractedDate {

    public static final int TECH_URL = 1;
    public static final int TECH_HTTP_HEADER = 2;
    public static final int TECH_HTML_HEAD = 3;
    public static final int TECH_HTML_STRUC = 4;
    public static final int TECH_HTML_CONT = 5;
    public static final int TECH_REFERENCE = 6;
    public static final int TECH_ARCHIVE = 7;

    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DAY = 3;
    public static final int HOUR = 4;
    public static final int MINUTE = 5;
    public static final int SECOND = 6;

    /**
     * Found date as string.
     */
    private String dateString = null;
    /** The format, the dateString is found. */
    private String format;

    /** URL */
    private String url = null;

    // date values
    private int year = -1;
    private int month = -1;
    private int day = -1;
    private int hour = -1;
    private int minute = -1;
    private int second = -1;

    private String timezone = null;

    private double rate = 0;

    /**
     * Standard constructor.
     */
    public ExtractedDate() {
        super();
    }

    /**
     * Creates a new date and sets the dateString.
     * 
     * @param dateString
     */
    public ExtractedDate(final String dateString) {
        super();
        this.dateString = dateString;
    }

    /**
     * creates a new date and sets dateString and format
     * 
     * @param dateString
     * @param format
     */
    public ExtractedDate(final String dateString, final String format) {
        super();
        this.dateString = dateString;
        this.format = format;
        setDateParticles();
    }

    /**
     * Normalizes the date, if a format is given. <br>
     * If no day is given, it will be set to the 1st of month. <br>
     * Set the time to 00:00:00+00, if no time is given <br>
     * 
     * @return a date in format YYYY-MM-DD
     */
    private void setDateParticles() {
        if (format == null || this.dateString == null) {
            return;
        }

        String dateString = this.dateString;

        String[] tempArray = ExtractedDateHelper.removeTimezone(dateString);
        timezone = tempArray[1];
        if (timezone != null) {
            dateString = tempArray[0];
        }

        String[] dateParts = new String[3];
        if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YMD_T[1])) {
            String separator = "T";
            int index = dateString.indexOf(separator);
            if (index == -1) {
                separator = " ";
            }
            String[] temp = dateString.split(separator);
            setDateValues(temp[0].split(ExtractedDateHelper.getSeparator(temp[0])), 0, 1, 2);
            setTimeValues(temp[1]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YMD[1])) {
            setDateValues(dateString.split(ExtractedDateHelper.getSeparator(dateString)), 0, 1, 2);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YM[1])) {
            setDateValues(dateString.split("-"), 0, 1, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YWD[1])) {
            setDatebyWeekOfYear(dateString, true, true);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YWD_T[1])) {
            String separator;
            int index;
            index = dateString.indexOf("T");
            if (index == -1) {
                separator = " ";
            } else {
                separator = "T";
            }
            dateParts = dateString.split(separator);
            setDatebyWeekOfYear(dateParts[0], true, true);
            setTimeValues(dateParts[1]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YW[1])) {
            setDatebyWeekOfYear(dateString, false, true);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YD[1])) {
            setDateByDayOfYear(true);
        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_D[1])) {
            setDateValues(dateString.split(ExtractedDateHelper.getSeparator(dateString)), 0, 1, 2);
        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_MMMM_D[1])) {
            setDateValues(dateString.split("/"), 0, 1, 2);
        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_SPLIT[1])) {
            dateParts = dateString.split("/");
            year = ExtractedDateHelper.normalizeYear(dateParts[0]);
            int tempMonth = 0;
            try {
                day = Integer.parseInt(dateParts[dateParts.length - 1]);
                tempMonth = -1;
            } catch (NumberFormatException exeption) {
                final String lastField = dateParts[dateParts.length - 1];
                final String[] tempDateParts = lastField.split(ExtractedDateHelper.getSeparator(lastField));
                month = Integer.parseInt(tempDateParts[0]);
                day = Integer.parseInt(tempDateParts[1]);
            }
            if (tempMonth == -1) {
                month = Integer.parseInt(dateParts[dateParts.length - 2]);
            }

        } else if (format.equalsIgnoreCase(RegExp.DATE_URL[1])) {
            setDateValues(dateString.split(ExtractedDateHelper.getSeparator(dateString)), 0, 1, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MM_Y[1])) {
            String separator = ExtractedDateHelper.getSeparator(dateString);
            setDateValues(dateString.split(separator), 2, 1, 0);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MM_D_Y[1])) {
            setDateValues(dateString.split(ExtractedDateHelper.getSeparator(dateString)), 2, 0, 1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MMMM_Y[1])) {
            if (dateString.indexOf("\\.") != -1) {
                dateString = dateString.replaceAll("\\.", "");
            }
            if (dateString.indexOf("-") != -1) {
                dateString = dateString.replaceAll("-", " ");
            }
            dateParts = dateString.split(" ");
            setDateValues(dateParts, 2, 1, 0);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MMMM_D_Y[1])) {
            setDateValues(dateString.split(" "), 2, 0, 1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MMMM_D_Y_SEP[1])) {
            setDateValues(dateString.split("-"), 2, 0, 1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EUSA_MMMM_Y[1])) {
            setDateValues(dateString.split(" "), 1, 0, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_MM_Y[1])) {
            String separator = ExtractedDateHelper.getSeparator(dateString);
            setDateValues(dateString.split(separator), 1, 0, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MM[1])) {
            String separator = ExtractedDateHelper.getSeparator(dateString);
            setDateValues(dateString.split(separator), -1, 1, 0);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MMMM[1])) {
            /*
             * int index = dateString.indexOf(".");
             * if (index == -1) {
             * setDateValues(dateString.split(" "), -1, 1, 0);
             * } else {
             * setDateValues(dateString.split("\\."), -1, 1, 0);
             * }
             */
            dateString = dateString.replaceAll("\\.", "");
            setDateValues(dateString.split(" "), -1, 1, 0);

        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MM_D[1])) {
            setDateValues(dateString.split("/"), -1, 0, 1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MMMM_D[1])) {
            setDateValues(dateString.split(" "), -1, 0, 1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MM_Y[1])) {
            setDateValues(dateString.split("/"), 1, 0, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ANSI_C[1])) {
            dateParts = dateString.split(" ");
            setDateValues(dateParts, 4, 1, 2);
            setTimeValues(dateParts[3]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ANSI_C_TZ[1])) {
            dateParts = dateString.split(" ");
            setDateValues(dateParts, 4, 1, 2);
            setTimeValues(dateParts[3] + dateParts[5]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_RFC_1123[1])) {
            dateParts = dateString.split(" ");
            setDateValues(dateParts, 3, 2, 1);
            setTimeValues(dateParts[4]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_RFC_1036[1])) {
            String parts[] = dateString.split(" ");
            setDateValues(parts[1].split("-"), 2, 1, 0);
            setTimeValues(parts[2]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YMD_NO[1])) {
            year = Integer.parseInt(dateString.substring(0, 4));
            month = Integer.parseInt(dateString.substring(4, 6));
            day = Integer.parseInt(dateString.substring(6, 8));
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YWD_NO[1])) {
            setDatebyWeekOfYear(dateString, true, false);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YW_NO[1])) {
            System.out.println(dateString);
            setDatebyWeekOfYear(dateString, false, false);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YD_NO[1])) {
            setDateByDayOfYear(false);
        } else if (format.equalsIgnoreCase(RegExp.DATE_RFC_1123_UTC[1])) {
            dateParts = dateString.split(" ");
            setDateValues(dateParts, 3, 2, 1);
            setTimeValues(dateParts[4] + dateParts[5]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_RFC_1036_UTC[1])) {
            String parts[] = dateString.split(" ");
            setDateValues(parts[1].split("-"), 2, 1, 0);
            setTimeValues(parts[2] + parts[3]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MM_Y_T[1])) {

            String meridiem = hasAMPM(dateString);
            if (meridiem != null) {
                dateString = removeAMPM(dateString, meridiem);
            }
            String[] parts = dateString.split(" ");

            String separator = ExtractedDateHelper.getSeparator(parts[0]);
            String[] date = parts[0].split(separator);
            setDateValues(date, 2, 1, 0);
            StringBuffer sb = new StringBuffer();
            for (int i = 1; i < parts.length; i++) {
                if (parts[i].indexOf("/") == -1) {
                    sb.append(parts[i]);
                }
            }
            setTimeValues(sb.toString());

            set24h(meridiem);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MMMM_Y_T[1])) {

            String meridiem = hasAMPM(dateString);
            if (meridiem != null) {
                dateString = removeAMPM(dateString, meridiem);
            }
            if (dateString.indexOf("-") != -1) {
                dateString = dateString.replaceAll("-", " ");
            }
            String[] parts = dateString.split(" ");
            setDateValues(parts, 2, 1, 0);
            StringBuffer sb = new StringBuffer();
            for (int i = 3; i < parts.length; i++) {
                if (parts[i].indexOf("/") == -1) {
                    sb.append(parts[i]);
                }
            }
            setTimeValues(sb.toString());
            set24h(meridiem);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MM_D_Y_T[1])) {

            String meridiem = hasAMPM(dateString);
            if (meridiem != null) {
                dateString = removeAMPM(dateString, meridiem);
            }
            String[] parts = dateString.split(" ");
            String[] date = parts[0].split("/");
            setDateValues(date, 2, 0, 1);
            StringBuffer sb = new StringBuffer();
            for (int i = 1; i < parts.length; i++) {
                if (parts[i].indexOf("/") == -1) {
                    sb.append(parts[i]);
                }
            }
            setTimeValues(sb.toString());
            set24h(meridiem);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MMMM_D_Y_T[1])) {

            String meridiem = hasAMPM(dateString);
            if (meridiem != null) {
                dateString = removeAMPM(dateString, meridiem);
            }
            String[] parts = dateString.split(" ");
            setDateValues(parts, 2, 0, 1);
            StringBuffer sb = new StringBuffer();
            for (int i = 3; i < parts.length; i++) {
                if (parts[i].indexOf("/") == -1) {
                    sb.append(parts[i]);
                }
            }
            setTimeValues(sb.toString());
            set24h(meridiem);
        }

    }

    /**
     * Removes PM and AM from a string and delete double whitespace.
     * 
     * @param text String to be cleared.
     * @param meridiem AM or PM
     * @return Cleared string.
     */
    private String removeAMPM(String text, String meridiem) {
        String newText;
        newText = text.replaceAll(meridiem, "");
        return newText.replaceAll("  ", " ");
    }

    /**
     * Checks for AM and PM in a string and returns the found.
     * 
     * @param text String to b checked.
     * @return Am or PM.
     */
    private String hasAMPM(String text) {
        int index;
        String meridiem = null;
        index = text.indexOf("AM");
        if (index == -1) {
            index = text.indexOf("am");
            if (index == -1) {
                index = text.indexOf("PM");
                if (index == -1) {
                    index = text.indexOf("pm");
                    if (index != -1) {
                        meridiem = "pm";
                    }
                } else {
                    meridiem = "PM";
                }
            } else {
                meridiem = "am";
            }
        } else {
            meridiem = "AM";
        }
        return meridiem;
    }

    /**
     * If this date has a hour and the date-string has AM or PM values, the hour will be changed in 24h system.
     * 
     * @param meridiem Am or PM
     */
    private void set24h(String meridiem) {

        if (this.hour != -1 && meridiem != null) {
            if (meridiem.equalsIgnoreCase("pm")) {
                if (this.hour > 0 && this.hour < 12) {
                    this.hour += 12;
                }
            } else if (meridiem.equalsIgnoreCase("am") && this.hour == 12) {
                this.hour = 0;
            }
        }
    }

    /**
     * Constructs a normalized datestring in a format from YYYY-MM-DD HH:MM:SS to YYYY-MM depending of given values
     * 
     * @param dateParts
     * @return
     */
    public String getNormalizedDate() {
        return getNormalizedDate(true);
    }

    /**
     * Constructs a normalized datestring in a format from YYYY-MM-DD HH:MM:SS to YYYY-MM depending of given values
     * 
     * @param dateParts
     * @return
     */
    public String getNormalizedDate(boolean time) {
        String normalizedDate;
        if (year == -1) {
            normalizedDate = "0";
        } else {
            normalizedDate = String.valueOf(year);
        }
        if (month == -1) {
            normalizedDate += "-0";
        } else {
            normalizedDate += "-" + ExtractedDateHelper.get2Digits(month);
        }
        if (day != -1) {
            normalizedDate += "-" + ExtractedDateHelper.get2Digits(day);
            if (hour != -1 && time) {
                normalizedDate += " " + ExtractedDateHelper.get2Digits(hour);
                if (minute != -1) {
                    normalizedDate += ":" + ExtractedDateHelper.get2Digits(minute);
                    if (second != -1) {
                        normalizedDate += ":" + ExtractedDateHelper.get2Digits(second);
                    }
                }
            }
        }
        return normalizedDate;

    }

    /**
     * If a date is given by week and day, the normal date (day, month and year) will be calculated.
     * If no day is given, the first day of week will be set.
     * Using ISO8601 standard ( First week of a year has four or more days; first day of a week is Monday)
     * 
     * @param withDay flag for the cases, that a day is given or not
     * @param withSeparator Is there a separator in the dateString?
     * 
     */
    private void setDatebyWeekOfYear(final String dateString, final boolean withDay, final boolean withSeparator) {
        String[] dateParts = new String[3];

        if (withSeparator) {
            dateParts = dateString.split("-");
        } else {
            dateParts[0] = dateString.substring(0, 4);
            dateParts[1] = dateString.substring(4, 7);
            if (withDay) {
                dateParts[2] = dateString.substring(7, 8);
            }
        }

        final Calendar calendar = new GregorianCalendar();

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
     * If a date is given by year and day of year, the date (day, month and year) will be calculated.
     * Using ISO8601 standard ( First week of a year has four or more days; first day of a week is Monday)
     * 
     * @param withSeparator Is there a separator in the dateString?
     * 
     * 
     */
    private void setDateByDayOfYear(final boolean withSeparator) {
        final Calendar calendar = new GregorianCalendar();
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        if (withSeparator) {
            final String[] dateParts = this.dateString.split("-");
            calendar.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
            calendar.set(Calendar.DAY_OF_YEAR, Integer.parseInt(dateParts[1]));

        } else {
            calendar.set(Calendar.YEAR, Integer.parseInt(this.dateString.substring(0, 4)));
            calendar.set(Calendar.DAY_OF_YEAR, Integer.parseInt(this.dateString.substring(4)));
        }

        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        day = calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * <b>!!! IMPORTANT: call <u>AFTER</u> setting dateparts like year, month and day !!! </b><br>
     * <br>
     * 
     * Set hour, minute and second, if string is in one of the following forms: <br>
     * <i>HH:MM:SS</i>, <i>HH:MM</i> or <i>HH</i> and has optional a difference to UTC in on of the following formats: <br>
     * <i>+|- HH:MM</i>, <i>+|- HH</i> or "<i>Z</i>" (stands for UTC timezone).
     * 
     * @param time
     */
    private void setTimeValues(final String time) {
        String actualTime = time;
        String diffToUTC = null;
        int index;
        // int milliSec = 0;

        index = actualTime.indexOf('.');
        if (index != -1) {
            String regExp = "\\.(\\d)*";
            Pattern pattern;
            Matcher matcher;
            pattern = Pattern.compile(regExp);
            matcher = pattern.matcher(actualTime);
            if (matcher.find()) {
                /*
                 * final int start = matcher.start();
                 * final int end = matcher.end();
                 * milliSec = Integer.parseInt(actualTime.substring(start + 1, end));
                 */
                actualTime = actualTime.replaceAll(regExp, "");

            }
        }

        String separator = null;
        index = time.indexOf('Z');
        if (index == -1) {
            index = time.indexOf('+');
            if (index == -1) {
                index = time.indexOf('-');
                if (index != -1) {
                    separator = "-";
                }
            } else {
                separator = "\\+";
            }
        } else {
            separator = "Z";
        }
        String cleanedTime = actualTime;
        if (separator != null) {
            cleanedTime = actualTime.split(separator)[0];
            if (!separator.equalsIgnoreCase("Z")) {
                diffToUTC = actualTime.split(separator)[1];
            }
        }
        setActualTimeValues(cleanedTime);
        if (diffToUTC != null) {
            setTimeDiff(diffToUTC, separator);
        }
    }

    /**
     * Refreshes the time with given difference to UTC. <br>
     * E.g.: time is 14:00 and difference to UTC is +02:00 -> new time is 12:00. <br>
     * <br>
     * If only one of hour, day, month or year is not set, we can not calculate a difference to UTC, because we do not
     * know following or previous date. <br>
     * E.g.: if year is unknown: what day is following on February 28th? February 29th or March 1st? <br>
     * If day is unknown, what is following on 23:59:59?
     * 
     * @param time must have format: HH:MM or HH.
     * @param sign must be + or -
     */
    private void setTimeDiff(final String time, final String sign) {
        if (this.year == -1 || this.month == -1 || this.day == -1 || this.hour == -1) {
            return;
        }
        int hour;
        int minute = 0;
        if (time.indexOf(':') == -1) {
            if (time.length() == 4) {
                hour = Integer.parseInt(time.substring(0, 2));
                minute = Integer.parseInt(time.substring(2, 4));
            } else {
                hour = Integer.parseInt(time);
            }
        } else {
            String[] timeParts = time.split(":");
            hour = Integer.parseInt(timeParts[0]);
            minute = Integer.parseInt(timeParts[1]);
        }

        int actualMinute = 0;
        if (this.minute != -1) {
            actualMinute = this.minute;

        }
        final Calendar calendar = new GregorianCalendar(this.year, this.month - 1, this.day, this.hour, actualMinute);

        if (sign.equalsIgnoreCase("-")) {
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + hour);
            calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + minute);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - hour);
            calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) - minute);
        }
        this.year = calendar.get(Calendar.YEAR);
        this.month = calendar.get(Calendar.MONTH) + 1;
        this.day = calendar.get(Calendar.DAY_OF_MONTH);
        this.hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (this.minute != -1 || minute != 0) {
            this.minute = calendar.get(Calendar.MINUTE);
        }

    }

    /**
     * Sets hour, minute and second by extracting from time-string. <br>
     * There for the time-string must have one the following forms: HH:MM:SS or HH:MM or HH. <br>
     * Only the given parts will be set.
     * 
     * @param time must have one of the following forms: HH:MM:SS or HH:MM or HH.
     */
    private void setActualTimeValues(final String time) {
        if (time.indexOf(':') == -1) {
            this.hour = Integer.parseInt(time);

        } else {
            final String[] timeParts = time.split(":");
            if (timeParts.length > 0) {
                this.hour = Integer.parseInt(timeParts[0]);
                if (timeParts.length > 1) {
                    this.minute = Integer.parseInt(timeParts[1]);
                    if (timeParts.length > 2) {
                        this.second = Integer.parseInt(timeParts[2]);
                    }
                }
            }
        }
    }

    /**
     * Sets the year, month and day of this date by getting a array with this values and the position of each value in
     * the array.
     * 
     * @param dateParts The array with date-parts.
     * @param yearPos Position of year in the date-array.
     * @param monthPos Position of month in the date-array.
     * @param dayPos Position of day in the date-array.
     */
    private void setDateValues(String[] dateParts, int yearPos, int monthPos, int dayPos) {
        if (yearPos != -1) {
            this.year = ExtractedDateHelper.normalizeYear(dateParts[yearPos]);
        }
        if (monthPos != -1) {
            dateParts[monthPos] = dateParts[monthPos].replaceAll(" ", "");
            try {
                dateParts[monthPos] = String.valueOf(Integer.parseInt(dateParts[monthPos]));
            } catch (NumberFormatException e) {
                dateParts[monthPos] = ExtractedDateHelper.getMonthNumber(dateParts[monthPos]);
            }
            this.month = Integer.parseInt(dateParts[monthPos]);

        }
        if (dayPos != -1) {
            this.day = Integer.parseInt(ExtractedDateHelper.removeNodigits(dateParts[dayPos]));
        }
    }

    /**
     * 
     * @param dateString
     */
    public void setDateString(final String dateString) {
        this.dateString = dateString;
    }

    /**
     * 
     * @return
     */
    public String getDateString() {
        return dateString;
    }

    public void setFormat(final String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public int get(int field) {
        int value = -1;
        switch (field) {
            case YEAR:
                value = this.year;
                break;
            case MONTH:
                value = this.month;
                break;
            case DAY:
                value = this.day;
                break;
            case HOUR:
                value = this.hour;
                break;
            case MINUTE:
                value = this.minute;
                break;
            case SECOND:
                value = this.second;
                break;
        }
        return value;
    }

    public ArrayList<Object> getAll() {
        ArrayList<Object> result = new ArrayList<Object>();
        result.add(this.dateString);
        result.add(this.format);
        result.add(this.year);
        result.add(this.month);
        result.add(this.day);
        result.add(this.hour);
        result.add(this.minute);
        result.add(this.second);
        result.add(this.timezone);
        result.add(this.url);
        return result;
    }

    public void setAll(ArrayList<Object> values) {
        for (int i = 0; i < values.size(); i++) {
            switch (i) {
                case 0:
                    this.dateString = (String) values.get(i);
                    break;
                case 1:
                    this.format = (String) values.get(i);
                    break;
                case 2:
                    this.year = (Integer) values.get(i);
                    ;
                    break;
                case 3:
                    this.month = (Integer) values.get(i);
                    ;
                    break;
                case 4:
                    this.day = (Integer) values.get(i);
                    ;
                    break;
                case 5:
                    this.hour = (Integer) values.get(i);
                    ;
                    break;
                case 6:
                    this.minute = (Integer) values.get(i);
                    ;
                    break;

                case 7:
                    this.second = (Integer) values.get(i);
                    ;
                    break;
                case 8:
                    this.timezone = (String) values.get(i);
                    ;
                    break;
                case 9:
                    this.url = (String) values.get(i);
                    ;
            }
        }
    }

    public void set(int field, int value) {
        switch (field) {
            case YEAR:
                this.year = value;
                break;
            case MONTH:
                this.month = value;
                break;
            case DAY:
                this.day = value;
                break;
            case HOUR:
                this.hour = value;
                break;
            case MINUTE:
                this.minute = value;
                break;
            case SECOND:
                this.second = value;
                break;

        }
    }

    public String toString() {
        return dateString + " -> " + this.getNormalizedDate() + " Format: " + this.format + " Technique: "
                + ExtractedDateHelper.getTypString(getType());
    }

    public int getType() {
        return 0;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public int getExactness() {
        int exactness = 0;
        if (this.year != -1) {
            exactness++;
            if (this.month != -1) {
                exactness++;
                if (this.day != -1) {
                    exactness++;
                    if (this.hour != -1) {
                        exactness++;
                        if (this.minute != -1) {
                            exactness++;
                            if (this.second != -1) {
                                exactness++;
                            }
                        }
                    }
                }
            }
        }
        return exactness;
    }

    public String getKeyword() {
        return "";
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getRate() {
        return rate;
    }
}
