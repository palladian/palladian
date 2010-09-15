package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;
import tud.iir.helper.HTMLHelper;
import tud.iir.knowledge.KeyWords;
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
        int stopFlag = Math.min(DateComparator.STOP_DAY, date.getExactness());
        boolean gt = (comp.compare(begin, date, stopFlag) > -1);
        boolean lt = (comp.compare(date, end, stopFlag) > -1);
        return (gt && lt);
    }

    /**
     * Returns the date with highest rate. <br>
     * 
     * @param <T>
     * @param dates
     * @return Hashmap with a single entry.
     */
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

    /**
     * Increase the rate by 10 percent, if date sourrunding tag is a headline-tag.
     * 
     * @param contentDates
     * @return
     */
    public static HashMap<ContentDate, Double> evaluateTag(HashMap<ContentDate, Double> contentDates) {
        HashMap<ContentDate, Double> result = contentDates;
        for (Entry<ContentDate, Double> e : contentDates.entrySet()) {
            if (HTMLHelper.isHeadlineTag(e.getKey().getTag())) {
                double newRate = (1 - e.getValue()) * 0.1 + e.getValue();
                result.put(e.getKey(), Math.round(newRate * 100) / 100.0);
            }
        }
        return result;
    }

    /**
     * Calculates rate of dates with keyword within attribute.
     * 
     * @param attrDates
     * @return
     */
    public static HashMap<ContentDate, Double> evaluateKeyLocAttr(ArrayList<ContentDate> attrDates) {
        HashMap<ContentDate, Double> attrResult = new HashMap<ContentDate, Double>();
        ContentDate date;
        double rate;
        for (int i = 0; i < attrDates.size(); i++) {
            date = attrDates.get(i);
            rate = calcContDateAttr(date);
            attrResult.put(date, rate);
        }
        ArrayList<ContentDate> rate1Dates = DateArrayHelper.getRatedDates(attrResult, 1);
        ArrayList<ContentDate> middleRatedDates = DateArrayHelper.getRatedDates(attrResult, 0.7);
        ArrayList<ContentDate> lowRatedDates = DateArrayHelper.getRatedDates(attrResult, 0.5);

        if (rate1Dates.size() > 0) {
            setRateWhightedByGroups(rate1Dates, attrResult);

            setRateToZero(middleRatedDates, attrResult);
            setRateToZero(lowRatedDates, attrResult);
        } else if (middleRatedDates.size() > 0) {
            setRateWhightedByGroups(middleRatedDates, attrResult);

            setRateToZero(lowRatedDates, attrResult);
        } else {
            setRateWhightedByGroups(lowRatedDates, attrResult);
        }
        return attrResult;

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
    public static <T> void setRateWhightedByGroups(ArrayList<T> datesToSet, HashMap<T, Double> dates) {
        setRateWhightedByGroups(datesToSet, dates, DateComparator.STOP_DAY);
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
    public static <T> void setRateWhightedByGroups(ArrayList<T> datesToSet, HashMap<T, Double> dates, int stopFlag) {
        ArrayList<ArrayList<T>> groupedDates = DateArrayHelper.arrangeByDate(datesToSet, stopFlag);
        for (int k = 0; k < groupedDates.size(); k++) {
            for (int i = 0; i < groupedDates.get(k).size(); i++) {
                double newRate = (1.0 * groupedDates.get(k).size()) / datesToSet.size();
                dates.put(groupedDates.get(k).get(i), Math.round(newRate * 100) / 100.0);
            }
        }
    }

    /**
     * Sets for all dates from arraylist the rate-value to 0.0 in map.
     * 
     * @param <T>
     * @param datesToBeSetZero
     * @param map
     */
    public static <T> void setRateToZero(ArrayList<T> datesToBeSetZero, HashMap<T, Double> map) {
        setRat(datesToBeSetZero, map, 0.0);

    }

    /**
     * Sets for all dates from arraylist the rate-value to given value in map.
     * 
     * @param <T>
     * @param datesToBeSetZero
     * @param map
     */
    public static <T> void setRat(ArrayList<T> datesToBeSetZero, HashMap<T, Double> map, double rate) {
        for (int i = 0; i < datesToBeSetZero.size(); i++) {
            map.put(datesToBeSetZero.get(i), 0.0);
        }

    }

    /**
     * Calculates the rate of dates with keywords within content.
     * 
     * @param contDates
     * @return
     */
    public static HashMap<ContentDate, Double> evaluateKeyLocCont(ArrayList<ContentDate> contDates) {
        HashMap<ContentDate, Double> contResult = new HashMap<ContentDate, Double>();
        double factor_keyword;
        double factor_content;
        for (int i = 0; i < contDates.size(); i++) {
            ContentDate date = contDates.get(i);
            factor_content = calcContDateContent(date);
            contResult.put(date, factor_content);
        }
        ArrayList<ContentDate> rate1dates = DateArrayHelper.getRatedDates(contResult, 1.0);
        ArrayList<ArrayList<ContentDate>> rate1groupe = DateArrayHelper.arrangeByDate(rate1dates);
        ContentDate key;

        for (int k = 0; k < rate1groupe.size(); k++) {
            for (int i = 0; i < rate1groupe.get(k).size(); i++) {
                // anz der dates mit gleichen werten / anz aller dates
                key = rate1groupe.get(k).get(i);
                factor_keyword = calcContDateAttr(key);
                double newRate = (1.0 * rate1groupe.get(k).size()) / rate1dates.size();
                contResult.put(key, Math.round(newRate * factor_keyword * 100) / 100.0);
            }
        }
        ArrayList<ContentDate> rateRestDates = DateArrayHelper.getRatedDates(contResult, 1.0, false);
        ArrayList<ArrayList<ContentDate>> rateRestGroupe = DateArrayHelper.arrangeByDate(rateRestDates);
        for (int k = 0; k < rateRestGroupe.size(); k++) {
            for (int i = 0; i < rateRestGroupe.get(k).size(); i++) {
                key = rateRestGroupe.get(k).get(i);
                factor_keyword = calcContDateAttr(key);
                double newRate = (1.0 * contResult.get(key) * rateRestGroupe.get(k).size()) / contDates.size();
                contResult.put(key, Math.round(newRate * factor_keyword * 100) / 100.0);
            }
        }
        return contResult;
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
        if (hasKeyword(keyword, KeyWords.firstPriorityKeywords)) {
            keywordPriority = KeyWords.FIRST_PRIORITY;
        } else if (hasKeyword(keyword, KeyWords.secondPriorityKeywords)) {
            keywordPriority = KeyWords.SECOND_PRIORITY;
        } else if (hasKeyword(keyword, KeyWords.thirdPriorityKexwords)) {
            keywordPriority = KeyWords.THIRD_PRIORITY;
        }
        return keywordPriority;
    }

    private static boolean hasKeyword(String keyword, String[] keywords) {
        boolean hasKeyword = false;
        for (int i = 0; i < keywords.length; i++) {
            if (keyword.equalsIgnoreCase(keywords[i])) {
                hasKeyword = true;
                break;
            }
        }
        return hasKeyword;
    }

    /**
     * Sets the factor for rate-calculation of dates with keywords within attributes.
     * 
     * @param date
     * @return
     */
    public static double calcContDateAttr(ContentDate date) {
        String key = date.getKeyword();
        double factor = 0;
        byte keywordPriority = DateEvaluatorHelper.getKeywordPriority(date);
        if (key != null) {
            if (keywordPriority == KeyWords.FIRST_PRIORITY) {
                factor = 1;
            } else if (keywordPriority == KeyWords.SECOND_PRIORITY) {
                factor = 0.7;
            } else {
                factor = 0.5;
            }
        }
        return factor;
    }

    /**
     * Sets the factor for rate-calculation of dates with keywords within content.
     * 
     * @param date
     * @return
     */
    public static double calcContDateContent(ContentDate date) {
        int distance = date.get(ContentDate.DISTANCE_DATE_KEYWORD);
        // factor = factor * Math.round((Math.pow((-x + 40), (1 / 1.5)) / 11.69607) * 100) / 100;
        // Stufen= 4 Leerzeichen ^= 3 Wörtern
        double factor = 0;
        if (distance < 0) {
            factor = 0;
        } else if (distance < 5) {
            factor = 1;
        } else if (distance < 8) {
            factor = 0.82;
        } else if (distance < 11) {
            factor = 0.64;
        } else if (distance < 14) {
            factor = 0.47;
        } else if (distance < 17) {
            factor = 0.29;
        } else if (distance < 19) {
            factor = 0.12;
        } else {
            factor = 0.0;
        }

        return factor;

    }
}
