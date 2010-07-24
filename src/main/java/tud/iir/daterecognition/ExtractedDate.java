package tud.iir.daterecognition;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tud.iir.helper.DateHelper;
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

    /**
     * Found date as string.
     */
    private String dateString = null;
    /** The format, the dateString is found. */
    private String format;
    /** Technique the ExtractedDate is found. <br> */
    private int extractionTechnique = -1;
    private int year = -1;
    private int month = -1;
    private int day = -1;
    private int hour = -1;
    private int minute = -1;
    private int second = -1;
    private int diffToUTC;
    /**
     * Context, in witch the date was found. <br>
     * E.g.: URL, tag-name, HTTP-tag, keyword...
     */
    private String context = null;

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
        String[] dateParts = new String[3];
        if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YMD_T[1])) {
            String separator = "T";
            final int index = this.dateString.indexOf(separator);
            if (index == -1) {
                separator = " ";
            }
            final String[] temp = this.dateString.split(separator);
            dateParts = temp[0].split("-");
            year = DateGetterHelper.normalizeYear(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[2]));
            setTimeValues(temp[1]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YMD[1])) {
            dateParts = this.dateString.split("-");
            year = DateGetterHelper.normalizeYear(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[2]));
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YM[1])) {
            dateParts = this.dateString.split("-");
            year = DateGetterHelper.normalizeYear(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YWD[1])) {
            setDatebyWeekOfYear(this.dateString, true, true);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YWD_T[1])) {
            String separator;
            int index;
            index = this.dateString.indexOf("T");
            if (index == -1) {
                separator = " ";
            } else {
                separator = "T";
            }
            dateParts = this.dateString.split(separator);
            setDatebyWeekOfYear(dateParts[0], true, true);
            setTimeValues(dateParts[1]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YW[1])) {
            setDatebyWeekOfYear(this.dateString, false, true);

        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YD[1])) {
            setDateByDayOfYear(true);

        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_D[1])) {
            dateParts = this.dateString.split(DateGetterHelper.getSeparator(this.dateString));
            year = DateGetterHelper.normalizeYear(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[2]));
        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_SPLIT[1])) {
            dateParts = this.dateString.split("/");
            year = DateGetterHelper.normalizeYear(dateParts[0]);
            int tempMonth = 0;
            try {
                day = Integer.parseInt(dateParts[dateParts.length - 1]);
                tempMonth = -1;
            } catch (NumberFormatException exeption) {
                final String lastField = dateParts[dateParts.length - 1];
                final String[] tempDateParts = lastField.split(DateGetterHelper.getSeparator(lastField));
                month = Integer.parseInt(tempDateParts[0]);
                day = Integer.parseInt(tempDateParts[1]);
            }
            if (tempMonth == -1) {
                month = Integer.parseInt(dateParts[dateParts.length - 2]);
            }

        } else if (format.equalsIgnoreCase(RegExp.DATE_URL[1])) {
            dateParts = this.dateString.split(DateGetterHelper.getSeparator(this.dateString));
            year = DateGetterHelper.normalizeYear(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MM_Y[1])) {
            dateParts = this.dateString.split("\\.");
            year = DateGetterHelper.normalizeYear(dateParts[2]);
            month = Integer.parseInt(dateParts[1]);
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[0]));
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MM_D_Y[1])) {
            dateParts = this.dateString.split("/");
            year = DateGetterHelper.normalizeYear(dateParts[2]);
            month = Integer.parseInt(dateParts[0]);
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[1]));
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MMMM_Y[1])) {
            dateParts[0] = this.dateString.split("\\.")[0];
            final String[] temp = this.dateString.split("\\.")[1].split(" ");
            System.arraycopy(temp, 1, dateParts, 1, temp.length - 1);
            year = DateGetterHelper.normalizeYear(dateParts[2]);
            month = Integer.parseInt(DateGetterHelper.getMonthNumber(dateParts[1]));
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[0]));
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MMMM_D_Y[1])) {
            dateParts = this.dateString.split(" ");
            year = DateGetterHelper.normalizeYear(dateParts[2]);
            month = Integer.parseInt(DateGetterHelper.getMonthNumber(dateParts[0]));
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[1]));
        } else if (format.equalsIgnoreCase(RegExp.DATE_EUSA_MMMM_Y[1])) {
            dateParts = this.dateString.split(" ");
            year = DateGetterHelper.normalizeYear(dateParts[1]);
            month = Integer.parseInt(DateGetterHelper.getMonthNumber(dateParts[0]));
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_MM_Y[1])) {
            dateParts = this.dateString.split(".");
            year = DateGetterHelper.normalizeYear(dateParts[1]);
            month = Integer.parseInt(dateParts[0]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MM[1])) {
            dateParts = this.dateString.split(".");
            month = Integer.parseInt(dateParts[1]);
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[0]));
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MMMM[1])) {
            dateParts = this.dateString.split(".");
            month = Integer.parseInt(DateGetterHelper.getMonthNumber(dateParts[1]));
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[0]));
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MM_D[1])) {
            dateParts = this.dateString.split("/");
            month = Integer.parseInt(dateParts[0]);
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[1]));
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MMMM_D[1])) {
            dateParts = this.dateString.split(" ");
            month = Integer.parseInt(DateGetterHelper.getMonthNumber(dateParts[0]));
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[1]));
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MM_Y[1])) {
            dateParts = this.dateString.split("/");
            year = DateGetterHelper.normalizeYear(dateParts[1]);
            month = Integer.parseInt(dateParts[0]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ANSI_C[1])) {
            dateParts = this.dateString.split(" ");
            year = DateGetterHelper.normalizeYear(dateParts[4]);
            month = Integer.parseInt(DateGetterHelper.getMonthNumber(dateParts[1]));
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[2]));
            final String time[] = dateParts[3].split(":");
            this.hour = Integer.parseInt(time[0]);
            this.minute = Integer.parseInt(time[1]);
            this.second = Integer.parseInt(time[2]);

        } else if (format.equalsIgnoreCase(RegExp.DATE_ANSI_C_TZ[1])) {
            dateParts = this.dateString.split(" ");
            year = DateGetterHelper.normalizeYear(dateParts[4]);
            month = Integer.parseInt(DateGetterHelper.getMonthNumber(dateParts[1]));
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[2]));
            setTimeValues(dateParts[3] + dateParts[5]);

        } else if (format.equalsIgnoreCase(RegExp.DATE_RFC_1123[1])) {
            dateParts = this.dateString.split(" ");
            year = DateGetterHelper.normalizeYear(dateParts[3]);
            month = Integer.parseInt(DateGetterHelper.getMonthNumber(dateParts[2]));
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[1]));
            final String time[] = dateParts[4].split(":");
            this.hour = Integer.parseInt(time[0]);
            this.minute = Integer.parseInt(time[1]);
            this.second = Integer.parseInt(time[2]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_RFC_1036[1])) {
            final String parts[] = this.dateString.split(" ");
            dateParts = parts[1].split("-");
            year = DateGetterHelper.normalizeYear(dateParts[2]);
            month = Integer.parseInt(DateGetterHelper.getMonthNumber(dateParts[1]));
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[0]));
            final String[] time = parts[2].split(":");
            this.hour = Integer.parseInt(time[0]);
            this.minute = Integer.parseInt(time[1]);
            this.second = Integer.parseInt(time[2]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YMD_NO[1])) {
            year = Integer.parseInt(this.dateString.substring(0, 4));
            month = Integer.parseInt(this.dateString.substring(4, 6));
            day = Integer.parseInt(this.dateString.substring(6, 8));
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YWD_NO[1])) {
            setDatebyWeekOfYear(this.dateString, true, false);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YW_NO[1])) {
            setDatebyWeekOfYear(this.dateString, false, false);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YD_NO[1])) {
            setDateByDayOfYear(false);
        }
    }

    /**
     * Constructs a normalized datestring in a format from YYYY-MM-DD HH:MM:SS to YYYY-MM depending of given values
     * 
     * @param dateParts
     * @return
     */
    public String getNormalizedDateString() {
        String normalizedDate;
        if (year == -1) {
            normalizedDate = "0";
        } else {
            normalizedDate = String.valueOf(year);
        }
        if (month == -1) {
            normalizedDate += "-0";
        } else {
            normalizedDate += "-" + DateGetterHelper.get2Digits(month);
        }
        if (day != -1) {
            normalizedDate += "-" + DateGetterHelper.get2Digits(day);
            if (hour != -1) {
                normalizedDate += " " + DateGetterHelper.get2Digits(hour);
                if (minute != -1) {
                    normalizedDate += ":" + DateGetterHelper.get2Digits(minute);
                    if (second != -1) {
                        normalizedDate += ":" + DateGetterHelper.get2Digits(second);
                    }
                }
            }
        }
        return normalizedDate;

    }

    public Date getNormalizedDate() {
        return new Date(getTimeStamp());
    }

    public long getTimeStamp() {
        return DateHelper.getTimestamp(getNormalizedDateString());
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
            final String regExp = "\\.(\\d)*";
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
            hour = Integer.parseInt(time);
        } else {
            final String[] timeParts = time.split(":");
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

    public void setExtractionTechnique(final int extractionTechnique) {
        this.extractionTechnique = extractionTechnique;
    }

    public int getExtractionTechnique() {
        return extractionTechnique;
    }

    /**
     * 
     * @return Year of extracted date.
     */
    public int getYear() {
        return this.year;
    }

    public void setYear(final int year) {
        this.year = year;
    }

    /**
     * 
     * @return Month of extracted date.
     */
    public int getMonth() {
        return this.month;
    }

    public void setMonth(final int month) {
        this.month = month;
    }

    /**
     * 
     * @return Day of extracted date.
     */
    public int getDay() {
        return this.day;
    }

    public void setDay(final int day) {
        this.day = day;
    }

    /**
     * 
     * @return Hour of extracted date.
     */
    public int getHour() {
        return this.hour;
    }

    public void setHour(final int hour) {
        this.hour = hour;
    }

    /**
     * 
     * @return Minutes of extracted date.
     */
    public int getMinute() {
        return this.minute;
    }

    public void setMinute(final int minute) {
        this.minute = minute;
    }

    /**
     * 
     * @return Second of extracted date.
     */
    public int getSecond() {
        return this.second;
    }

    public void setSecond(final int second) {
        this.second = second;
    }

    /**
     * 
     * @return difference to UTC of extracted date.
     */
    public int getDiffToUTC() {
        return this.diffToUTC;
    }

    public void setDiffToUTC(final int diffToUTC) {
        this.diffToUTC = diffToUTC;
    }

    /**
     * To set the context in witch the date was found. <br>
     * E.g.: URL, tag-name, HTTP-tag, keyword...
     * 
     * @param context
     */
    public void setContext(final String context) {
        this.context = context;
    }

    /**
     * Gets the context in witch the date was found. <br>
     * E.g.: URL, tag-name, HTTP-tag, keyword...
     * 
     * @return A string, that can be a URL, a tag-name, a keyword <br>
     *         To specify use: getExtractionTechnique() .
     */
    public String getContext() {
        return context;
    }

}
