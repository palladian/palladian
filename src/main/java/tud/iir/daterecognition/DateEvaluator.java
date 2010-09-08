package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HTTPDate;
import tud.iir.daterecognition.dates.HeadDate;
import tud.iir.daterecognition.dates.ReferenceDate;
import tud.iir.daterecognition.dates.StructureDate;
import tud.iir.daterecognition.dates.URLDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;
import tud.iir.knowledge.KeyWords;
import tud.iir.knowledge.RegExp;

public class DateEvaluator {

    @SuppressWarnings("unchecked")
    public <T> HashMap<T, Double> evaluate(ArrayList<T> extractedDates) {
        HashMap<T, Double> evaluatedDates = new HashMap<T, Double>();

        ArrayList<T> dates = DateArrayHelper.filter(extractedDates, DateArrayHelper.FILTER_IS_IN_RANGE);
        HashMap<T, Double> urlResult = new HashMap<T, Double>();
        HashMap<T, Double> httpResult = new HashMap<T, Double>();
        HashMap<T, Double> headResult = new HashMap<T, Double>();
        HashMap<T, Double> structResult = new HashMap<T, Double>();
        HashMap<T, Double> contResult = new HashMap<T, Double>();

        ArrayList<URLDate> urlDates = (ArrayList<URLDate>) DateArrayHelper.filter(dates, ExtractedDate.TECH_URL);

        ArrayList<HTTPDate> httpDates = (ArrayList<HTTPDate>) DateArrayHelper.filter(dates,
                ExtractedDate.TECH_HTTP_HEADER);

        ArrayList<HeadDate> headDates = (ArrayList<HeadDate>) DateArrayHelper.filter(dates,
                ExtractedDate.TECH_HTML_HEAD);

        ArrayList<StructureDate> structDates = (ArrayList<StructureDate>) DateArrayHelper.filter(dates,
                ExtractedDate.TECH_HTML_STRUC);

        ArrayList<ContentDate> contDates = (ArrayList<ContentDate>) DateArrayHelper.filter(dates,
                ExtractedDate.TECH_HTML_CONT);
        ArrayList<ContentDate> contFullDates = (ArrayList<ContentDate>) DateArrayHelper.filter(contDates,
                DateArrayHelper.FILTER_FULL_DATE);

        ArrayList<ReferenceDate> refDates = (ArrayList<ReferenceDate>) DateArrayHelper.filter(dates,
                ExtractedDate.TECH_REFERENCE);

        if (urlDates != null && urlDates.size() > 0) {
            urlResult.putAll((Map<? extends T, ? extends Double>) evaluateURLDate(urlDates));
        }
        if (httpDates != null && httpDates.size() > 0) {
            httpResult.putAll((Map<? extends T, ? extends Double>) evaluateHTTPDate(httpDates));
        }
        if (headDates != null && headDates.size() > 0) {
            headResult.putAll((Map<? extends T, ? extends Double>) evaluateHeadDate(headDates));
        }

        if (contFullDates != null && contFullDates.size() > 0) {
            contResult.putAll((Map<? extends T, ? extends Double>) evaluateContentDate(contFullDates));
        } else if (contDates != null && contDates.size() > 0) {
            contResult.putAll((Map<? extends T, ? extends Double>) evaluateContentDate(contDates));
        }
        if (structDates != null && structDates.size() > 0) {
            structResult.putAll((Map<? extends T, ? extends Double>) evaluateStructDate(structDates));
            if (contResult != null && contResult.size() > 0) {
                contResult.putAll(reRateContentDates(contResult, (HashMap<StructureDate, Double>) structResult));
            }
        }

        evaluatedDates.putAll(urlResult);
        evaluatedDates.putAll(httpResult);
        evaluatedDates.putAll(headResult);
        evaluatedDates.putAll(structResult);
        evaluatedDates.putAll(contResult);
        if (DateArrayHelper.isAllZero(evaluatedDates)) {
            evaluatedDates.putAll(reRateIfAllZero(contResult));
        }

        return evaluatedDates;
    }

