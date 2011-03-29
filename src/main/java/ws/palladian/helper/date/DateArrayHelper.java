package ws.palladian.helper.date;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import ws.palladian.daterecognition.DateRaterHelper;
import ws.palladian.daterecognition.ExtractedDateHelper;
import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.HTTPDate;
import ws.palladian.daterecognition.dates.HeadDate;
import ws.palladian.daterecognition.dates.StructureDate;

/**
 * Helper functions for arrays consisting extracted dates or subclasses.
 * 
 * @author Martin Gregor
 * 
 */
public class DateArrayHelper {

    /** Filter dates in range (1993 - today). */
    public static final int FILTER_IS_IN_RANGE = 0;
    /** Filter URLDates. */
    public static final int FILTER_TECH_URL = ExtractedDate.TECH_URL; // 1
    /** Filter HTTPHeaderDates. */
    public static final int FILTER_TECH_HTTP_HEADER = ExtractedDate.TECH_HTTP_HEADER;// 2
    /** Filter HTMLHeadDates. */
    public static final int FILTER_TECH_HTML_HEAD = ExtractedDate.TECH_HTML_HEAD;// 3
    /** Filter HTMLStructureDates. */
    public static final int FILTER_TECH_HTML_STRUC = ExtractedDate.TECH_HTML_STRUC;// 4
    /** Filter HTMLContentDates. */
    public static final int FILTER_TECH_HTML_CONT = ExtractedDate.TECH_HTML_CONT;// 5
    /** Filter ReferenceDates. */
    public static final int FILTER_TECH_REFERENCE = ExtractedDate.TECH_REFERENCE;// 6
    /** Filter ArchiveDates. */
    public static final int FILTER_TECH_ARCHIVE = ExtractedDate.TECH_ARCHIVE;// 7
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
    public static <T> ArrayList<T> filter(ArrayList<T> dates, int filter) {
        ArrayList<T> temp = new ArrayList<T>();
        T date;
        Iterator<T> iterator = dates.iterator();
        while (iterator.hasNext()) {
            date = iterator.next();
            switch (filter) {
                case FILTER_IS_IN_RANGE:
                    if (DateRaterHelper.isDateInRange((ExtractedDate) date)) {
                        temp.add(date);
                    }
                    break;
                case FILTER_TECH_URL:
                case FILTER_TECH_HTTP_HEADER:
                case FILTER_TECH_HTML_HEAD:
                case FILTER_TECH_HTML_STRUC:
                case FILTER_TECH_HTML_CONT:
                case FILTER_TECH_REFERENCE:
                case FILTER_TECH_ARCHIVE:
                    if (((ExtractedDate) date).getType() == filter) {
                        temp.add(date);
                    }
                    break;
                case FILTER_KEYLOC_ATTR:
                    if (((ExtractedDate) date).getType() == FILTER_TECH_HTML_CONT) {
                        if (((ContentDate) date).get(ContentDate.KEYWORDLOCATION) == filter) {
                            temp.add(date);
                        }
                    }
                    break;
                case FILTER_KEYLOC_CONT:
                    if (((ExtractedDate) date).getType() == FILTER_TECH_HTML_CONT) {
                        if (((ContentDate) date).get(ContentDate.KEYWORDLOCATION) == filter) {
                            temp.add(date);
                        }
                    }
                    break;
                case FILTER_KEYLOC_NO:
                    if (((ExtractedDate) date).getType() == FILTER_TECH_HTML_CONT) {
                        if (((ContentDate) date).get(ContentDate.KEYWORDLOCATION) == -1) {
                            temp.add(date);
                        }
                    }
                    break;
                case FILTER_FULL_DATE:
                    if (((ExtractedDate) date).get(ExtractedDate.YEAR) != -1
                            && ((ExtractedDate) date).get(ExtractedDate.MONTH) != -1
                            && ((ExtractedDate) date).get(ExtractedDate.DAY) != -1) {

                        temp.add(date);
                    }
                    break;
            }

        }
        return temp;

    }

