package tud.iir.helper;

import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;

import tud.iir.daterecognition.ExtractedDate;

public class DateComparator implements Comparator<ExtractedDate> {

    /** Compare will stop after year. Value = 1. */
    public static final int STOP_YEAR = 1;
    /** Compare will stop after month. Value = 2. */
    public static final int STOP_MONTH = 2;
    /** Compare will stop after day. Value = 3. */
    public static final int STOP_DAY = 3;
    /** Compare will stop after hour. Value = 4. */
    public static final int STOP_HOUR = 4;
    /** Compare will stop after minute. Value = 5. */
    public static final int STOP_MINUTE = 5;
    /** Compare will not stop. (After second there are no more comparable values. Value = 6. */
    public static final int STOP_SECOND = 6;

    /** Get date-difference in milliseconds */
    public static final int MEASURE_MILLI_SEC = 1;
    /** Get date-difference in seconds */
    public static final int MEASURE_SEC = 1000;
    /** Get date-difference in minutes */
    public static final int MEASURE_MIN = 60000;
    /** Get date-difference in hours */
    public static final int MEASURE_HOUR = 3600000;
    /** Get date-difference in days */
    public static final int MEASURE_DAY = 86400000;

    /**
     * Compares two dates.<br>
     * Returns -1, 0 or 1 if date1 is newer, equals or older then date2.<br>
     * If both dates are not comparable, for e.g. date1.month is not set, the returning value will be -2.<br>
     * <br>
     * This does only matter, if the higher parameter are equal.<br>
     * For e.g.:<br>
     * date.year = 2007 and date2.year =2006; date1.month=11 and date2.month =-1.<br>
     * Then the returning value will be -1, because 2007>2006.<br>
     * If date1.year is 2006 as well, then the return value will be -2, because the years are equal and the month can
     * not be compared.
     * 
     */
    @Override
    public int compare(ExtractedDate date1, ExtractedDate date2) {
        return compare(date1, date2, STOP_SECOND);
    }

    /**
     * Like <b>compare(ExtractedDate date1, ExtractedDate date2)</b>, but compares only until a given depth. <br>
     * For e.g. usually 12.04.2007 and April 2007 can not be compared. But with stopflag STOP_DAY only year and month
     * will be compared.<br>
     * So normal compare would return -2, but this time the result is 0.
     * 
     * @param date1
     * @param date2
     * @param stopFlag Depth of comparing. Values are given as static constant in this class. (STOP_...)
     * @return
     */
    public int compare(ExtractedDate date1, ExtractedDate date2, int stopFlag) {
        int returnValue;
        returnValue = compare(date1.get(ExtractedDate.YEAR), date2.get(ExtractedDate.YEAR));
        if (returnValue == 0 && stopFlag > DateComparator.STOP_YEAR) {
            returnValue = compare(date1.get(ExtractedDate.MONTH), date2.get(ExtractedDate.MONTH));
            if (returnValue == 0 && stopFlag > DateComparator.STOP_MONTH) {
                returnValue = compare(date1.get(ExtractedDate.DAY), date2.get(ExtractedDate.DAY));
                if (returnValue == 0 && stopFlag > DateComparator.STOP_DAY) {
                    returnValue = compare(date1.get(ExtractedDate.HOUR), date2.get(ExtractedDate.HOUR));
                    if (returnValue == 0 && stopFlag > DateComparator.STOP_HOUR) {
                        returnValue = compare(date1.get(ExtractedDate.MINUTE), date2.get(ExtractedDate.MINUTE));
                        if (returnValue == 0 && stopFlag > DateComparator.STOP_MINUTE) {
                            returnValue = compare(date1.get(ExtractedDate.SECOND), date2.get(ExtractedDate.SECOND));
                        }
                    }
                }
            }

        }
        return returnValue;
    }

    /**
     * Compares a parameter of two dates. (date1.getYear() and date2.getYear()). <br>
     * If i or k equals -1, then -2 will be returned.<br>
     * Otherwise -1 for i > k, 0 for i=k, 1 for i<k;
     * 
     * @param i
     * @param k
     * @return
     */
    public int compare(int i, int k) {
        int returnValue = -2;
        if (i == k) {
            returnValue = 0;
        } else {
            if (i != -1 && k != -1) {
                if (i < k) {
                    returnValue = 1;
                } else {
                    returnValue = -1;
                }
            }
        }
        return returnValue;
    }