    public static <T> HashMap<T, Double> reRateIfAllZero(HashMap<T, Double> dates) {
        HashMap<T, Double> result = dates;

        HashMap<T, Double> exactestDates = DateArrayHelper.getExactestMap(dates);

        DateComparator dc = new DateComparator();
        T date = dc.getOldestDate(exactestDates);
        HashMap<T, Double> sameDates = DateArrayHelper.getSameDatesMap((ExtractedDate) date, result);
        for (Entry<T, Double> e : sameDates.entrySet()) {
            double newRate = (1.0 * sameDates.size()) / result.size();
            result.put(e.getKey(), Math.round(newRate * 100) / 100.0);
        }

        date = dc.getOldestDate(result);
        sameDates = DateArrayHelper.getSameDatesMap((ExtractedDate) date, result);
        for (Entry<T, Double> e : sameDates.entrySet()) {
            double newRate = (((1.0 - e.getValue()) * sameDates.size()) / result.size()) + e.getValue();
            result.put(e.getKey(), Math.round(newRate * 100) / 100.0);
        }
        return result;

    }

    public <C> HashMap<C, Double> reRateContentDates(HashMap<C, Double> contentDates,
            HashMap<StructureDate, Double> structDates) {
        DateComparator dc = new DateComparator();
        ArrayList<StructureDate> structureDates = dc.orderDates(structDates, true);
        HashMap<C, Double> result = contentDates;
        HashMap<C, Double> temp = contentDates;
        HashMap<C, Double> tempContentDates = new HashMap<C, Double>();
        HashMap<C, Double> tempResult = new HashMap<C, Double>();
        for (int i = 0; i < structureDates.size(); i++) {
            tempContentDates = DateArrayHelper.getSameDatesMap((ExtractedDate) structureDates.get(i), temp,
                    DateComparator.STOP_MINUTE);
            if (tempContentDates.size() == 0) {
                tempContentDates = DateArrayHelper.getSameDatesMap((ExtractedDate) structureDates.get(i), temp,
                        DateComparator.STOP_HOUR);
            }
            if (tempContentDates.size() == 0) {
                tempContentDates = DateArrayHelper.getSameDatesMap((ExtractedDate) structureDates.get(i), temp,
                        DateComparator.STOP_DAY);
            }
            for (Entry<C, Double> cDate : tempContentDates.entrySet()) {
                String cDateTag = ((ContentDate) cDate.getKey()).getTag();
                String eTag = ((StructureDate) structureDates.get(i)).getTag();
                if (cDateTag.equalsIgnoreCase(eTag)) {
                    double tempRate = ((1 - cDate.getValue()) / structureDates.size()) + cDate.getValue();
                    double newRate = (1 - tempRate) * structDates.get(structureDates.get(i)) + tempRate;
                    tempResult.put(cDate.getKey(), Math.round(newRate * 100) / 100.0);
                    temp.remove(cDate.getKey());
                }
            }

        }
        result.putAll(tempResult);
        return result;
    }

