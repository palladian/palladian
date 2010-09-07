package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.helper.DateArrayHelper;
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
        boolean gt = (comp.compare(begin, date, DateComparator.STOP_DAY) > -1);
        boolean lt = (comp.compare(date, end, DateComparator.STOP_DAY) > -1);
        return (gt && lt);
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

    public static double calcContDateAttr(ContentDate date) {
        String key = date.getKeyword();
        double factor = 0;
        if (key != null) {
            if (key.equalsIgnoreCase("published") || key.equalsIgnoreCase("posted") || key.equalsIgnoreCase("pubdate")
                    || key.equalsIgnoreCase("released") || key.equalsIgnoreCase("pdate")) {
                factor = 1;
            } else {
                factor = -1;
            }
        }
        return factor;
    }

    public static double calcContDateContent(ContentDate date) {
        int distance = date.get(ContentDate.DISTANCE_DATE_KEYWORD);
        // factor = factor * Math.round((Math.pow((-x + 40), (1 / 1.5)) / 11.69607) * 100) / 100;
        double factor = 0;
        if (distance < 0) {
            factor = 0;
        } else if (distance < 7) {
            factor = 1;
        } else if (distance < 16) {
            factor = 0.6;
        } else if (distance < 30) {
            factor = 0.4;
        } else {
            factor = 0;
        }

        return factor;

    }

    public static HashMap<ContentDate, Double> evaluateKeyLocAttr(ArrayList<ContentDate> attrDates) {
        HashMap<ContentDate, Double> attrResult = new HashMap<ContentDate, Double>();
        boolean attrRateIs1 = false;
        for (int i = 0; i < attrDates.size(); i++) {
            double rate;
            ContentDate date = attrDates.get(i);
            rate = calcContDateAttr(date);
            if (rate == 1) {
                attrRateIs1 = true;
            }
            attrResult.put(date, rate);
        }
        ArrayList<ContentDate> negRatedDates = DateArrayHelper.getRatedDates(attrResult, -1);

        if (attrRateIs1) {
            for (int i = 0; i < negRatedDates.size(); i++) {
                attrResult.put(negRatedDates.get(i), 0.0);
            }

            ArrayList<ContentDate> rate1Dates = DateArrayHelper.getRatedDates(attrResult, 1);
            ArrayList<ArrayList<ContentDate>> groupedDates = DateArrayHelper.arrangeByDate(rate1Dates);
            for (int k = 0; k < groupedDates.size(); k++) {
                for (int i = 0; i < groupedDates.get(k).size(); i++) {
                    attrResult.put(groupedDates.get(k).get(i), (1.0 * groupedDates.get(k).size()) / rate1Dates.size());
                }
            }

        } else {
            ArrayList<ArrayList<ContentDate>> groupedDates = DateArrayHelper.arrangeByDate(negRatedDates);
            for (int k = 0; k < groupedDates.size(); k++) {
                for (int i = 0; i < groupedDates.get(k).size(); i++) {
                    attrResult.put(groupedDates.get(k).get(i), (1.0 * groupedDates.get(k).size())
                            / negRatedDates.size());
                }
            }
        }
        return attrResult;

    }

    public static HashMap<ContentDate, Double> evaluateKeyLocCont(ArrayList<ContentDate> contDates) {
        HashMap<ContentDate, Double> contResult = new HashMap<ContentDate, Double>();
        for (int i = 0; i < contDates.size(); i++) {
            double rate;
            ContentDate date = contDates.get(i);
            rate = calcContDateAttr(date);
            contResult.put(date, rate);
        }
        ArrayList<ContentDate> rate1dates = DateArrayHelper.getRatedDates(contResult, 1.0);
        ArrayList<ArrayList<ContentDate>> rate1groupe = DateArrayHelper.arrangeByDate(rate1dates);
        for (int k = 0; k < rate1groupe.size(); k++) {
            for (int i = 0; i < rate1groupe.get(k).size(); i++) {
                // anz der dates mit gleichen werten / anz aller dates
                contResult.put(rate1groupe.get(k).get(i), (1.0 * rate1groupe.get(k).size()) / rate1dates.size());
            }
        }
        ArrayList<ContentDate> rateRestDates = DateArrayHelper.getRatedDates(contResult, 1.0, false);
        ArrayList<ArrayList<ContentDate>> rateRestGroupe = DateArrayHelper.arrangeByDate(rateRestDates);
        for (int k = 0; k < rateRestGroupe.size(); k++) {
            for (int i = 0; i < rateRestGroupe.get(k).size(); i++) {
                ContentDate key = rateRestGroupe.get(k).get(i);
                contResult.put(key, (contResult.get(key) * rateRestGroupe.get(k).size()) / contDates.size());
            }
        }

        return contResult;

    }
}