    /**
     * Filters an array-list.<br>
     * For filters use this static fields.
     * 
     * @param <T>
     * @param dates
     * @param filter
     * @return
     */
    public static <T> HashMap<T, Double> filter(HashMap<T, Double> dates, int filter) {
        HashMap<T, Double> temp = new HashMap<T, Double>();
        T date;
        Double rate;
        for (Entry<T, Double> e : dates.entrySet()) {
            date = e.getKey();
            rate = e.getValue();
            switch (filter) {
                case FILTER_IS_IN_RANGE:
                    if (DateRaterHelper.isDateInRange((ExtractedDate) date)) {
                        temp.put(date, rate);
                    }
                    break;
                case FILTER_TECH_URL:
                case FILTER_TECH_HTTP_HEADER:
                case FILTER_TECH_HTML_HEAD:
                case FILTER_TECH_HTML_STRUC:
                case FILTER_TECH_HTML_CONT:
                case FILTER_TECH_REFERENCE:
                case FILTER_TECH_ARCHIVE:
                    if (((ExtractedDate) date).getType() == filter) {
                        temp.put(date, rate);
                    }
                    break;

            }

        }
        return temp;

    }

    public static <T> ArrayList<T> filterFormat(ArrayList<T> dates, String format) {
        ArrayList<T> temp = new ArrayList<T>();
        T date;
        Iterator<T> iterator = dates.iterator();
        while (iterator.hasNext()) {
            date = iterator.next();
            if (((ExtractedDate) date).getFormat().equalsIgnoreCase(format)) {
                temp.add(date);
            }
        }
        return temp;
    }

    /**
     * Group equal dates in array lists. <br>
     * E.g. d1=May 2010; d2=05.2010; d3=01.05.10; d4=01st May '10 --> (d1&d2) & (d3&d4). <br>
     * Every date can be only in one group.<br>
     * A group is a array list of dates.
     * 
     * @param <T>
     * @param dates Arraylist of dates.
     * @return A arraylist of groups, that are arraylists too.
     */
    public static <T> ArrayList<ArrayList<T>> arrangeByDate(ArrayList<T> dates, int stopFlag) {
        ArrayList<ArrayList<T>> result = new ArrayList<ArrayList<T>>();
        DateComparator dc = new DateComparator();
        for (int datesIndex = 0; datesIndex < dates.size(); datesIndex++) {
            boolean sameDatestamp = false;
            T date = dates.get(datesIndex);
            for (int resultIndex = 0; resultIndex < result.size(); resultIndex++) {
                T firstDate = result.get(resultIndex).get(0);
                int compare = dc.compare((ExtractedDate) firstDate, (ExtractedDate) date, stopFlag);
                if (compare == 0) {
                    result.get(resultIndex).add(date);
                    sameDatestamp = true;
                    break;
                }
            }
            if (!sameDatestamp) {
                ArrayList<T> newDate = new ArrayList<T>();
                newDate.add(date);
                result.add(newDate);
            }
        }
        return result;
    }

    /**
     * Group equal dates in array lists. <br>
     * E.g. d1=May 2010; d2=05.2010; d3=01.05.10; d4=01st May '10 --> (d1&d2) & (d3&d4). <br>
     * Every date can be only in one group.<br>
     * A group is a array list of dates.
     * 
     * @param <T>
     * @param dates Arraylist of dates.
     * @return A arraylist of groups, that are arraylists too.
     */
    public static <T> ArrayList<ArrayList<T>> arrangeByDate(ArrayList<T> dates) {
        return arrangeByDate(dates, DateComparator.STOP_DAY);
    }

    /**
     * Orders a map by dates.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    public static <T> ArrayList<HashMap<T, Double>> arrangeMapByDate(HashMap<T, Double> dates) {
        return arrangeMapByDate(dates, DateComparator.STOP_DAY);
    }

    /**
     * Orders a map by dates.
     * 
     * @param <T>
     * @param dates
     * @param stopFlag At what exactness a comparison should stop. Use {@link DateComparator} static fields.
     * @return
     */
    public static <T> ArrayList<HashMap<T, Double>> arrangeMapByDate(HashMap<T, Double> dates, int stopFlag) {
        ArrayList<HashMap<T, Double>> result = new ArrayList<HashMap<T, Double>>();
        DateComparator dc = new DateComparator();
        for (Entry<T, Double> e : dates.entrySet()) {
            boolean sameDatestamp = false;
            T date = e.getKey();
            for (int resultIndex = 0; resultIndex < result.size(); resultIndex++) {
                T firstDate = null;
                for (Entry<T, Double> temp : result.get(resultIndex).entrySet()) {
                    firstDate = temp.getKey();
                    break;
                }
                int compare = dc.compare((ExtractedDate) firstDate, (ExtractedDate) date, stopFlag);
                if (compare == 0) {
                    result.get(resultIndex).put(date, e.getValue());
                    sameDatestamp = true;
                    break;
                }
            }
            if (!sameDatestamp) {
                HashMap<T, Double> newDate = new HashMap<T, Double>();
                newDate.put(date, e.getValue());
                result.add(newDate);
            }
        }
        return result;
    }