    public HashMap<StructureDate, Double> evaluateStructDate(ArrayList<StructureDate> structDates) {
        HashMap<StructureDate, Double> result = new HashMap<StructureDate, Double>();
        double rate;
        boolean has1rate = false;
        for (int i = 0; i < structDates.size(); i++) {
            StructureDate date = structDates.get(i);
            String keyword = date.getKeyword();
            if (keyword.equalsIgnoreCase("published") || keyword.equalsIgnoreCase("pubdate")
                    || keyword.equalsIgnoreCase("posted") || keyword.equalsIgnoreCase("released")
                    || keyword.equalsIgnoreCase("pdate")) {
                rate = 1;
                has1rate = true;
            } else if (keyword.equalsIgnoreCase("update") || keyword.equalsIgnoreCase("changed")
                    || keyword.equalsIgnoreCase("modified")) {
                rate = -1; // TODO: rate bestimmen.
            } else {
                rate = 0;
            }
            result.put(date, rate);
        }

        ArrayList<StructureDate> rateNegDates = DateArrayHelper.getRatedDates(result, -1);
        if (has1rate) {
            ArrayList<StructureDate> rate1dates = DateArrayHelper.getRatedDates(result, 1);
            ArrayList<ArrayList<StructureDate>> rate1Groupe = DateArrayHelper.arrangeByDate(rate1dates,
                    DateComparator.STOP_MINUTE);
            for (int i = 0; i < rate1Groupe.size(); i++) {
                for (int k = 0; k < rate1Groupe.get(i).size(); k++) {
                    double newRate = (1.0 * rate1Groupe.get(i).size()) / rate1dates.size();
                    result.put(rate1Groupe.get(i).get(k), Math.round(newRate * 100) / 100.0);
                }
            }
            for (int i = 0; i < rateNegDates.size(); i++) {
                result.put(rateNegDates.get(i), 0.0);
            }
        } else {
            ArrayList<ArrayList<StructureDate>> rateNegGroupe = DateArrayHelper.arrangeByDate(rateNegDates,
                    DateComparator.STOP_MINUTE);
            for (int i = 0; i < rateNegGroupe.size(); i++) {
                for (int k = 0; k < rateNegGroupe.get(i).size(); k++) {
                    double newRate = (1.0 * rateNegGroupe.get(i).size()) / rateNegDates.size();
                    result.put(rateNegGroupe.get(i).get(k), Math.round(newRate * 100) / 100.0);
                }
            }
        }

        return result;
    }

    // zuerst high prio keywors bewerten - darumter das ältestte datum wählen rest abwerten
    // mittlere prio nur bewerten, wenn keine high prio -älteste datum auf 1, rest abwerten
    // rest daten nur wenn andere nicht vorhanden - bewertung 1/anz.
    public HashMap<HeadDate, Double> evaluateHeadDate(ArrayList<HeadDate> headDates) {
        HashMap<HeadDate, Double> result = new HashMap<HeadDate, Double>();
        double rate;
        boolean hasHighRate = false;
        boolean hasMiddleRate = false;
        for (int i = 0; i < headDates.size(); i++) {
            HeadDate date = headDates.get(i);
            String keyword = date.getKeyword();
            if (keyword.equalsIgnoreCase("published") || keyword.equalsIgnoreCase("pubdate")
                    || keyword.equalsIgnoreCase("posted") || keyword.equalsIgnoreCase("released")
                    || keyword.equalsIgnoreCase("pdate")) {
                rate = 1;
                hasHighRate = true;
            } else if (keyword.equalsIgnoreCase("update") || keyword.equalsIgnoreCase("changed")
                    || keyword.equalsIgnoreCase("modified") || keyword.equalsIgnoreCase("last-modified")) {
                rate = -1; // TODO: rate bestimmen.
                hasMiddleRate = true;
            } else {
                rate = -2;
            }
            result.put(date, rate);
        }
        if (hasHighRate) {
            ArrayList<HeadDate> otherDates = DateArrayHelper.getRatedDates(result, 1, false);
            for (int i = 0; i < otherDates.size(); i++) {
                result.put(otherDates.get(i), 0.0);
            }

        } else if (hasMiddleRate) {
            ArrayList<HeadDate> ratenNegDates = DateArrayHelper.getRatedDates(result, -1, true);
            for (int i = 0; i < ratenNegDates.size(); i++) {
                result.put(ratenNegDates.get(i), 1.0);
            }
            ArrayList<HeadDate> otherDates = DateArrayHelper.getRatedDates(result, 1, false);
            for (int i = 0; i < otherDates.size(); i++) {
                result.put(otherDates.get(i), 0.0);
            }

        } else {
            ArrayList<HeadDate> ratenNegDates = DateArrayHelper.getRatedDates(result, -2, true);
            for (int i = 0; i < ratenNegDates.size(); i++) {
                result.put(ratenNegDates.get(i), 1.0);
            }
        }

        ArrayList<HeadDate> rate1dates = DateArrayHelper.getRatedDates(result, 1);
        DateComparator dc = new DateComparator();
        rate1dates = dc.orderDates(rate1dates);

        HeadDate oldest = rate1dates.get(0);
        for (int i = 1; i < rate1dates.size(); i++) {
            double diff = dc.getDifference(oldest, rate1dates.get(i), DateComparator.MEASURE_HOUR);
            if (diff > 24) {
                diff = 24;
            }
            double newRate = Math.round((1.0 - (diff / 24.0)) * 100) / 100.0;
            result.put(rate1dates.get(i), newRate);
        }

        return result;
    }