    /**
     * Finds out, until which depth two dates are comparable. <br>
     * Order is year, month, day,hour, minute and second.
     * 
     * @param date1
     * @param date2
     * @return Integer with the value of stop_property. Look for it in static properties.
     */
    public int getCompareDepth(ExtractedDate date1, ExtractedDate date2) {
        int value = -1;
        if (!(date1.get(ExtractedDate.YEAR) == -1 ^ date2.get(ExtractedDate.YEAR) == -1)) {
            value = STOP_YEAR;
            if (!(date1.get(ExtractedDate.MONTH) == -1 ^ date2.get(ExtractedDate.MONTH) == -1)) {
                value = STOP_MONTH;
                if (!(date1.get(ExtractedDate.DAY) == -1 ^ date2.get(ExtractedDate.DAY) == -1)) {
                    value = STOP_DAY;
                    if (!(date1.get(ExtractedDate.HOUR) == -1 ^ date2.get(ExtractedDate.HOUR) == -1)) {
                        value = STOP_HOUR;
                        if (!(date1.get(ExtractedDate.MINUTE) == -1 ^ date2.get(ExtractedDate.MINUTE) == -1)) {
                            value = STOP_MINUTE;
                            if (!(date1.get(ExtractedDate.SECOND) == -1 ^ date2.get(ExtractedDate.SECOND) == -1)) {
                                value = STOP_SECOND;
                            }
                        }
                    }
                }
            }
        }
        return value;
    }

    /**
     * Returns the difference between two extracted dates.<br>
     * If dates can not be compared -1 will be returned. <br>
     * Otherwise difference is calculated to maximal possible depth. (year-month-day-hour-minute-second).<br>
     * Measures of returning value can be set to milliseconds, seconds, minutes, hours and days. There for use static
     * properties.
     * 
     * @param date1
     * @param date2
     * @param measure Found in DateComparator.
     * @return A positive (absolute) difference. To know which date is more actual use <b>compare</b>.
     */
    public double getDifference(ExtractedDate date1, ExtractedDate date2, int measure) {
        double diff = -1;
        int depth = getCompareDepth(date1, date2);
        Calendar cal1 = new GregorianCalendar();
        Calendar cal2 = new GregorianCalendar();

        if (depth > 0) {
            cal1.set(Calendar.YEAR, date1.get(ExtractedDate.YEAR));
            cal2.set(Calendar.YEAR, date2.get(ExtractedDate.YEAR));
            if (depth > STOP_YEAR) {
                cal1.set(Calendar.MONTH, date1.get(ExtractedDate.MONTH));
                cal2.set(Calendar.MONTH, date2.get(ExtractedDate.MONTH));
                if (depth > STOP_MONTH) {
                    cal1.set(Calendar.DAY_OF_MONTH, date1.get(ExtractedDate.DAY));
                    cal2.set(Calendar.DAY_OF_MONTH, date2.get(ExtractedDate.DAY));
                    if (depth > STOP_DAY) {
                        cal1.set(Calendar.HOUR_OF_DAY, date1.get(ExtractedDate.HOUR));
                        cal2.set(Calendar.HOUR_OF_DAY, date2.get(ExtractedDate.HOUR));
                        if (depth > STOP_HOUR) {
                            cal1.set(Calendar.MINUTE, date1.get(ExtractedDate.MINUTE));
                            cal2.set(Calendar.MINUTE, date2.get(ExtractedDate.MINUTE));
                            if (depth > STOP_MINUTE) {
                                cal1.set(Calendar.SECOND, date1.get(ExtractedDate.SECOND));
                                cal2.set(Calendar.SECOND, date2.get(ExtractedDate.SECOND));
                            }
                        }
                    }
                }
            }
            diff = Math.round(Math.abs(cal1.getTimeInMillis() - cal2.getTimeInMillis()) * 100 / measure) / 100;
        }

        return diff;
    }
}