    /**
     * Count how often a date is in a list. <br>
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
    public static <T,V> int countDates(T date, ArrayList<V> dates) {
        
    	return countDates(date, dates, -1);
    	/*int count = 0;
        DateComparator dc = new DateComparator();
        for (int i = 0; i < dates.size(); i++) {
            if (!date.equals(dates.get(i))) {
                int stopFlag = Math.min(((ExtractedDate) date).getExactness(), ((ExtractedDate) dates.get(i))
                        .getExactness());
                if (dc.compare((ExtractedDate) date, (ExtractedDate) dates.get(i), stopFlag) == 0) {
                    count++;
                }
            }
        }
        return count;*/
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
    public static <T,V> int countDates(T date, ArrayList<V> dates, int stopFlag) {
        int count = 0;
        DateComparator dc = new DateComparator();
        for (int i = 0; i < dates.size(); i++) {
            if (!date.equals(dates.get(i))) {
                int tempStopFlag = stopFlag;
                if (tempStopFlag == -1) {
                    tempStopFlag = Math.min(((ExtractedDate) date).getExactness(), ((ExtractedDate) dates.get(i))
                            .getExactness());
                }
                if (dc.compare((ExtractedDate) date, (ExtractedDate) dates.get(i), tempStopFlag) == 0) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * Count how often a date is in a list. <br>
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
    public static <T> int countDates(T date, HashMap<T, Double> dates) {
        return countDates(date, dates, DateComparator.STOP_DAY);
    }

    public static <T> int countDates(T date, HashMap<T, Double> dates, int stopFlag) {
        int count = 0;
        DateComparator dc = new DateComparator();
        for (Entry<T, Double> e : dates.entrySet()) {
            if (!date.equals(e.getKey())) {
                int tempStopFlag = stopFlag;
                if (tempStopFlag == -1) {
                    tempStopFlag = Math.min(((ExtractedDate) date).getExactness(), ((ExtractedDate) e.getKey())
                            .getExactness());
                }
                if (dc.compare((ExtractedDate) date, (ExtractedDate) e.getKey(), tempStopFlag) == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    

    /**
     * Same as printeDateArray() with filter of techniques. These are found in ExtracedDate as static properties. <br>
     * And a format, found as second value of RegExp.
     * 
     * @param <T>
     * 
     * @param dates
     * @param filterTechnique
     * @param format
     */
    public static <T> void printDateArray(ArrayList<T> dates, int filterTechnique, String format) {
        ArrayList<T> temp = dates;
        if (filterTechnique > 0) {
            temp = filter(dates, filterTechnique);
        }

        Iterator<T> dateIterator = temp.iterator();
        while (dateIterator.hasNext()) {

            T date = dateIterator.next();
            if (format == null || format == ((ExtractedDate) date).getFormat()) {
                System.out.println(date.toString());
                System.out
                        .println("------------------------------------------------------------------------------------------------");
            }
        }
    }

    /**
     * System.out.println for each date in dates, with some properties.
     * 
     * @param <T>
     * 
     * @param dates
     */
    public static <T> void printDateArray(ArrayList<T> dates) {
        printDateArray(dates, 0);
    }

    /**
     * Same as printeDateArray() with filter of techniques. These are found in ExtracedDate as static properties.
     * 
     * @param <T>
     * 
     * @param dates
     * @param filterTechnique
     */
    public static <T> void printDateArray(ArrayList<T> dates, int filterTechnique) {
        printDateArray(dates, filterTechnique, null);
    }

    /**
     * Remove dates from the array.
     * 
     * @param <T>
     * @param dates
     * @param format
     * @return
     */
    public static <T> ArrayList<T> removeFormat(ArrayList<T> dates, String format) {
        ArrayList<T> result = new ArrayList<T>();
        for (int i = 0; i < dates.size(); i++) {
            T date = dates.get(i);
            if (!((ExtractedDate) date).getFormat().equalsIgnoreCase(format)) {
                result.add(date);
            }
        }
        return result;
    }

    /**
     * Prints an entry-array.
     * 
     * @param <T>
     * @param dateMap
     */
    public static <T> void printDateMap(Entry<T, Double>[] dateMap) {
        printDateMap(dateMap, 0);
    }

    /**
     * Prints an entry-array.<br>
     * You got possibility to filter first.
     * 
     * @param <T>
     * @param dateMap
     * @param filter
     */
    public static <T> void printDateMap(Entry<T, Double>[] dateMap, int filter) {
        for (int i = 0; i < dateMap.length; i++) {
            T date = dateMap[i].getKey();
            String dateString = ((ExtractedDate) date).getDateString();
            String normDate = ((ExtractedDate) date).getNormalizedDateString();
            String type = ExtractedDateHelper.getTypString(((ExtractedDate) date).getType());
            String keyword = "";
            int dist = -1;
            switch (((ExtractedDate) date).getType()) {
                case ExtractedDate.TECH_HTML_CONT:
                    keyword = ((ContentDate) date).getKeyword();
                    dist = ((ContentDate) date).get(ContentDate.DISTANCE_DATE_KEYWORD);
                    break;
                case ExtractedDate.TECH_HTML_STRUC:
                    keyword = ((StructureDate) date).getKeyword();
                    break;
                case ExtractedDate.TECH_HTML_HEAD:
                    keyword = ((HeadDate) date).getKeyword();
                    break;
                case ExtractedDate.TECH_HTTP_HEADER:
                    keyword = ((HTTPDate) date).getKeyword();
                    break;
            }
            if (((ExtractedDate) date).getType() == filter || filter == 0) {
                System.out.println("Rate: " + dateMap[i].getValue() + " Type: " + type + " Keyword: " + keyword
                        + " Distance: " + dist);
                System.out.println(dateString + " --> " + normDate);
                System.out
                        .println("-------------------------------------------------------------------------------------");
            }
        }
    }

    /**
     * Print hashmap of dates.
     * 
     * @param <T>
     * @param dateMap
     */
    public static <T> void printDateMap(HashMap<T, Double> dateMap) {
        printDateMap(dateMap, 0);
    }

    /**
     * Print hashmap of dates.<br>
     * With possibility of filtering.
     * 
     * @param <T>
     * @param dateMap
     * @param filter
     */
    public static <T> void printDateMap(HashMap<T, Double> dateMap, int filter) {
        for (Entry<T, Double> e : dateMap.entrySet()) {
            T date = e.getKey();
            String dateString = ((ExtractedDate) date).getDateString();
            String normDate = ((ExtractedDate) date).getNormalizedDateString();
            String type = ExtractedDateHelper.getTypString(((ExtractedDate) date).getType());
            String keyword = "";
            int dist = -1;
            switch (((ExtractedDate) date).getType()) {
                case ExtractedDate.TECH_HTML_CONT:
                    keyword = ((ContentDate) date).getKeyword();
                    dist = ((ContentDate) date).get(ContentDate.DISTANCE_DATE_KEYWORD);

                    break;
                case ExtractedDate.TECH_HTML_STRUC:
                    keyword = ((StructureDate) date).getKeyword();
                    break;
                case ExtractedDate.TECH_HTML_HEAD:
                    keyword = ((HeadDate) date).getKeyword();
                    break;
                case ExtractedDate.TECH_HTTP_HEADER:
                    keyword = ((HTTPDate) date).getKeyword();
                    break;
            }
            if (((ExtractedDate) e.getKey()).getType() == filter || filter == 0) {
                System.out.println("Rate: " + e.getValue() + " Type: " + type + " Keyword: " + keyword + " Distance: "
                        + dist);
                System.out.println(dateString + " --> " + normDate);
                System.out
                        .println("-------------------------------------------------------------------------------------");
            }
        }
    }

    /**
     * Returns an array of dates, that have a given rate.
     * 
     * @param <T>
     * @param dates
     * @param rate
     * @return
     */
    public static <T> ArrayList<T> getRatedDates(HashMap<T, Double> dates, double rate) {
        return getRatedDates(dates, rate, true);
    }

    /**
     * Returns an array of dates, that have the given rate. (include = true) <br>
     * Returns an array of dates, that have <b>not</b> the given rate. (include = false) <br>
     * 
     * @see DateArrayHelper.getRatedDates
     * @param <T>
     * @param dates
     * @param rate
     * @param include
     * @return
     */
    public static <T> ArrayList<T> getRatedDates(HashMap<T, Double> dates, double rate, boolean include) {
        ArrayList<T> result = new ArrayList<T>();
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
    public static <T> ArrayList<T> getRatedDates(ArrayList<T> dates, double rate) {
        return getRatedDates(dates, rate, true);
    }

    /**
     * Returns all dates with or without given rate.
     * 
     * @param <T>
     * @param dates
     * @param rate
     * @param include True for dates with rate. False for dates without rate.
     * @return
     */
    public static <T> ArrayList<T> getRatedDates(ArrayList<T> dates, double rate, boolean include) {
        ArrayList<T> result = new ArrayList<T>();
        for (int i = 0; i < dates.size(); i++) {
            T date = dates.get(i);
            if (((ExtractedDate) date).getRate() == rate == include) {
                result.add(date);
            }
        }
        return result;
    }

    /**
     * Returns an array of dates, that have a given rate.
     * 
     * @param <T>
     * @param dates
     * @param rate
     * @return
     */
    public static <T> HashMap<T, Double> getRatedDatesMap(HashMap<T, Double> dates, double rate) {
        return getRatedDatesMap(dates, rate, true);
    }

    /**
     * Returns an array of dates, that have the given rate. (include = true) <br>
     * Returns an array of dates, that have <b>not</b> the given rate. (include = false) <br>
     * 
     * @see DateArrayHelper.getRatedDates
     * @param <T>
     * @param dates
     * @param rate
     * @param include
     * @return
     */
    public static <T> HashMap<T, Double> getRatedDatesMap(HashMap<T, Double> dates, double rate, boolean include) {
        HashMap<T, Double> result = new HashMap<T, Double>();
        for (Entry<T, Double> e : dates.entrySet()) {
            if (e.getValue() == rate == include) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    /**
     * Returns an array of dates that are equal to a given date.
     * 
     * @param <T>
     * @param date
     * @param dates
     * @return
     */
    public static <T> ArrayList<T> getSameDates(ExtractedDate date, ArrayList<T> dates) {
        return getSameDates(date, dates, DateComparator.STOP_DAY);
    }

    public static <T> ArrayList<T> getSameDates(ExtractedDate date, ArrayList<T> dates, int stopFlag) {
        DateComparator dc = new DateComparator();
        ArrayList<T> result = new ArrayList<T>();
        for (int i = 0; i < dates.size(); i++) {
            if (dc.compare(date, (ExtractedDate) dates.get(i), stopFlag) == 0) {
                result.add(dates.get(i));
            }
        }
        return result;
    }

    /**
     * Returns a hashmap of date are equal to given date.
     * 
     * @param <T>
     * @param date
     * @param dates
     * @return
     */
    public static <T> HashMap<T, Double> getSameDatesMap(ExtractedDate date, HashMap<T, Double> dates) {
        return getSameDatesMap(date, dates, DateComparator.STOP_DAY);
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
    public static <T> HashMap<T, Double> getSameDatesMap(ExtractedDate date, HashMap<T, Double> dates, int stopFlag) {
        DateComparator dc = new DateComparator();
        HashMap<T, Double> result = new HashMap<T, Double>();
        for (Entry<T, Double> e : dates.entrySet()) {
            if (dc.compare(date, (ExtractedDate) e.getKey(), stopFlag) == 0) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    /**
     * Order by rate.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    public static <T> Entry<T, Double>[] orderHashMap(HashMap<T, Double> dates) {
        return orderHashMap(dates, false);
    }

    /**
     * Order by rate. Lowest is first.
     * 
     * @param <T>
     * @param dates
     * @param reverse
     * @return
     */
    public static <T> Entry<T, Double>[] orderHashMap(HashMap<T, Double> dates, boolean reverse) {
        Entry<T, Double>[] dateArray = hashMapToArray(dates);
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
     * Creates an array of entries of hashmap.
     * 
     * @param <T>
     * @param <V>
     * @param map
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T, V> Entry<T, V>[] hashMapToArray(HashMap<T, V> map) {
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
    public static <T, V> ArrayList<T> hashMapToArrayList(HashMap<T, V> map) {
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
    public static <T> boolean isAllZero(HashMap<T, Double> dates) {
        boolean isAllZero = true;
        for (Entry<T, Double> e : dates.entrySet()) {
            if (e.getValue() > 0) {
                isAllZero = false;
                break;
            }
        }
        return isAllZero;
    }

    /**
     * Returns Hashmap of Dates, where there exactness is equal or greater to given exactness.
     * 
     * @param <T>
     * @param dates
     * @param exactness
     * @return
     */
    public static <T> HashMap<T, Double> getExacterDates(HashMap<T, Double> dates, int exactness) {
        HashMap<T, Double> resultDates = new HashMap<T, Double>();
        for (Entry<T, Double> e : dates.entrySet()) {
            if (((ExtractedDate) e.getKey()).getExactness() >= exactness) {
                resultDates.put(e.getKey(), e.getValue());
            }
        }
        return resultDates;
    }

    /**
     * Finds out the greatest exactness of the given dates. <br>
     * Returns all dates with this greatest exactness.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    public static <T> ArrayList<T> getExactestDates(HashMap<T, Double> dates) {
        ArrayList<T> result = new ArrayList<T>();
        HashMap<T, Double> exactedDates = new HashMap<T, Double>();
        for (Entry<T, Double> e : dates.entrySet()) {
            exactedDates.put(e.getKey(), ((ExtractedDate) e.getKey()).getExactness() * 1.0);
        }
        Entry<T, Double>[] orderedHashMap = orderHashMap(exactedDates, true);
        if (orderedHashMap.length > 0) {
            double greatestExactness = orderedHashMap[0].getValue();
            for (Entry<T, Double> e : exactedDates.entrySet()) {
                if (e.getValue() == greatestExactness) {
                    result.add(e.getKey());
                }
            }
        }
        return result;
    }

    /**
     * Finds out the greatest exactness of the given dates. <br>
     * Returns all dates with this greatest exactness.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    public static <T> ArrayList<T> getExactestDates(ArrayList<T> dates) {
        ArrayList<T> result = new ArrayList<T>();
        HashMap<T, Double> exactedDates = new HashMap<T, Double>();
        for (int i = 0; i < dates.size(); i++) {
            exactedDates.put(dates.get(i), ((ExtractedDate) dates.get(i)).getExactness() * 1.0);
        }
        Entry<T, Double>[] orderedHashMap = orderHashMap(exactedDates, true);
        if (orderedHashMap.length > 0) {
            double greatestExactness = orderedHashMap[0].getValue();
            for (Entry<T, Double> e : exactedDates.entrySet()) {
                if (e.getValue() == greatestExactness) {
                    result.add(e.getKey());
                }
            }
        }
        return result;
    }

    /**
     * Finds out the greatest exactness of the given dates. <br>
     * Returns all dates with this greatest exactness.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    public static <T> HashMap<T, Double> getExactestMap(HashMap<T, Double> dates) {
        HashMap<T, Double> result = new HashMap<T, Double>();
        HashMap<T, Double> exactedDates = new HashMap<T, Double>();
        for (Entry<T, Double> e : dates.entrySet()) {
            exactedDates.put(e.getKey(), ((ExtractedDate) e.getKey()).getExactness() * 1.0);
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
    public static <T> double getHighestRate(HashMap<T, Double> dates) {
        double result = 0;
        for (Entry<T, Double> e : dates.entrySet()) {
            result = Math.max(result, e.getValue());
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
    public static <T> double getHighestRate(ArrayList<T> dates) {
        double result = 0;
        for (int i = 0; i < dates.size(); i++) {
            result = Math.max(result, ((ExtractedDate) dates.get(i)).getRate());
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
    public static <T, V> T getFirstElement(HashMap<T, V> map) {
        T result = null;
        for (Entry<T, V> e : map.entrySet()) {
            result = e.getKey();
        }
        return result;
    }
    
    public static  <T> ArrayList<T> removeNull(ArrayList<T> list){
    	ArrayList<T> returnList = new ArrayList<T>();
    	for(int i=0; i<list.size(); i++){
    		if(list.get(i) != null){
    			returnList.add(list.get(i));
    		}
    	}
    	return returnList;
    }
    
    /**
     * If some rates are greater then one, use this method to normalize them.
     * 
     * @param <T>
     * @param dates
     */
    public static <T> HashMap<T, Double> normalizeRate(HashMap<T, Double> dates) {
    	HashMap<T, Double> returnDates = dates;
        double highestRate = DateArrayHelper.getHighestRate(returnDates);
        if (highestRate > 1.0) {
            for (Entry<T, Double> e : returnDates.entrySet()) {
            	returnDates.put(e.getKey(), Math.round(e.getValue() / highestRate * 10000) / 10000.0);
            }
        }
        return returnDates;
    }
}
