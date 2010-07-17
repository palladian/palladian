package tud.iir.daterecognition;

import java.util.Calendar;
import java.util.GregorianCalendar;

import tud.iir.knowledge.RegExp;

/**
 * Represents a date, found in a webpage. <br>
 * A object will be created with a date-string and a possible format. <br>
 * It can be asked for year, month, day and time. If some values can not be constructed the value will be -1.
 * 
 * @author Martin Gregor*
 */
public class ExtractedDate {

    /**
     * Found date as string.
     */
    private String dateString = null;
    /** The format, the dateString is found. */
    private String format;
    /** Technique the ExtractedDate is found. <br> */
    private int extractionTechnique;
    private int year = -1;
    private int month = -1;
    private int day = -1;
    private int hour = -1;
    private int minute = -1;
    private int second = -1;
    private int diffToUTC;

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
        if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YMD[1])) {
            dateParts = this.dateString.split("-");
            year = DateGetterHelper.normalizeYear(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[2]));
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YM[1])) {
            dateParts = this.dateString.split("-");
            year = DateGetterHelper.normalizeYear(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);

        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YWD[1])) {
            setDatebyWeekOfYear(true);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YW[1])) {
            setDatebyWeekOfYear(false);

        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YD[1])) {
            dateParts = this.dateString.split("-");
            final Calendar calendar = new GregorianCalendar();
            calendar.setMinimalDaysInFirstWeek(4);
            calendar.setFirstDayOfWeek(Calendar.MONDAY);
            calendar.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
            calendar.set(Calendar.DAY_OF_YEAR, Integer.parseInt(dateParts[1]));
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            day = calendar.get(Calendar.DAY_OF_MONTH);
        } else if (format.equalsIgnoreCase(RegExp.URL_DATE_D[1])) {
            dateParts = this.dateString.split(DateGetterHelper.getSeparator(this.dateString));
            year = DateGetterHelper.normalizeYear(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
            day = Integer.parseInt(DateGetterHelper.removeNodigits(dateParts[2]));
        } else if (format.equalsIgnoreCase(RegExp.URL_DATE[1])) {
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
        }
    }

    /**
     * Constructs a normalized datestring in a format from YYYY-MM-DD HH:MM:SS to YYYY-MM depending of given values
     * 
     * @param dateParts
     * @return
     */
    public String getNormalizedDate() {
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

    /**
     * If a date is given by week and day, the normal date (day, month and year) will be calculated.
     * If no day is given, the first day of week will be set.
     * Using ISO8601 standard ( First week of a year has four or more days; first day of a week is Monday)
     * 
     * @param withDay flag for the cases, that a day is given or not
     * @return date in yyyy-mm-dd format
     */
    private void setDatebyWeekOfYear(final boolean withDay) {
        final String[] dateParts = this.dateString.split("-");
        final Calendar calendar = new GregorianCalendar();

        calendar.setMinimalDaysInFirstWeek(4);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
        calendar.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(dateParts[1]));
        if (withDay) {
            calendar.set(Calendar.DAY_OF_WEEK, Integer.parseInt(dateParts[2]));
        } else {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        }
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        if (withDay) {
            day = calendar.get(Calendar.DAY_OF_MONTH);
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

}
