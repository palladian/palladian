package ws.palladian.extraction.date;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.DateFormat;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.dates.KeywordDate;

/**
 * This class contains methods to help DateRate to rate dates.
 * 
 * @author Martin Gregor
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
        //ExtractedDate begin = new ExtractedDate("1990-11-13T00:00:00Z", RegExp.DATE_ISO8601_YMD_T[1]);
        ExtractedDate begin = DateParser.parseDate("1990-11-13T00:00:00Z", RegExp.DATE_ISO8601_YMD_T.getFormat());
        ExtractedDate end = new ExtractedDate();
//        DateComparator comp = new DateComparator();
//        int stopFlag = Math.min(DateComparator.STOP_DAY, date.getExactness());
//        boolean gt = comp.compare(begin, date, stopFlag) > -1;
//        boolean lt = comp.compare(date, end, stopFlag) > -1;
//        return gt && lt;

        DateExactness compareDepth = DateExactness.DAY;
        if  (date.getExactness().getValue() != 0) {
            compareDepth = DateExactness.getCommonExactness(DateExactness.DAY, date.getExactness());
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
     * Calculates the rate for dates.<br>
     * NewRate = CountOfSameDatesToSet / CountOfDatesToSet. <br>
     * Example: datesToSet.size()=5; 3/5 and 2/5.
     * 
     * @param <T>
     * @param datesToSet
     * @param dates
     */
    public static <T extends ExtractedDate> void setRateWhightedByGroups(List<T> datesToSet, Map<T, Double> dates, DateExactness compareDepth) {
        List<List<T>> groupedDates = DateArrayHelper.cluster(datesToSet, compareDepth);
        for (int k = 0; k < groupedDates.size(); k++) {
            for (int i = 0; i < groupedDates.get(k).size(); i++) {
                double newRate = 1.0 * groupedDates.get(k).size() / datesToSet.size();
                dates.put(groupedDates.get(k).get(i), Math.round(newRate * 10000) / 10000.0);
            }
        }
    }

    /**
     * Sets for all dates from {@link List} the rate-value to 0.0 in map.
     * 
     * @param <T>
     * @param datesToBeSetZero
     * @param map
     */
    public static <T> void setRateToZero(List<T> datesToBeSetZero, Map<T, Double> map) {
        setRate(datesToBeSetZero, map, 0.0);
    }

    /**
     * Sets for all dates from {@link List} the rate-value to given value in map.
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
    public static <T extends ExtractedDate> void checkDayMonthYearOrder(T orginalDate, ExtractedDate toCheckDate) {
        DateFormat[] formats = { RegExp.DATE_URL_D, RegExp.DATE_URL_MMMM_D, RegExp.DATE_ISO8601_YMD,
                RegExp.DATE_ISO8601_YMD_NO };

        for (int i = 0; i < formats.length; i++) {
            if (orginalDate.getFormat().equalsIgnoreCase(formats[i].getFormat())) {
                if (orginalDate.get(ExtractedDate.YEAR) == toCheckDate.get(ExtractedDate.YEAR)) {
                    if (orginalDate.get(ExtractedDate.MONTH) == toCheckDate.get(ExtractedDate.DAY)
                            && orginalDate.get(ExtractedDate.DAY) == toCheckDate.get(ExtractedDate.MONTH)) {
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
    public static <T extends KeywordDate> byte getKeywordPriority(T date) {
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
    public static <T extends ExtractedDate> void writeRateInDate(Map<T, Double> map) {
        for (Entry<T, Double> e : map.entrySet()) {
            e.getKey().setRate(e.getValue());
        }
    }
}
