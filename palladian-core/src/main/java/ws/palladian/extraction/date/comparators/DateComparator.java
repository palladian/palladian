package ws.palladian.extraction.date.comparators;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import ws.palladian.helper.date.dates.ExtractedDate;

/**
 * <p>
 * This class gives the ability to compare dates by age. Be careful when using it as a comparator in sort functions.
 * Dates can have different exactness, means one has a time and the other no day. For more information see at particular
 * methods.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class DateComparator implements Comparator<ExtractedDate> {
    
    private final DateExactness compareDepth;

//    /** Compare will stop after year. Value = 1. */
//    public static final int STOP_YEAR = 1;
//    /** Compare will stop after month. Value = 2. */
//    public static final int STOP_MONTH = 2;
//    /** Compare will stop after day. Value = 3. */
//    public static final int STOP_DAY = 3;
//    /** Compare will stop after hour. Value = 4. */
//    public static final int STOP_HOUR = 4;
//    /** Compare will stop after minute. Value = 5. */
//    public static final int STOP_MINUTE = 5;
//    /** Compare will not stop. (After second there are no more comparable values. Value = 6. */
//    public static final int STOP_SECOND = 6;
//    /** Use for methods providing a dynamic stop, depending on exactness of each date. Value = -1. */
//    public static final int STOP_DYNAMIC = -1;

    // TODO replace by TimeUnit?
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
    
    public DateComparator(DateExactness compareDepth) {
        this.compareDepth = compareDepth;
    }
    
    public DateComparator() {
        this(DateExactness.SECOND);
    }

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
//        return compare(date1, date2, STOP_SECOND);
        int returnValue;
        returnValue = compare(date1.get(ExtractedDate.YEAR), date2.get(ExtractedDate.YEAR));
        if (returnValue == 0 && compareDepth.getValue() > DateExactness.YEAR.getValue()) {
            returnValue = compare(date1.get(ExtractedDate.MONTH), date2.get(ExtractedDate.MONTH));
            if (returnValue == 0 && compareDepth.getValue() > DateExactness.MONTH.getValue()) {
                returnValue = compare(date1.get(ExtractedDate.DAY), date2.get(ExtractedDate.DAY));
                if (returnValue == 0 && compareDepth.getValue() > DateExactness.DAY.getValue()) {
                    returnValue = compare(date1.get(ExtractedDate.HOUR), date2.get(ExtractedDate.HOUR));
                    if (returnValue == 0 && compareDepth.getValue() > DateExactness.HOUR.getValue()) {
                        returnValue = compare(date1.get(ExtractedDate.MINUTE), date2.get(ExtractedDate.MINUTE));
                        if (returnValue == 0 && compareDepth.getValue() > DateExactness.MINUTE.getValue()) {
                            returnValue = compare(date1.get(ExtractedDate.SECOND), date2.get(ExtractedDate.SECOND));
                        }
                    }
                }
            }

        }
        return returnValue;
    }

//    /**
//     * Like <b>compare(ExtractedDate date1, ExtractedDate date2)</b>, but compares only until a given depth. <br>
//     * For e.g. usually 12.04.2007 and April 2007 can not be compared. But with stopflag STOP_DAY only year and month
//     * will be compared.<br>
//     * So normal compare would return -2, but this time the result is 0.
//     * 
//     * @param date1
//     * @param date2
//     * @param stopFlag Depth of comparing. Values are given as static constant in this class. (STOP_...)
//     * @return
//     */
//    public int compare(AbstractDate date1, AbstractDate date2, int stopFlag) {
//        int returnValue;
//        returnValue = compare(date1.get(ExtractedDate.YEAR), date2.get(ExtractedDate.YEAR));
//        if (returnValue == 0 && stopFlag > DateComparator.STOP_YEAR) {
//            returnValue = compare(date1.get(ExtractedDate.MONTH), date2.get(ExtractedDate.MONTH));
//            if (returnValue == 0 && stopFlag > DateComparator.STOP_MONTH) {
//                returnValue = compare(date1.get(ExtractedDate.DAY), date2.get(ExtractedDate.DAY));
//                if (returnValue == 0 && stopFlag > DateComparator.STOP_DAY) {
//                    returnValue = compare(date1.get(ExtractedDate.HOUR), date2.get(ExtractedDate.HOUR));
//                    if (returnValue == 0 && stopFlag > DateComparator.STOP_HOUR) {
//                        returnValue = compare(date1.get(ExtractedDate.MINUTE), date2.get(ExtractedDate.MINUTE));
//                        if (returnValue == 0 && stopFlag > DateComparator.STOP_MINUTE) {
//                            returnValue = compare(date1.get(ExtractedDate.SECOND), date2.get(ExtractedDate.SECOND));
//                        }
//                    }
//                }
//            }
//
//        }
//        return returnValue;
//    }

