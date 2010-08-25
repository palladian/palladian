package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.Iterator;

import tud.iir.helper.DateComparator;
import tud.iir.knowledge.RegExp;

public class DateEvaluatorHelper {

    public static final int FILTER_IS_IN_RANGE = 0;
    public static final int FILTER_TECH_URL = ExtractedDate.TECH_URL; // 1
    public static final int FILTER_TECH_HTTP_HEADER = ExtractedDate.TECH_HTTP_HEADER;// 2
    public static final int FILTER_TECH_HTML_HEAD = ExtractedDate.TECH_HTML_HEAD;// 3
    public static final int FILTER_TECH_HTML_STRUC = ExtractedDate.TECH_HTML_STRUC;// 4
    public static final int FILTER_TECH_HTML_CONT = ExtractedDate.TECH_HTML_CONT;// 5
    public static final int FILTER_TECH_REFERENCE = ExtractedDate.TECH_REFERENCE;// 6
    public static final int FILTER_TECH_ARCHIVE = ExtractedDate.TECH_ARCHIVE;// 7

    /**
     * Checks if a date is between 13th of November 1990, time 0:00 and now.
     * 
     * @param date
     * @return
     */
    public static boolean isDateInRange(ExtractedDate date) {
        ExtractedDate begin = new ExtractedDate("1990-11-13T00:00:00Z", RegExp.DATE_ISO8601_YMD_T[1]);
        ExtractedDate end = ExtractedDateHelper.createActualDate();

        DateComparator comp = new DateComparator();
        return ((comp.compare(begin, date) > -1) && (comp.compare(date, end) > -1));
    }

    public static <T> ArrayList<T> filter(ArrayList<T> dates, int filter) {
        ArrayList<T> temp = new ArrayList<T>();
        T date;
        Iterator<T> iterator = dates.iterator();
        while (iterator.hasNext()) {
            date = iterator.next();
            switch (filter) {
                case FILTER_IS_IN_RANGE:
                    if (!isDateInRange((ExtractedDate) date)) {
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
}
