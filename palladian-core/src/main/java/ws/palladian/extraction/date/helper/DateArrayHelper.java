package ws.palladian.extraction.date.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.extraction.date.DateRaterHelper;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.ExtractedDate;

/**
 * Helper functions for arrays consisting extracted dates or subclasses.
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class DateArrayHelper {

    @SuppressWarnings("unchecked")
    public static <T extends ExtractedDate> List<T> filter(List<? extends ExtractedDate> dates, Class<T> filter) {
        List<T> ret = new ArrayList<T>();
        for (ExtractedDate date : dates) {
            if (filter.isInstance(date)) {
                ret.add((T)date);
            }
        }
        return ret;
    }
    
    public static <T extends ExtractedDate> List<T> filterByRange(List<T> dates) {
        List<T> result = new ArrayList<T>(dates);
        CollectionHelper.filter(result, new Filter<T>() {
            @Override
            public boolean accept(T date) {
                return DateRaterHelper.isDateInRange(date);
            }
        });
        return result;
    }
    
    public static <T extends ExtractedDate> List<T> filterFullDate(List<T> dates) {
        List<T> result = new ArrayList<T>(dates);
        CollectionHelper.filter(result, new Filter<T>() {
            @Override
            public boolean accept(T date) {
                return date.get(ExtractedDate.YEAR) != -1 && date.get(ExtractedDate.MONTH) != -1
                        && date.get(ExtractedDate.DAY) != -1;
            }
        });
        return result;
    }

    /**
     * Group equal dates in array lists. <br>
     * E.g. d1=May 2010; d2=05.2010; d3=01.05.10; d4=01st May '10 --> (d1&d2) &
     * (d3&d4). <br>
     * Every date can be only in one group.<br>
     * A group is a array list of dates.
     * 
     * @param <T>
     * @param dates
     *            Arraylist of dates.
     * @return A arraylist of groups, that are arraylists too.
     */
    public static <T extends ExtractedDate> List<List<T>> cluster(List<T> dates, DateExactness compareDepth) {
        List<List<T>> clusters = new ArrayList<List<T>>();
        DateComparator dc = new DateComparator(compareDepth);
        for (int datesIndex = 0; datesIndex < dates.size(); datesIndex++) {
            boolean sameDatestamp = false;
            T date = dates.get(datesIndex);
            for (int resultIndex = 0; resultIndex < clusters.size(); resultIndex++) {
                T firstDate = clusters.get(resultIndex).get(0);
                int compare = dc.compare(firstDate, date);
                if (compare == 0) {
                    clusters.get(resultIndex).add(date);
                    sameDatestamp = true;
                    break;
                }
            }
            if (!sameDatestamp) {
                List<T> newDate = new ArrayList<T>();
                newDate.add(date);
                clusters.add(newDate);
            }
        }
        return clusters;
    }

    /**
     * Count how often a date is in a list.<br>
     * Dates will compared up to depth of stopFlag. <br>
     * Not the object, but the exact date.<br>
     * If the date-object is also in the list, it will not count.<br>
     * <br>
     * E.g.: list={date1,date2,date3} and date1 = date2 != date3. <br>
     * Look up for date1, the returning value will be 1 and not 2!
     * 
     * @param <T>
     * @param date
     * @param dates
     * @return
     */
    public static int countDates(ExtractedDate date, List<? extends ExtractedDate> dates, DateExactness exactness) {
        int count = 0;
        for (ExtractedDate currentDate : dates) {
            if (date.equals(currentDate)) {
                continue;
            }
            DateExactness thisExactness = exactness;
            if (exactness == DateExactness.UNSET) {
                thisExactness = DateExactness.getCommonExactness(date.getExactness(), currentDate.getExactness());
            }
            DateComparator dc = new DateComparator(thisExactness);
            if (dc.compare(date, currentDate) == 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns an array of dates, that have a given rate.
     * 
     * @param <T>
     * @param dates
     * @param rate
     * @return
     */
    public static <T extends ExtractedDate> List<T> getRatedDates(Map<T, Double> dates, double rate) {
        return getRatedDates(dates, rate, true);
    }

    /**
     * Returns an array of dates, that have the given rate. (include = true) <br>
     * Returns an array of dates, that have <b>not</b> the given rate. (include
     * = false) <br>
     * 
     * @see DateArrayHelper.getRatedDates
     * @param <T>
     * @param dates
     * @param rate
     * @param include
     * @return
     */
    public static <T extends ExtractedDate> List<T> getRatedDates(Map<T, Double> dates, double rate, boolean include) {
        List<T> result = new ArrayList<T>();
        for (Entry<T, Double> e : dates.entrySet()) {
            if (e.getValue() == rate == include) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    /**
     * Returns all dates with given rate.
     * 
     * @param <T>
     * @param dates
     * @param rate
     * @return
     */
    public static <T extends ExtractedDate> List<T> getRatedDates(List<T> dates, double rate) {
        return getRatedDates(dates, rate, true);
    }

    /**
     * Returns all dates with or without given rate.
     * 
     * @param <T>
     * @param dates
     * @param rate
     * @param include
     *            True for dates with rate. False for dates without rate.
     * @return
     */
    private static <T extends ExtractedDate> List<T> getRatedDates(List<T> dates, double rate, boolean include) {
        List<T> result = new ArrayList<T>();
        for (int i = 0; i < dates.size(); i++) {
            T date = dates.get(i);
            if (date.getRate() == rate == include) {
                result.add(date);
            }
        }
        return result;
    }

    /**
     * Returns a hashmap of date are equal to given date.<br>
     * Date comparison stops at stopFlag.
     * 
     * @param <T>
     * @param date
     * @param dates
     * @return
     */
    public static <T extends ExtractedDate> Map<T, Double> getSameDatesMap(ExtractedDate date, Map<T, Double> dates,
            DateExactness compareDepth) {
        DateComparator dc = new DateComparator(compareDepth);
        Map<T, Double> result = new HashMap<T, Double>();
        for (Entry<T, Double> e : dates.entrySet()) {
            if (dc.compare(date, e.getKey()) == 0) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    /**
     * Order by rate. Lowest is first.
     * 
     * @param <T>
     * @param dates
     * @param reverse
     * @return
     */
    public static <T> Entry<T, Double>[] orderHashMap(Map<T, Double> dates, boolean reverse) {
        Entry<T, Double>[] dateArray = mapToArray(dates);
        quicksort(0, dateArray.length - 1, dateArray);
        if (reverse) {
            @SuppressWarnings("unchecked")
            Entry<T, Double>[] temp = new Entry[dateArray.length];
            for (int i = 0; i < dateArray.length; i++) {
                temp[i] = dateArray[dateArray.length - 1 - i];
            }
            dateArray = temp;
        }
        return dateArray;
    }

    private static <T> void quicksort(int left, int right, Entry<T, Double>[] dates) {

        if (left < right) {
            int divide = divide(left, right, dates);
            quicksort(left, divide - 1, dates);
            quicksort(divide + 1, right, dates);
        }
    }

    private static <T> int divide(int left, int right, Entry<T, Double>[] dates) {
        int i = left;
        int j = right - 1;
        Entry<T, Double> pivot = dates[right];
        while (i < j) {
            while (dates[i].getValue() < pivot.getValue() && i < right) {
                i++;
            }
            while (dates[j].getValue() >= pivot.getValue() && j > left) {
                j--;
            }
            if (i < j) {
                Entry<T, Double> help = dates[i];
                dates[i] = dates[j];
                dates[j] = help;
            }
        }
        if (dates[i].getValue() > pivot.getValue()) {
            Entry<T, Double> help = dates[i];
            dates[i] = dates[right];
            dates[right] = help;
        }
        return i;
    }

    /**
     * 
     * Creates an array of entries of Map.
     * 
     * @param <T>
     * @param <V>
     * @param map
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <T, V> Entry<T, V>[] mapToArray(Map<T, V> map) {
        Entry<T, V>[] array = new Entry[map.size()];
        int i = 0;
        for (Entry<T, V> e : map.entrySet()) {
            array[i] = e;
            i++;
        }
        return array;
    }

    /**
     * Check if all values of hashmap are zero.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    public static boolean isAllZero(Map<?, Double> dates) {
        for (Double value : dates.values()) {
            if (value > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds out the greatest exactness of the given dates. <br>
     * Returns all dates with this greatest exactness.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    public static <T extends ExtractedDate> Map<T, Double> getExactestMap(Map<T, Double> dates) {
        Map<T, Double> result = new HashMap<T, Double>();
        Map<T, Double> exactedDates = new HashMap<T, Double>();
        for (Entry<T, Double> e : dates.entrySet()) {
            exactedDates.put(e.getKey(), e.getKey().getExactness().getValue() * 1.0);
        }
        Entry<T, Double>[] orderedHashMap = orderHashMap(exactedDates, true);
        if (orderedHashMap.length > 0) {
            double greatestExactness = orderedHashMap[0].getValue();
            for (Entry<T, Double> e : exactedDates.entrySet()) {
                if (e.getValue() == greatestExactness) {
                    result.put(e.getKey(), dates.get(e.getKey()));
                }
            }
        }
        return result;
    }

    /**
     * Returns the highest rate in a map.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    public static double getHighestRate(Map<?, Double> dates) {
        double result = 0;
        for (Double value : dates.values()) {
            result = Math.max(result, value);
        }
        return result;
    }

    /**
     * Returns the highest rate in a ArraylList of ExtractedDate.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    public static double getHighestRate(List<? extends ExtractedDate> dates) {
        double result = 0;
        for (ExtractedDate date : dates) {
            result = Math.max(result, (date.getRate()));
        }
        return result;
    }
}