//    /**
//     * Ignores exactness of dates.<br>
//     * 2007-10-01 is before 2007-10-01 12:00
//     * 
//     * @param date1
//     * @param date2
//     * @param ignoreComparable
//     * @return
//     */
//    private int compare(ExtractedDate date1, ExtractedDate date2, boolean ignoreComparable) {
//        int compare = compare(date1, date2, ignoreComparable, CompareDepth.SECOND);
//        if (compare == -2) {
//            compare = 1;
//        } else if (compare == -3) {
//            compare = -1;
//        }
//        return compare;
//    }

//    /**
//     * Ignores exactness of dates. But you can set a maximum exactness until it will be compared.<br>
//     * 2007-10-01 is before 2007-10-01 12:00
//     * 
//     * @param date1
//     * @param date2
//     * @param ignoreComparable
//     * @return
//     */
//    private int compare(ExtractedDate date1, ExtractedDate date2, boolean ignoreComparable, CompareDepth compareDepth) {
//        int compare;
//        if (ignoreComparable) {
//            compare = compare(date1, date2);
//        } else {
//            compare = compare(date1, date2, compareDepth);
//        }
//        return compare;
//    }

    /**
     * Compares a parameter of two dates. (date1.getYear() and date2.getYear()). <br>
     * If i or k equals -1, then -2 will be returned.<br>
     * Otherwise -1 for i > k, 0 for i=k, 1 for i&lt; k; <br>
     * If k=i=-1 -> 0 will be returned.
     * 
     * @param i
     * @param k
     * @return
     */
    private int compare(int i, int k) {
        // int returnValue = -2;
        // if (i == k) {
        // returnValue = 0;
        // } else {
        // if (i != -1 && k != -1) {
        // if (i < k) {
        // returnValue = 1;
        // } else {
        // returnValue = -1;
        // }
        // } else {
        // if (i == -1) {
        // returnValue = -2;
        // } else {
        // returnValue = -3;
        // }
        // }
        // }
        // return returnValue;

        // for Java 1.7 compatibility I had to change the above code, as it would violate the Comparator's contract.
        // Considering the available test cases, the following implementation also works, but I haven't tested it
        // extensively. If we have unset date particles (i.e. field equals -1) we return Integer.MAX_VALUE, or
        // Integer.MIN_VALUE instead of generally returning a value of -2. -- Philipp, 2011-12-15
        int result;
        if (i == -1 && k == -1) {
            result = 0;
        } else if (i == -1) {
            result = Integer.MAX_VALUE;
        } else if (k == -1) {
            result = Integer.MIN_VALUE;
        } else {
            result = Integer.valueOf(k).compareTo(i);
        }
        return result;
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
        int depth = DateExactness.getCommonExactness(date1, date2).getValue();
        Calendar cal1 = new GregorianCalendar();
        Calendar cal2 = new GregorianCalendar();

        if (depth > 0) {
            cal1.set(Calendar.YEAR, date1.get(ExtractedDate.YEAR));
            cal2.set(Calendar.YEAR, date2.get(ExtractedDate.YEAR));
            if (depth > DateExactness.YEAR.getValue()) {
                cal1.set(Calendar.MONTH, date1.get(ExtractedDate.MONTH));
                cal2.set(Calendar.MONTH, date2.get(ExtractedDate.MONTH));
                if (depth > DateExactness.MONTH.getValue()) {
                    cal1.set(Calendar.DAY_OF_MONTH, date1.get(ExtractedDate.DAY));
                    cal2.set(Calendar.DAY_OF_MONTH, date2.get(ExtractedDate.DAY));
                    if (depth > DateExactness.DAY.getValue()) {
                        cal1.set(Calendar.HOUR_OF_DAY, date1.get(ExtractedDate.HOUR));
                        cal2.set(Calendar.HOUR_OF_DAY, date2.get(ExtractedDate.HOUR));
                        if (depth > DateExactness.HOUR.getValue()) {
                            cal1.set(Calendar.MINUTE, date1.get(ExtractedDate.MINUTE));
                            cal2.set(Calendar.MINUTE, date2.get(ExtractedDate.MINUTE));
                            if (depth > DateExactness.MINUTE.getValue()) {
                                cal1.set(Calendar.SECOND, date1.get(ExtractedDate.SECOND));
                                cal2.set(Calendar.SECOND, date2.get(ExtractedDate.SECOND));
                            }
                        }
                    }
                }
            }
            diff = Math.round(Math.abs(cal1.getTimeInMillis() - cal2.getTimeInMillis()) * 100.0 / measure) / 100.0;
        }

        return diff;
    }

