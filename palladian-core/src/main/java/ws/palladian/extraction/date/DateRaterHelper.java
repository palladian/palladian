package ws.palladian.extraction.date;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.comparators.DateComparator.CompareDepth;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.ExtractedDateHelper;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.html.HtmlHelper;

/**
 * This class contains methods to help DateRate to rate dates. Like the name said.
 * 
 * @author Martin Greogr
 * 
 */
public class DateRaterHelper {

    /**
     * Checks if a date is between 13th of November 1990, time 0:00 and now.
     * 
     * @param date
     * @return
     */
    public static boolean isDateInRange(ExtractedDate date) {
        ExtractedDate begin = new ExtractedDate("1990-11-13T00:00:00Z", RegExp.DATE_ISO8601_YMD_T[1]);
        ExtractedDate end = ExtractedDateHelper.getCurrentDate();
//        DateComparator comp = new DateComparator();
//        int stopFlag = Math.min(DateComparator.STOP_DAY, date.getExactness());
//        boolean gt = comp.compare(begin, date, stopFlag) > -1;
//        boolean lt = comp.compare(date, end, stopFlag) > -1;
//        return gt && lt;

        CompareDepth compareDepth = CompareDepth.DAY;
        if  (date.getExactness() != 0) {
            compareDepth = CompareDepth.min(CompareDepth.DAY, CompareDepth.byValue(date.getExactness()));
        }
        DateComparator dateComparator = new DateComparator(compareDepth);
        boolean gt = dateComparator.compare(begin, date) > -1;
        boolean lt = dateComparator.compare(date, end) > -1;
        return gt && lt;
    }

//    /**
//     * Returns the date with highest rate. <br>
//     * 
//     * @param <T>
//     * @param dates
//     * @return Hashmap with a single entry.
//     */
//    public static <T> Map<T, Double> getHighestRate(Map<T, Double> dates) {
//        HashMap<T, Double> map = new HashMap<T, Double>();
//        T date = null;
//        double temp = -1;
//        for (Entry<T, Double> e : dates.entrySet()) {
//            double value = e.getValue();
//
//            if (value > temp) {
//                date = e.getKey();
//                temp = value;
//            }
//        }
//        map.put(date, temp);
//        return map;
//    }

//    /**
//     * Returns the date with highest rate. <br>
//     * 
//     * @param <T>
//     * @param dates
//     * @return Hashmap with a single entry.
//     */
//    public static <T> Double getHighestRateValue(Map<T, Double> dates) {
//        double temp = -1;
//        for (Entry<T, Double> e : dates.entrySet()) {
//            double value = e.getValue();
//            if (value > temp) {
//                temp = value;
//            }
//        }
//        return temp;
//    }

    /**
     * Increase the rate by 10 percent, if date sourrunding tag is a headline-tag.
     * 
     * @param contentDates
     * @return
     */
    public static Map<ContentDate, Double> evaluateTag(Map<ContentDate, Double> contentDates) {
        Map<ContentDate, Double> result = contentDates;
        for (Entry<ContentDate, Double> e : contentDates.entrySet()) {
            if (HtmlHelper.isHeadlineTag(e.getKey().getTag())) {
                double newRate = (1 - e.getValue()) * 0.1 + e.getValue();
                result.put(e.getKey(), Math.round(newRate * 10000) / 10000.0);
            }
        }
        return result;
    }

    /**
     * Calculates the rate for dates.<br>
     * NewRate = CountOfSameDatesToSet / CountOfDatesToSet. <br>
     * Example: datesToSet.size()=5; 3/5 and 2/5.
     * 
     * @param <T>
     * @param datesToSet
     * @param dates
     */
    public static <T> Map<T, Double> setRateWhightedByGroups(List<T> datesToSet, List<T> dates) {
        return setRateWhightedByDates(datesToSet, dates);
        // setRateWhightedByGroups(datesToSet, dates, DateComparator.STOP_DAY);
    }

    /**
     * Calculates the rate for dates.<br>
     * NewRate = CountOfSameDatesToSet / CountOfDatesToSet. <br>
     * Example: datesToSet.size()=5; 3/5 and 2/5.
     * 
     * @param <T>
     * @param datesToSet
     * @param dates
     */
    public static <T> void setRateWhightedByGroups(List<T> datesToSet, Map<T, Double> dates, CompareDepth compareDepth) {
        List<List<T>> groupedDates = DateArrayHelper.arrangeByDate(datesToSet, compareDepth);
        for (int k = 0; k < groupedDates.size(); k++) {
            for (int i = 0; i < groupedDates.get(k).size(); i++) {
                double newRate = 1.0 * groupedDates.get(k).size() / datesToSet.size();
                dates.put(groupedDates.get(k).get(i), Math.round(newRate * 10000) / 10000.0);
            }
        }
    }

