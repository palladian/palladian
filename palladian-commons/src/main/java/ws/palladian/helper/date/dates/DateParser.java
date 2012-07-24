package ws.palladian.helper.date.dates;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.ExtractedDateHelper;
import ws.palladian.helper.nlp.StringHelper;

public class DateParser {

    private int year = -1;
    private int month = -1;
    private int day = -1;
    private int hour = -1;
    private int minute = -1;
    private int second = -1;
    private String dateString;
    private String timezone = null;

    public static ExtractedDate parse(String dateString, String format) {
        DateParser dateParser = new DateParser();
        dateParser.setDateParticles(dateString, format);
        ExtractedDate extractedDate = new ExtractedDate(dateString, format);
        extractedDate.set(AbstractDate.YEAR, dateParser.year);
        extractedDate.set(AbstractDate.MONTH, dateParser.month);
        extractedDate.set(AbstractDate.DAY, dateParser.day);
        extractedDate.set(AbstractDate.HOUR, dateParser.hour);
        extractedDate.set(AbstractDate.MINUTE, dateParser.minute);
        extractedDate.set(AbstractDate.SECOND, dateParser.second);
        return extractedDate;
    }

    /**
     * Normalizes the date, if a format is given. <br>
     * If no day is given, it will be set to the 1st of month. <br>
     * Set the time to 00:00:00+00, if no time is given <br>
     * 
     * @return a date in format YYYY-MM-DD
     */
    private void setDateParticles(String dateString, String format) {
        // if (format == null || this.dateString == null) {
        // return;
        // }
        //
        // String dateString = this.dateString;
        this.dateString = dateString;

        String[] tempArray = removeTimezone(dateString);
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
            setDateValues(temp[0].split(getSeparator(temp[0])), 0, 1, 2);
            setTimeValues(temp[1]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YMD[1])) {
            setDateValues(dateString.split(getSeparator(dateString)), 0, 1, 2);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YM[1])) {
            setDateValues(dateString.split("-"), 0, 1, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YWD[1])) {
            setDateByWeekOfYear(dateString, true, true);
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
            setDateByWeekOfYear(dateParts[0], true, true);
            setTimeValues(dateParts[1]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YW[1])) {
            setDateByWeekOfYear(dateString, false, true);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YD[1])) {
            setDateByDayOfYear(true);
        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_D[1])) {
            setDateValues(dateString.split(getSeparator(dateString)), 0, 1, 2);
        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_MMMM_D[1])) {
            setDateValues(dateString.split("/"), 0, 1, 2);
        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_SPLIT[1])) {
            dateParts = dateString.split("/");
            int tempMonth = 0;
            try {
                year = normalizeYear(dateParts[0]);
                day = Integer.parseInt(dateParts[dateParts.length - 1]);
                tempMonth = -1;
            } catch (NumberFormatException exeption) {
                final String lastField = dateParts[dateParts.length - 1];
                final String[] tempDateParts = lastField.split(getSeparator(lastField));
                month = Integer.parseInt(tempDateParts[0]);
                day = Integer.parseInt(tempDateParts[1]);
            }
            if (tempMonth == -1) {
                month = Integer.parseInt(dateParts[dateParts.length - 2]);
            }

        } else if (format.equalsIgnoreCase(RegExp.DATE_URL[1])) {
            setDateValues(dateString.split(getSeparator(dateString)), 0, 1, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MM_Y[1])) {
            String separator = getSeparator(dateString);
            setDateValues(dateString.split(separator), 2, 1, 0);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MM_D_Y[1])) {
            setDateValues(dateString.split(getSeparator(dateString)), 2, 0, 1);
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
            try {
                String[] parts = dateString.split(" ");
                if (parts.length == 2) {
                    String[] tempParts = new String[3];
                    tempParts[0] = parts[0].split("\\.")[0];
                    tempParts[1] = parts[0].split("\\.")[1];
                    tempParts[2] = parts[1];
                    parts = tempParts;
                }
                setDateValues(parts, 2, 0, 1);
            } catch (Exception e) {
            }

        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MMMM_D_Y_SEP[1])) {
            setDateValues(dateString.split("-"), 2, 0, 1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EUSA_MMMM_Y[1])) {
            setDateValues(dateString.split(" "), 1, 0, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EUSA_YYYY_MMM_D[1])) {
            setDateValues(dateString.split("-"), 0, 1, 2);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_MM_Y[1])) {
            String separator = getSeparator(dateString);
            setDateValues(dateString.split(separator), 1, 0, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MM[1])) {
            String separator = getSeparator(dateString);
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
            setDateByWeekOfYear(dateString, true, false);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YW_NO[1])) {
            setDateByWeekOfYear(dateString, false, false);
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

            String meridiem = hasAmPm(dateString);
            if (meridiem != null) {
                dateString = removeAmPm(dateString, meridiem);
            }
            String[] parts = dateString.split(" ");

            String separator = getSeparator(parts[0]);
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

            String meridiem = hasAmPm(dateString);
            if (meridiem != null) {
                dateString = removeAmPm(dateString, meridiem);
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

            String meridiem = hasAmPm(dateString);
            if (meridiem != null) {
                dateString = removeAmPm(dateString, meridiem);
            }
            String[] parts = dateString.split(" ");
            String separator = getSeparator(parts[0]);
            String[] date = parts[0].split(separator);
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

            String meridiem = hasAmPm(dateString);
            if (meridiem != null) {
                dateString = removeAmPm(dateString, meridiem);
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
        } else if (format.equalsIgnoreCase(RegExp.DATE_CONTEXT_YYYY[1])) {
            year = Integer.valueOf(dateString);
        }

    }

    /**
     * Removes PM and AM from a string and delete double whitespace.
     * 
     * @param text String to be cleared.
     * @param meridiem AM or PM
     * @return Cleared string.
     */
    private String removeAmPm(String text, String meridiem) {
        String newText = text.replaceAll(meridiem, "");
        return newText.replaceAll("  ", " ");
    }

    /**
     * Checks for AM and PM in a string and returns the found.
     * 
     * @param text String to b checked.
     * @return Am or PM.
     */
    private String hasAmPm(String text) {
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
     * If a date is given by week and day, the normal date (day, month and year) will be calculated.
     * If no day is given, the first day of week will be set.
     * Using ISO8601 standard ( First week of a year has four or more days; first day of a week is Monday)
     * 
     * @param withDay flag for the cases, that a day is given or not
     * @param withSeparator Is there a separator in the dateString?
     * 
     */
    private void setDateByWeekOfYear(final String dateString, final boolean withDay, final boolean withSeparator) {
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
        if (time.indexOf(':') == -1 && !time.isEmpty()) {
            this.hour = Integer.parseInt(time);

        } else {
            final String[] timeParts = time.trim().split(":");
            if (timeParts.length > 0 && !timeParts[0].isEmpty()) {
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
            try {
                this.year = normalizeYear(dateParts[yearPos]);
            } catch (Exception e) {
                // LOGGER.error(e.getMessage());
            }
        }
        if (monthPos != -1) {
            dateParts[monthPos] = dateParts[monthPos].replace(" ", "");
            try {
                dateParts[monthPos] = String.valueOf(Integer.parseInt(dateParts[monthPos]));
            } catch (NumberFormatException e) {
                dateParts[monthPos] = ExtractedDateHelper.getMonthNumber(dateParts[monthPos]);
            }
            if (dateParts[monthPos] != null) {
                this.month = Integer.parseInt(dateParts[monthPos]);
            }

        }
        if (dayPos != -1) {
            this.day = Integer.parseInt(removeNoDigits(dateParts[dayPos]));
        }
    }

    /**
     * Normalizes a year. Removes apostrophe (e.g. '99) and makes it four digit.
     * 
     * @param year
     * @return A four digit year.
     */
    static int normalizeYear(String year) {
        return get4DigitYear(Integer.parseInt(removeNoDigits(year)));
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
    static int get4DigitYear(int year) {
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
     * Removes the symbols "'" from Year '99 and "," from Day 03, June.
     * 
     * @param date
     * @return the entered date without the symbols
     */
    static String removeNoDigits(String datePart) {
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
        cleardString = cleardString.replaceAll("\n.*", "");

        return cleardString;
    }

    /**
     * Removes timezone acronyms.
     * 
     * @param dateString
     * @return
     */
    static String[] removeTimezone(String dateString) {
        return StringHelper.removeFirstStringpart(dateString, RegExp.TIMEZONE);
    }

    /**
     * 
     * @param text a date, where year, month and day are separated by . / or _
     * @return the separating symbol
     */
    static String getSeparator(String text) {
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

}