//    /**
//     * Filters a set of dates out of an array, that have same extraction date like a given date.
//     * 
//     * @param <T> Type of array of dates.
//     * @param <V> Type of given date.
//     * @param date defines the extraction date.
//     * @param dates array to be filtered.
//     * @return Array of dates, that are equal to the date.
//     */
//    public <T, V> List<T> getEqualDate(V date, List<T> dates) {
//        ArrayList<T> returnDate = new ArrayList<T>();
//        for (int i = 0; i < dates.size(); i++) {
//            int compare = compare((ExtractedDate)date, (ExtractedDate)dates.get(i), STOP_DAY);
//            if (compare == 0) {
//                returnDate.add(dates.get(i));
//            }
//        }
//        return returnDate;
//    }

//    /**
//     * Oder dates, oldest first.
//     * 
//     * @param <T>
//     * @param dates
//     * @return
//     */
//    private <T extends ExtractedDate> List<T> orderDates(List<T> dates) {
//        return orderDates(dates, false);
//    }

    /**
     * <p>Orders a {@link List} of dates, beginning with oldest date.</p>
     * 
     * @param <T>
     * @param dates
     * @param reverse <code>true</code> is youngest first. <code>false</code> is oldest first.
     * @return A sorted {@link List} of dates.
     * @deprecated Use {@link Collections#sort(List)} with {@link DateComparator} instead.
     */
    @Deprecated
    public <T extends ExtractedDate> List<T> orderDates(Collection<T> dates, boolean reverse) {
//        T[] result = orderDatesArray(dates);
//        ArrayList<T> resultList = new ArrayList<T>();
//        if (reverse) {
//            for (int i = 0; i < result.length; i++) {
//                resultList.add(result[i]);
//            }
//        } else {
//            for (int i = result.length - 1; i >= 0; i--) {
//                resultList.add(result[i]);
//            }
//        }
//
//        return resultList;
        List<T> result = new ArrayList<T>(dates);
        Comparator<ExtractedDate> comparator = new DateComparator();
        if (!reverse) {
            comparator = Collections.reverseOrder(comparator);
        }
        Collections.sort(result, comparator);
        return result;
    }

    /**
     * Oder dates, oldest first.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    @Deprecated
    public <T extends ExtractedDate> List<T> orderDates(Map<T, Double> dates) {
//        ArrayList<T> temp = new ArrayList<T>();
//        for (Entry<T, Double> e : dates.entrySet()) {
//            temp.add(e.getKey());
//        }
//        return orderDates(temp);
        return orderDates(dates, false);
    }

    /**
     * Orders a hashmap of dates into an arraylist, beginning with oldest date.<br>
     * Flag for reverse:
     * 
     * @param <T>
     * @param dates
     * @param reverse True is youngest first. False is oldest first.
     * @return
     */
    @Deprecated
    public <T extends ExtractedDate> List<T> orderDates(Map<T, Double> dates, boolean reverse) {
//        List<T> temp = new ArrayList<T>();
//        for (Entry<T, Double> e : dates.entrySet()) {
//            temp.add(e.getKey());
//        }
//        return orderDates(temp, reverse);
        return orderDates(dates.keySet(), reverse);
    }

