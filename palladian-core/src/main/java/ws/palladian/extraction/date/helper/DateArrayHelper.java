package ws.palladian.extraction.date.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.extraction.date.DateRaterHelper;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.helper.date.dates.AbstractDate;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.helper.date.dates.DateExactness;
import ws.palladian.helper.date.dates.ExtractedDate;

/**
 * Helper functions for arrays consisting extracted dates or subclasses.
 * 
 * @author Martin Gregor
 * 
 */
public class DateArrayHelper {

    /** Filter dates in range (1993 - today). */
    public static final int FILTER_IS_IN_RANGE = 0;
    /** Filter contentDates with key-location in attribute. */
    public static final int FILTER_KEYLOC_ATTR = ContentDate.KEY_LOC_ATTR; // 201
    /** Filter contentDates with key-location in content. */
    public static final int FILTER_KEYLOC_CONT = ContentDate.KEY_LOC_CONTENT;// 202
    /** Filter contentDates without key in attribute nor content. */
    public static final int FILTER_KEYLOC_NO = 203;
    /** Filter dates with year, month and day. */
    public static final int FILTER_FULL_DATE = 204;

    /**
     * Filters an array-list.<br>
     * For filters use this static fields.
     * 
     * @param <T>
     * @param dates
     * @param filter
     * @return
     */
    public static <T extends ExtractedDate> List<T> filter(List<T> dates, int filter) {
        List<T> ret = new ArrayList<T>();
        int tempFilter = filter;
        for (T date : dates) {
            switch (filter) {
                case FILTER_IS_IN_RANGE:
                    if (DateRaterHelper.isDateInRange(date)) {
                        ret.add(date);
                    }
                    break;
                case FILTER_FULL_DATE:
                    if (date.get(AbstractDate.YEAR) != -1 && date.get(AbstractDate.MONTH) != -1
                            && date.get(AbstractDate.DAY) != -1) {

                        ret.add(date);
                    }
                    break;
                case FILTER_KEYLOC_NO:
                    tempFilter = -1;
                case FILTER_KEYLOC_CONT:
                case FILTER_KEYLOC_ATTR:
                    //if (date.getType().equals(DateType.ContentDate)) {
                    if (date instanceof ContentDate) {
                        int keyloc = ((ContentDate)date).get(ContentDate.KEYWORDLOCATION);
                        if (keyloc == tempFilter) {
                            ret.add(date);
                        }
                    }
                    break;
            }

        }
        return ret;
    }

//    /**
//     * @deprecated Use {@link #filter(List, Class)} instead.
//     */
//    @Deprecated
//    public static <T extends ExtractedDate> List<T> filter(List<T> dates, DateType filter) {
//        List<T> ret = new ArrayList<T>();
//        for (T date : dates) {
//            if (date.getType().equals(filter)) {
//                ret.add(date);
//            }
//        }
//        return ret;
//    }
    
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

//    /**
//     * Filters an array-list.<br>
//     * For filters use this static fields.
//     * 
//     * @param <T>
//     * @param dates
//     * @param filter
//     * @return
//     */
//    public static <T> Map<T, Double> filter(Map<T, Double> dates, int filter) {
//        HashMap<T, Double> temp = new HashMap<T, Double>();
//        T date;
//        Double rate;
//        for (Entry<T, Double> e : dates.entrySet()) {
//            date = e.getKey();
//            rate = e.getValue();
//            switch (filter) {
//                case FILTER_IS_IN_RANGE:
//                    if (DateRaterHelper.isDateInRange((ExtractedDate) date)) {
//                        temp.put(date, rate);
//                    }
//                    break;
//            }
//
//        }
//        return temp;
//
//    }

//    /**
//     * Filters an array-list.<br>
//     * For filters use this static fields.
//     * 
//     * @param <T>
//     * @param dates
//     * @param filter
//     * @return
//     */
//    public static <T> Map<T, Double> filter(Map<T, Double> dates, DateType filter) {
//        HashMap<T, Double> temp = new HashMap<T, Double>();
//        T date;
//        Double rate;
//        for (Entry<T, Double> e : dates.entrySet()) {
//            date = e.getKey();
//            rate = e.getValue();
//            if (((ExtractedDate) date).getType().equals(filter)) {
//                temp.put(date, rate);
//            }
//        }
//        return temp;
//
//    }

//    public static <T> List<T> filterFormat(List<T> dates, String format) {
//        ArrayList<T> temp = new ArrayList<T>();
//        T date;
//        Iterator<T> iterator = dates.iterator();
//        while (iterator.hasNext()) {
//            date = iterator.next();
//            if (((ExtractedDate) date).getFormat().equalsIgnoreCase(format)) {
//                temp.add(date);
//            }
//        }
//        return temp;
//    }

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
    public static <T extends ExtractedDate> List<List<T>> arrangeByDate(List<T> dates, DateExactness compareDepth) {
        List<List<T>> result = new ArrayList<List<T>>();
        for (int datesIndex = 0; datesIndex < dates.size(); datesIndex++) {
            boolean sameDatestamp = false;
            T date = dates.get(datesIndex);
            for (int resultIndex = 0; resultIndex < result.size(); resultIndex++) {
                T firstDate = result.get(resultIndex).get(0);
                DateComparator dc = new DateComparator(compareDepth);
                int compare = dc.compare(firstDate, date);
                if (compare == 0) {
                    result.get(resultIndex).add(date);
                    sameDatestamp = true;
                    break;
                }
            }
            if (!sameDatestamp) {
                List<T> newDate = new ArrayList<T>();
                newDate.add(date);
                result.add(newDate);
            }
        }
        return result;
    }

//    /**
//     * Group equal dates in array lists. <br>
//     * E.g. d1=May 2010; d2=05.2010; d3=01.05.10; d4=01st May '10 --> (d1&d2) &
//     * (d3&d4). <br>
//     * Every date can be only in one group.<br>
//     * A group is a array list of dates.
//     * 
//     * @param <T>
//     * @param dates
//     *            Arraylist of dates.
//     * @return A arraylist of groups, that are arraylists too.
//     */
//    public static <T extends ExtractedDate> List<List<T>> arrangeByDate(List<T> dates) {
//        return arrangeByDate(dates, DateExactness.DAY);
//    }

//    /**
//     * Orders a map by dates.
//     * 
//     * @param <T>
//     * @param dates
//     * @return
//     */
//    public static <T> List<Map<T, Double>> arrangeMapByDate(Map<T, Double> dates) {
//        return arrangeMapByDate(dates, DateComparator.STOP_DAY);
//    }

//    /**
//     * Orders a map by dates.
//     * 
//     * @param <T>
//     * @param dates
//     * @param stopFlag
//     *            At what exactness a comparison should stop. Use {@link DateComparator} static fields.
//     * @return
//     */
//    public static <T> List<Map<T, Double>> arrangeMapByDate(Map<T, Double> dates, int stopFlag) {
//        ArrayList<Map<T, Double>> result = new ArrayList<Map<T, Double>>();
//        DateComparator dc = new DateComparator();
//        for (Entry<T, Double> e : dates.entrySet()) {
//            boolean sameDatestamp = false;
//            T date = e.getKey();
//            for (int resultIndex = 0; resultIndex < result.size(); resultIndex++) {
//                T firstDate = null;
//                for (Entry<T, Double> temp : result.get(resultIndex).entrySet()) {
//                    firstDate = temp.getKey();
//                    break;
//                }
//                int compare = dc.compare((ExtractedDate) firstDate, (ExtractedDate) date, stopFlag);
//                if (compare == 0) {
//                    result.get(resultIndex).put(date, e.getValue());
//                    sameDatestamp = true;
//                    break;
//                }
//            }
//            if (!sameDatestamp) {
//                HashMap<T, Double> newDate = new HashMap<T, Double>();
//                newDate.put(date, e.getValue());
//                result.add(newDate);
//            }
//        }
//        return result;
//    }

//    /**
//     * Count how often a date is in a list. <br>
//     * Not the object, but the exact date.<br>
//     * If the date-object is also in the list, it will not count.<br>
//     * <br>
//     * E.g.: list={date1,date2,date3} and date1 = date2 != date3. <br>
//     * Look up for date1, the returning value will be 1 and not 2!
//     * 
//     * @param <T>
//     * @param date
//     * @param dates
//     * @return
//     */
//    public static <T, V> int countDates(T date, List<V> dates) {
//
//        return countDates(date, dates, -1);
//        /*
//         * int count = 0; DateComparator dc = new DateComparator(); for (int i =
//         * 0; i < dates.size(); i++) { if (!date.equals(dates.get(i))) { int
//         * stopFlag = Math.min(((ExtractedDate) date).getExactness(),
//         * ((ExtractedDate) dates.get(i)) .getExactness()); if
//         * (dc.compare((ExtractedDate) date, (ExtractedDate) dates.get(i),
//         * stopFlag) == 0) { count++; } } } return count;
//         */
//    }

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
    public static <T extends ExtractedDate, V extends ExtractedDate> int countDates(T date, List<V> dates, int stopFlag) {
        int count = 0;
        for (int i = 0; i < dates.size(); i++) {
            if (!date.equals(dates.get(i))) {
                int tempStopFlag = stopFlag;
                if (tempStopFlag == -1) {
                    tempStopFlag = Math.min(date.getExactness().getValue(),
                            dates.get(i).getExactness().getValue());
                }
                DateComparator dc = new DateComparator(DateExactness.byValue(tempStopFlag));
                if (dc.compare(date, dates.get(i)) == 0) {
                    count++;
                }
            }
        }
        return count;
    }

//    /**
//     * Count how often a date is in a list. <br>
//     * Not the object, but the exact date.<br>
//     * If the date-object is also in the list, it will not count.<br>
//     * <br>
//     * E.g.: list={date1,date2,date3} and date1 = date2 != date3. <br>
//     * Look up for date1, the returning value will be 1 and not 2!
//     * 
//     * @param <T>
//     * @param date
//     * @param dates
//     * @return
//     */
//    public static <T> int countDates(T date, Map<T, Double> dates) {
//        return countDates(date, dates, DateComparator.STOP_DAY);
//    }

