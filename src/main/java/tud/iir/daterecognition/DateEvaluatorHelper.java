package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;

import tud.iir.helper.DateComparator;
import tud.iir.knowledge.RegExp;

public class DateEvaluatorHelper {

    public static final int FILTER_IS_IN_RANGE = 1;

    /**
     * Checks if a date is between 13th of November 1990, time 0:00 and now.
     * 
     * @param date
     * @return
     */
    public static boolean isDateInRange(ExtractedDate date) {
        boolean value;
        ExtractedDate begin = new ExtractedDate("1990-11-13T00:00:00Z", RegExp.DATE_ISO8601_YMD_T[1]);
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        ExtractedDate end = ExtractedDateHelper.createActualDate();

        DateComparator comp = new DateComparator();
        return ((comp.compare(begin, date) > -1) && (comp.compare(date, end) > -1));
    }

    public static ArrayList<ExtractedDate> filter(ArrayList<ExtractedDate> dates, int filter) {
        ArrayList<ExtractedDate> temp = dates;
        Iterator<ExtractedDate> iterator = dates.iterator();
        while (iterator.hasNext()) {
            ExtractedDate date = iterator.next();
            switch (filter) {
                case FILTER_IS_IN_RANGE:
                    if (isDateInRange(date)) {
                        temp.add(date);
                    }
                    break;
            }

        }
        return temp;

    }
}