//    /**
//     * Orders a datelist, beginning with oldest date.
//     * 
//     * @param <T>
//     * @param dates
//     * @return
//     */
//    @SuppressWarnings("unchecked")
//    private <T extends ExtractedDate> T[] orderDatesArray(Collection<T> dates) {
//        T[] dateArray = (T[])dates.toArray(new ExtractedDate[dates.size()]);
//        quicksort(0, dateArray.length - 1, dateArray);
//        return dateArray;
//
//    }

//    private <T extends ExtractedDate> void quicksort(int left, int right, T[] dates) {
//        if (left < right) {
//            int divide = divide(left, right, dates);
//            quicksort(left, divide - 1, dates);
//            quicksort(divide + 1, right, dates);
//        }
//    }

//    private <T extends ExtractedDate> int divide(int left, int right, T[] dates) {
//        int i = left;
//        int j = right - 1;
//        T pivot = dates[right];
//        while (i < j) {
////            while (compare(dates[i], pivot, true) < 1 && i < right) {
//            while (compare(dates[i], pivot) < 1 && i < right) {
//                i++;
//            }
////            while (compare(dates[j], pivot, true) > -1 && j > left) {
//            while (compare(dates[j], pivot) > -1 && j > left) {
//                j--;
//            }
//            if (i < j) {
//                T help = dates[i];
//                dates[i] = dates[j];
//                dates[j] = help;
//            }
//        }
////        if (compare(dates[i], pivot, true) > 0) {
//        if (compare(dates[i], pivot) > 0) {
//            T help = dates[i];
//            dates[i] = dates[right];
//            dates[right] = help;
//        }
//        return i;
//    }

//    /**
//     * Returns oldest date.
//     * 
//     * @param <T>
//     * @param dates
//     * @return
//     */
//    public <T extends ExtractedDate> T getOldestDate(Map<T, Double> dates) {
//        List<T> orderDates = orderDates(dates, false);
//        T date = null;
//        if (orderDates.size() > 0) {
//            date = orderDates.get(0);
//        }
//        return date;
//
//    }
//
//    /**
//     * Returns youngest dates.
//     * 
//     * @param <T>
//     * @param dates
//     * @return
//     */
//    public <T extends ExtractedDate> T getYoungestDate(Map<T, Double> dates) {
//        List<T> orderDates = orderDates(dates, true);
//        T date = null;
//        if (orderDates.size() > 0) {
//            date = orderDates.get(0);
//        }
//        return date;
//
//    }

    /**
     * Returns oldest date.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    public <T extends ExtractedDate> T getOldestDate(Collection<T> dates) {
        List<T> orderDates = orderDates(dates, false);
        T date = null;
        if (orderDates.size() > 0) {
            date = orderDates.get(0);
        }
        return date;
    }

    /**
     * Returns youngest dates.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    public <T extends ExtractedDate> T getYoungestDate(Collection<T> dates) {
        List<T> orderDates = orderDates(dates, true);
        T date = null;
        if (orderDates.size() > 0) {
            date = orderDates.get(0);
        }
        return date;
    }
}