    public static <T extends ExtractedDate> int countDates(T date, Map<T, Double> dates, int stopFlag) {
        int count = 0;
        for (Entry<T, Double> e : dates.entrySet()) {
            if (!date.equals(e.getKey())) {
                int tempStopFlag = stopFlag;
                if (tempStopFlag == -1) {
                    tempStopFlag = Math.min(date.getExactness().getValue(),
                            e.getKey().getExactness().getValue());
                }
                DateComparator dc = new DateComparator(DateExactness.byValue(tempStopFlag));
                if (dc.compare(date, e.getKey()) == 0) {
                    count++;
                }
            }
        }
        return count;
    }

//    /**
//     * Same as printeDateArray() with filter of techniques. These are found in
//     * ExtracedDate as static properties. <br>
//     * And a format, found as second value of RegExp.
//     * 
//     * @param <T>
//     * 
//     * @param dates
//     * @param filterTechnique
//     * @param format
//     */
//    public static <T extends ExtractedDate> void printDateArray(List<T> dates, Class<T> filter, String format) {
//        List<T> temp = dates;
//        if (filter != null) {
//            temp = filter(dates, filter);
//        }
//
//        for (T date : temp) {
//            if (format == null || format.equals(((ExtractedDate) date).getFormat())) {
//                System.out.println(date.toString());
//                System.out
//                        .println("------------------------------------------------------------------------------------------------");
//            }
//        }
//    }

//    /**
//     * Same as printeDateArray() with filter of techniques. These are found in
//     * ExtracedDate as static properties. <br>
//     * And a format, found as second value of RegExp.
//     * 
//     * @param <T>
//     * 
//     * @param dates
//     * @param filterTechnique
//     * @param format
//     */
//    private static <T extends ExtractedDate> void printDateArray(List<T> dates, int filterTechnique, String format) {
//        List<T> temp = dates;
//        if (filterTechnique > 0) {
//            temp = filter(dates, filterTechnique);
//        }
//
//        Iterator<T> dateIterator = temp.iterator();
//        while (dateIterator.hasNext()) {
//
//            T date = dateIterator.next();
//            if (format == null || format.equals(((ExtractedDate) date).getFormat())) {
//                System.out.println(date.toString());
//                System.out
//                        .println("------------------------------------------------------------------------------------------------");
//            }
//        }
//    }

//    /**
//     * System.out.println for each date in dates, with some properties.
//     * 
//     * @param <T>
//     * 
//     * @param dates
//     */
//    public static void printDateArray(List<? extends ExtractedDate> dates) {
////        printDateArray(dates, 0);
//        for (ExtractedDate extractedDate : dates) {
//            System.out.println(extractedDate.toString());
//            System.out
//                    .println("------------------------------------------------------------------------------------------------");
//        }
//    }

//    /**
//     * Same as printeDateArray() with filter of techniques. These are found in
//     * ExtracedDate as static properties.
//     * 
//     * @param <T>
//     * 
//     * @param dates
//     * @param filterTechnique
//     */
//    private static void printDateArray(List<? extends ExtractedDate> dates, int filterTechnique) {
//        printDateArray(dates, filterTechnique, null);
//    }

//    /**
//     * Same as printeDateArray() with filter of techniques. These are found in
//     * ExtracedDate as static properties.
//     * 
//     * @param <T>
//     * 
//     * @param dates
//     * @param filterTechnique
//     */
//    public static <T extends ExtractedDate> void printDateArray(List<T> dates, Class<T> filter) {
//        printDateArray(dates, filter, null);
//    }

//    /**
//     * Remove dates from the array.
//     * 
//     * @param <T>
//     * @param dates
//     * @param format
//     * @return
//     */
//    public static <T> List<T> removeFormat(List<T> dates, String format) {
//        ArrayList<T> result = new ArrayList<T>();
//        for (int i = 0; i < dates.size(); i++) {
//            T date = dates.get(i);
//            if (!((ExtractedDate) date).getFormat().equalsIgnoreCase(format)) {
//                result.add(date);
//            }
//        }
//        return result;
//    }

//    /**
//     * Prints an entry-array.
//     * 
//     * @param <T>
//     * @param dateMap
//     */
//    public static <T> void printDateMap(Entry<T, Double>[] dateMap) {
//        printDateMap(dateMap, null);
//    }

//    /**
//     * Prints an entry-array.<br>
//     * You got possibility to filter first.
//     * 
//     * @param <T>
//     * @param dateMap
//     * @param filter
//     */
//    public static <T> void printDateMap(Entry<T, Double>[] dateMap, DateType filter) {
//        for (int i = 0; i < dateMap.length; i++) {
//            T date = dateMap[i].getKey();
//            String dateString = ((ExtractedDate) date).getDateString();
//            String normDate = ((ExtractedDate) date).getNormalizedDateString();
//            String type = ExtractedDateHelper.getTypString(((ExtractedDate) date).getType());
//            String keyword = "";
//            int dist = -1;
//            switch (((ExtractedDate) date).getType()) {
//                case ContentDate:
//                    keyword = ((ContentDate) date).getKeyword();
//                    dist = ((ContentDate) date).get(ContentDate.DISTANCE_DATE_KEYWORD);
//                    break;
//                case StructureDate:
//                    keyword = ((StructureDate) date).getKeyword();
//                    break;
//                case MetaDate:
//                    keyword = ((MetaDate) date).getKeyword();
//                    break;
//            }
//            if (((ExtractedDate) date).getType().equals(filter) || filter == null) {
//                System.out.println("Rate: " + dateMap[i].getValue() + " Type: " + type + " Keyword: " + keyword
//                        + " Distance: " + dist);
//                System.out.println(dateString + " --> " + normDate);
//                System.out
//                        .println("-------------------------------------------------------------------------------------");
//            }
//        }
//    }

//    /**
//     * Print hashmap of dates.
//     * 
//     * @param <T>
//     * @param dateMap
//     */
//    public static <T> void printDateMap(Map<T, Double> dateMap) {
//        printDateMap(dateMap, null);
//    }

//    /**
//     * Print hashmap of dates.<br>
//     * With possibility of filtering.
//     * 
//     * @param <T>
//     * @param dateMap
//     * @param filter
//     */
//    public static <T> void printDateMap(Map<T, Double> dateMap, DateType filter) {
//        for (Entry<T, Double> e : dateMap.entrySet()) {
//            T date = e.getKey();
//            String dateString = ((ExtractedDate) date).getDateString();
//            String normDate = ((ExtractedDate) date).getNormalizedDateString();
//            String type = ExtractedDateHelper.getTypString(((ExtractedDate) date).getType());
//            String keyword = "";
//            int dist = -1;
//            switch (((ExtractedDate) date).getType()) {
//                case ContentDate:
//                    keyword = ((ContentDate) date).getKeyword();
//                    dist = ((ContentDate) date).get(ContentDate.DISTANCE_DATE_KEYWORD);
//
//                    break;
//                case StructureDate:
//                    keyword = ((StructureDate) date).getKeyword();
//                    break;
//                case MetaDate:
//                    keyword = ((MetaDate) date).getKeyword();
//                    break;
//            }
//            if (((ExtractedDate) e.getKey()).getType().equals(filter) || filter == null) {
//                System.out.println("Rate: " + e.getValue() + " Type: " + type + " Keyword: " + keyword + " Distance: "
//                        + dist);
//                System.out.println(dateString + " --> " + normDate);
//                System.out
//                        .println("-------------------------------------------------------------------------------------");
//            }
//        }
//    }

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
    public static <T extends ExtractedDate> List<T> getRatedDates(List<T> dates, double rate, boolean include) {
        List<T> result = new ArrayList<T>();
        for (int i = 0; i < dates.size(); i++) {
            T date = dates.get(i);
            if (date.getRate() == rate == include) {
                result.add(date);
            }
        }
        return result;
    }

//    /**
//     * Returns an array of dates, that have a given rate.
//     * 
//     * @param <T>
//     * @param dates
//     * @param rate
//     * @return
//     */
//    public static <T> Map<T, Double> getRatedDatesMap(Map<T, Double> dates, double rate) {
//        return getRatedDatesMap(dates, rate, true);
//    }

//    /**
//     * Returns an array of dates, that have the given rate. (include = true) <br>
//     * Returns an array of dates, that have <b>not</b> the given rate. (include
//     * = false) <br>
//     * 
//     * @see DateArrayHelper.getRatedDates
//     * @param <T>
//     * @param dates
//     * @param rate
//     * @param include
//     * @return
//     */
//    public static <T> Map<T, Double> getRatedDatesMap(Map<T, Double> dates, double rate, boolean include) {
//        HashMap<T, Double> result = new HashMap<T, Double>();
//        for (Entry<T, Double> e : dates.entrySet()) {
//            if (e.getValue() == rate == include) {
//                result.put(e.getKey(), e.getValue());
//            }
//        }
//        return result;
//    }

//    /**
//     * Returns an array of dates that are equal to a given date.
//     * 
//     * @param <T>
//     * @param date
//     * @param dates
//     * @return
//     */
//    public static <T> List<T> getSameDates(ExtractedDate date, List<T> dates) {
//        return getSameDates(date, dates, DateComparator.STOP_DAY);
//    }

//    public static <T> List<T> getSameDates(ExtractedDate date, List<T> dates, int stopFlag) {
//        DateComparator dc = new DateComparator();
//        ArrayList<T> result = new ArrayList<T>();
//        for (int i = 0; i < dates.size(); i++) {
//            if (dc.compare(date, (ExtractedDate) dates.get(i), stopFlag) == 0) {
//                result.add(dates.get(i));
//            }
//        }
//        return result;
//    }

//    /**
//     * Returns a hashmap of date are equal to given date.
//     * 
//     * @param <T>
//     * @param date
//     * @param dates
//     * @return
//     */
//    public static <T> Map<T, Double> getSameDatesMap(ExtractedDate date, Map<T, Double> dates) {
//        return getSameDatesMap(date, dates, DateComparator.STOP_DAY);
//    }