    public HashMap<HTTPDate, Double> evaluateHTTPDate(ArrayList<HTTPDate> httpDates) {
        HTTPDate date = httpDates.get(0);
        HashMap<HTTPDate, Double> result = new HashMap<HTTPDate, Double>();
        double rate = 0;
        if (date != null) {

            ExtractedDate current = ExtractedDateHelper.createActualDate();
            DateComparator dc = new DateComparator();
            double timedifference = dc.getDifference(httpDates.get(0), current, DateComparator.MEASURE_HOUR);

            if (timedifference > 3) {
                rate = 0.7;// 70% aller Webseiten haben richtigen last modified tag, aber bei dif. von 3h ist dies zu
                // nah an
                // expiere
            } else {
                rate = 0;
            }
        }

        result.put(date, rate);
        return result;
    }

    public HashMap<URLDate, Double> evaluateURLDate(ArrayList<URLDate> dates) {
        HashMap<URLDate, Double> evaluate = new HashMap<URLDate, Double>();
        for (int i = 0; i < dates.size(); i++) {
            double rate = 0;
            URLDate date = dates.get(i);
            if (date != null) {
                String format = date.getFormat();
                if (format != null) {
                    if (format.equalsIgnoreCase(RegExp.DATE_URL_D[1])) {
                        rate = 1;
                    } else if (format.equalsIgnoreCase(RegExp.DATE_URL_SPLIT[1])) {
                        rate = 1;
                    } else if (format.equalsIgnoreCase(RegExp.DATE_URL[1])) {
                        rate = 1;
                    } else if (format.equalsIgnoreCase(RegExp.DATE_URL_MMMM_D[1])) {
                        rate = 1;
                    } else {
                        rate = -1; // TODO: rate genau bestimmen.
                    }
                }
            }
            evaluate.put(date, rate);
        }
        return evaluate;
    }

    public HashMap<ExtractedDate, Double> evaluateContentDate(ArrayList<ContentDate> dates) {
        HashMap<ExtractedDate, Double> result = new HashMap<ExtractedDate, Double>();

        ArrayList<ContentDate> attrDates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_KEYLOC_ATTR);
        ArrayList<ContentDate> contDates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_KEYLOC_CONT);
        ArrayList<ContentDate> nokeywordDates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_KEYLOC_NO);

        HashMap<ContentDate, Double> attrResult = DateEvaluatorHelper.evaluateKeyLocAttr(attrDates);
        HashMap<ContentDate, Double> contResult = DateEvaluatorHelper.evaluateKeyLocCont(contDates);
        HashMap<ContentDate, Double> nokeywordResult = new HashMap<ContentDate, Double>();
        int differentDates = (DateArrayHelper.arrangeByDate(dates)).size();
        // Run through dates without keyword.
        for (int i = 0; i < nokeywordDates.size(); i++) {
            ContentDate date = nokeywordDates.get(i);
            String tag = date.getTag();
            String[] keys = KeyWords.DATE_BODY_STRUC;
            double newRate = 0;
            for (int j = 0; j < keys.length; j++) {
                if (tag.equalsIgnoreCase(keys[j])) {
                    newRate = 1.0 / differentDates;
                    break;
                }
            }
            nokeywordResult.put(date, Math.round(newRate * 100) / 100.0);
        }

        attrResult = DateEvaluatorHelper.evaluateTag(attrResult);
        contResult = DateEvaluatorHelper.evaluateTag(contResult);
        nokeywordResult = DateEvaluatorHelper.evaluateTag(nokeywordResult);

        result.putAll(attrResult);
        result.putAll(contResult);
        result.putAll(nokeywordResult);

        return result;
    }
}
