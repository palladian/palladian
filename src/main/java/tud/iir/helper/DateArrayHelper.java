package tud.iir.helper;

import java.util.ArrayList;
import java.util.Iterator;

import tud.iir.daterecognition.DateEvaluatorHelper;
import tud.iir.daterecognition.dates.ExtractedDate;

/**
 * Helper functions for arrays consisting extracted dates or subclasses.
 * 
 * @author Martin Gregor
 * 
 */
public class DateArrayHelper {

    public static final int FILTER_IS_IN_RANGE = 0;
    public static final int FILTER_TECH_URL = ExtractedDate.TECH_URL; // 1
    public static final int FILTER_TECH_HTTP_HEADER = ExtractedDate.TECH_HTTP_HEADER;// 2
    public static final int FILTER_TECH_HTML_HEAD = ExtractedDate.TECH_HTML_HEAD;// 3
    public static final int FILTER_TECH_HTML_STRUC = ExtractedDate.TECH_HTML_STRUC;// 4
    public static final int FILTER_TECH_HTML_CONT = ExtractedDate.TECH_HTML_CONT;// 5
    public static final int FILTER_TECH_REFERENCE = ExtractedDate.TECH_REFERENCE;// 6
    public static final int FILTER_TECH_ARCHIVE = ExtractedDate.TECH_ARCHIVE;// 7

    /**
     * Filters an array-list.
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
                    if (DateEvaluatorHelper.isDateInRange((ExtractedDate) date)) {
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
     * Groups dates in array lists, that are equal. <br>
     * E.g. d1=May 2010; d2=05.2010; d3=01.05.10; d4=01st May '10 --> (d1&d2) & (d3&d4). <br>
     * Every date can be only in one group.<br>
     * A group is a array list of dates.
     * 
     * @param <T>
     * @param dates Arraylist of dates.
     * @return A arraylist of groups, that are arraylists too.
     */
    public static <T> ArrayList<ArrayList<T>> arrangeByDate(ArrayList<T> dates) {
        ArrayList<ArrayList<T>> result = new ArrayList<ArrayList<T>>();
        DateComparator dc = new DateComparator();
        for (int datesIndex = 0; datesIndex < dates.size(); datesIndex++) {
            boolean sameDatestamp = false;
            T date = dates.get(datesIndex);
            for (int resultIndex = 0; resultIndex < result.size(); resultIndex++) {
                T firstDate = result.get(resultIndex).get(0);
                int compare = dc.compare((ExtractedDate) firstDate, (ExtractedDate) date, DateComparator.STOP_DAY);
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
            System.out.println("enter filter");
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
     * Removes dates out of the array.
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
}