    /**
     * Returns a hashmap of date are equal to given date.<br>
     * Date comparison stops at stopFlag.
     * 
     * @param <T>
     * @param date
     * @param dates
     * @return
     */
    public static <T> Map<T, Double> getSameDatesMap(ExtractedDate date, Map<T, Double> dates, DateExactness compareDepth) {
        DateComparator dc = new DateComparator(compareDepth);
        HashMap<T, Double> result = new HashMap<T, Double>();
        for (Entry<T, Double> e : dates.entrySet()) {
            if (dc.compare(date, (ExtractedDate) e.getKey()) == 0) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

//    /**
//     * Order by rate.
//     * 
//     * @param <T>
//     * @param dates
//     * @return
//     */
//    public static <T> Entry<T, Double>[] orderHashMap(Map<T, Double> dates) {
//        return orderHashMap(dates, false);
//    }

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
    public static <T, V> Entry<T, V>[] mapToArray(Map<T, V> map) {
        Entry<T, V>[] array = new Entry[map.size()];
        int i = 0;
        for (Entry<T, V> e : map.entrySet()) {
            array[i] = e;
            i++;
        }
        return array;
    }

    /**
     * * Keys of a hashmap will be put in a list.<br>
     * Ignoring value part of hashmap.
     * 
     * @param <T>
     * @param <V>
     * @param map
     * @return
     */
    public static <T, V> List<T> mapToList(Map<T, V> map) {
        ArrayList<T> array = new ArrayList<T>();
        for (Entry<T, V> e : map.entrySet()) {
            array.add(e.getKey());
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

//    /**
//     * Returns Hashmap of Dates, where there exactness is equal or greater to
//     * given exactness.
//     * 
//     * @param <T>
//     * @param dates
//     * @param exactness
//     * @return
//     */
//    public static <T> Map<T, Double> getExacterDates(Map<T, Double> dates, int exactness) {
//        HashMap<T, Double> resultDates = new HashMap<T, Double>();
//        for (Entry<T, Double> e : dates.entrySet()) {
//            if (((ExtractedDate) e.getKey()).getExactness() >= exactness) {
//                resultDates.put(e.getKey(), e.getValue());
//            }
//        }
//        return resultDates;
//    }

//    /**
//     * Finds out the greatest exactness of the given dates. <br>
//     * Returns all dates with this greatest exactness.
//     * 
//     * @param <T>
//     * @param dates
//     * @return
//     */
//    public static <T> List<T> getExactestDates(Map<T, Double> dates) {
//        ArrayList<T> result = new ArrayList<T>();
//        HashMap<T, Double> exactedDates = new HashMap<T, Double>();
//        for (Entry<T, Double> e : dates.entrySet()) {
//            exactedDates.put(e.getKey(), ((ExtractedDate) e.getKey()).getExactness() * 1.0);
//        }
//        Entry<T, Double>[] orderedHashMap = orderHashMap(exactedDates, true);
//        if (orderedHashMap.length > 0) {
//            double greatestExactness = orderedHashMap[0].getValue();
//            for (Entry<T, Double> e : exactedDates.entrySet()) {
//                if (e.getValue() == greatestExactness) {
//                    result.add(e.getKey());
//                }
//            }
//        }
//        return result;
//    }

//    /**
//     * Finds out the greatest exactness of the given dates. <br>
//     * Returns all dates with this greatest exactness.
//     * 
//     * @param <T>
//     * @param dates
//     * @return
//     */
//    public static <T> List<T> getExactestDates(List<T> dates) {
//        ArrayList<T> result = new ArrayList<T>();
//        HashMap<T, Double> exactedDates = new HashMap<T, Double>();
//        for (int i = 0; i < dates.size(); i++) {
//            exactedDates.put(dates.get(i), ((ExtractedDate) dates.get(i)).getExactness() * 1.0);
//        }
//        Entry<T, Double>[] orderedHashMap = orderHashMap(exactedDates, true);
//        if (orderedHashMap.length > 0) {
//            double greatestExactness = orderedHashMap[0].getValue();
//            for (Entry<T, Double> e : exactedDates.entrySet()) {
//                if (e.getValue() == greatestExactness) {
//                    result.add(e.getKey());
//                }
//            }
//        }
//        return result;
//    }

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

    /**
     * Returns first element of a hashmap.
     * 
     * @param <T>
     * @param map
     * @return
     */
    // WTF? What is a "first" element of a HashMap?!?!?!
    public static <T, V> T getFirstElement(Map<T, V> map) {
        T result = null;
        for (Entry<T, V> e : map.entrySet()) {
            result = e.getKey();
        }
        return result;
    }

    public static <T> List<T> removeNull(List<T> list) {
        ArrayList<T> returnList = new ArrayList<T>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) != null) {
                returnList.add(list.get(i));
            }
        }
        return returnList;
    }

//    /**
//     * If some rates are greater then one, use this method to normalize them.
//     * 
//     * @param <T>
//     * @param dates
//     */
//    public static <T> Map<T, Double> normalizeRate(Map<T, Double> dates) {
//        Map<T, Double> returnDates = dates;
//        double highestRate = DateArrayHelper.getHighestRate(returnDates);
//        if (highestRate > 1.0) {
//            for (Entry<T, Double> e : returnDates.entrySet()) {
//                returnDates.put(e.getKey(), Math.round(e.getValue() / highestRate * 10000) / 10000.0);
//            }
//        }
//        return returnDates;
//    }
}
