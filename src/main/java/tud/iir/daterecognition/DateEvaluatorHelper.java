package tud.iir.daterecognition;

import java.util.HashMap;
import java.util.Map.Entry;

import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.helper.DateComparator;
import tud.iir.knowledge.RegExp;

public class DateEvaluatorHelper {

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

    public static <T> HashMap<T, Double> getHighestRate(HashMap<T, Double> dates) {
        HashMap<T, Double> map = new HashMap<T, Double>();
        T date = null;
        double temp = -1;
        for (Entry<T, Double> e : dates.entrySet()) {
            double value = e.getValue();

            if (value > temp) {
                date = e.getKey();
                temp = value;
            }
        }
        map.put(date, temp);
        return map;
    }

}