    /**
     * Calculates the rate for dates.<br>
     * NewRate = CountOfSameDatesToSet / CountOfDatesToSet. <br>
     * Example: datesToSet.size()=5; 3/5 and 2/5.
     * 
     * @param <T>
     * @param datesToSet
     * @param dates
     */
    private static <T> Map<T, Double> setRateWhightedByDates(List<T> datesToSet, List<T> dates) {
        HashMap<T, Double> resultDates = new HashMap<T, Double>();
        for (int k = 0; k < datesToSet.size(); k++) {
            int contSame = DateArrayHelper.countDates(datesToSet.get(k), dates, -1) + 1;
            double newRate = 1.0 * contSame / dates.size();

            resultDates.put(datesToSet.get(k), Math.round(newRate * 10000) / 10000.0);

        }
        return resultDates;
    }

    /**
     * Sets for all dates from arraylist the rate-value to 0.0 in map.
     * 
     * @param <T>
     * @param datesToBeSetZero
     * @param map
     */
    public static <T> void setRateToZero(List<T> datesToBeSetZero, Map<T, Double> map) {
        setRate(datesToBeSetZero, map, 0.0);

    }

    /**
     * Sets for all dates from arraylist the rate-value to given value in map.
     * 
     * @param <T>
     * @param datesToBeSetZero
     * @param map
     */
    public static <T> void setRate(List<T> datesToBeSetZero, Map<T, Double> map, double rate) {
        for (int i = 0; i < datesToBeSetZero.size(); i++) {
            map.put(datesToBeSetZero.get(i), 0.0);
        }

    }

    /**
     * Compares a date1 with a well known date2, where you are sure that this is in the right format. <br>
     * To make this sure, the format will be checked automatically. (Formats are {@link RegExp.DATE_URL_D},
     * {@link RegExp.DATE_URL_MMMM_D}, {@link RegExp.DATE_ISO8601_YMD} and {@link RegExp.DATE_ISO8601_YMD_NO}. <br>
     * If date1 and date2 have equal years and day and month are mixed up, month and day in date2 will be exchanged. <br>
     * Caution, no other parameters will be changed. So the original datestring and format will stay, and if you call
     * {@link ExtractedDate.setDateParticles} old values will be rest. <br>
     * <br>
     * Example: date1: 2010-09-07; date2: 07/09/2010, but will be identified as US-American-date to 2010-07-09. <br>
     * date2 month and day will be exchanged so you get 2010-09-07 by calling {@link ExtractedDate.getNormalizedDate}.
     * 
     * @param <T>
     * @param orginalDate
     * @param toCheckDate
     */
    public static <T> void checkDayMonthYearOrder(T orginalDate, ExtractedDate toCheckDate) {
        String[] formats = { RegExp.DATE_URL_D[1], RegExp.DATE_URL_MMMM_D[1], RegExp.DATE_ISO8601_YMD[1],
                RegExp.DATE_ISO8601_YMD_NO[1] };
        ExtractedDate orginal = (ExtractedDate) orginalDate;

        for (int i = 0; i < formats.length; i++) {
            if (orginal.getFormat().equalsIgnoreCase(formats[i])) {
                if (orginal.get(ExtractedDate.YEAR) == toCheckDate.get(ExtractedDate.YEAR)) {
                    if (orginal.get(ExtractedDate.MONTH) == toCheckDate.get(ExtractedDate.DAY)
                            && orginal.get(ExtractedDate.DAY) == toCheckDate.get(ExtractedDate.MONTH)) {
                        int help = toCheckDate.get(ExtractedDate.MONTH);
                        toCheckDate.set(ExtractedDate.MONTH, toCheckDate.get(ExtractedDate.DAY));
                        toCheckDate.set(ExtractedDate.DAY, help);
                    }
                }
                break;
            }
        }
    }

    /**
     * Returns the classpriority of a keyword. If a date has no keyword -1 will be returned.<br>
     * Otherwise returning values are equal to {@link KeyWords} static values.
     * 
     * @param date
     * @return
     */
    public static byte getKeywordPriority(ExtractedDate date) {
        byte keywordPriority = -1;
        String keyword = date.getKeyword();
        if (keyword != null) {
            keywordPriority = KeyWords.getKeywordPriority(keyword);
        }

        return keywordPriority;
    }

    /**
     * For each date-entry in the hashmap, the value will be set as rate-parameter of the date.
     * 
     * @param <T>
     * @param map
     */
    public static <T> void writeRateInDate(Map<T, Double> map) {
        for (Entry<T, Double> e : map.entrySet()) {
            ((ExtractedDate) e.getKey()).setRate(e.getValue());
        }
    }
}
