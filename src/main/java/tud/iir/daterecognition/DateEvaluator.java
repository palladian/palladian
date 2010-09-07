package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HTTPDate;
import tud.iir.daterecognition.dates.HeadDate;
import tud.iir.daterecognition.dates.ReferenceDate;
import tud.iir.daterecognition.dates.StructureDate;
import tud.iir.daterecognition.dates.URLDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;
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
        if (structDates != null && structDates.size() > 0) {
            structResult.putAll((Map<? extends T, ? extends Double>) evaluateStructDate(structDates));
        }
        if (contDates != null && contDates.size() > 0) {
            contResult.putAll((Map<? extends T, ? extends Double>) evaluateContentDate(contDates));
        }

        evaluatedDates.putAll(urlResult);
        evaluatedDates.putAll(httpResult);
        evaluatedDates.putAll(headResult);
        evaluatedDates.putAll(structResult);
        evaluatedDates.putAll(contResult);

        /*
         * ArrayList<ArrayList<T>> dateGroups = DateArrayHelper.arrangeByDate(dates);
         * for (int i = 0; i < dateGroups.size(); i++) {
         * HashMap<T, Double> temp = evaluateGroup(dateGroups.get(i));
         * evaluatedDates.putAll(temp);
         * }
         */
        /*
         * HashMap<ContentDate, Double> allContentDates = (HashMap<ContentDate, Double>) DateArrayHelper.filter(
         * evaluatedDates, DateArrayHelper.FILTER_TECH_HTML_CONT);
         * evaluateContentDateByNumber(allContentDates);
         * evaluatedDates.putAll((Map<? extends T, ? extends Double>) allContentDates);
         */
        return evaluatedDates;
    }

    @SuppressWarnings("unchecked")
    public <T> HashMap<T, Double> evaluateGroup(ArrayList<T> array) {
        HashMap<T, Double> result = new HashMap<T, Double>();
        HashMap<T, Double> urlResult = new HashMap<T, Double>();
        HashMap<T, Double> httpResult = new HashMap<T, Double>();
        HashMap<T, Double> headResult = new HashMap<T, Double>();
        HashMap<T, Double> structResult = new HashMap<T, Double>();
        HashMap<T, Double> contResult = new HashMap<T, Double>();

        ArrayList<URLDate> urlDates = (ArrayList<URLDate>) DateArrayHelper.filter(array, ExtractedDate.TECH_URL);
        ArrayList<HTTPDate> httpDates = (ArrayList<HTTPDate>) DateArrayHelper.filter(array,
                ExtractedDate.TECH_HTTP_HEADER);
        ArrayList<HeadDate> headDates = (ArrayList<HeadDate>) DateArrayHelper.filter(array,
                ExtractedDate.TECH_HTML_HEAD);
        ArrayList<StructureDate> structDates = (ArrayList<StructureDate>) DateArrayHelper.filter(array,
                ExtractedDate.TECH_HTML_STRUC);
        ArrayList<ContentDate> contDates = (ArrayList<ContentDate>) DateArrayHelper.filter(array,
                ExtractedDate.TECH_HTML_CONT);
        ArrayList<ReferenceDate> refDates = (ArrayList<ReferenceDate>) DateArrayHelper.filter(array,
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
        if (structDates != null && structDates.size() > 0) {
            structResult.putAll((Map<? extends T, ? extends Double>) evaluateStructDate(structDates));
        }
        if (contDates != null && contDates.size() > 0) {
            contResult.putAll((Map<? extends T, ? extends Double>) evaluateContentDate(contDates));
        }

        result.putAll(urlResult);
        result.putAll(httpResult);
        result.putAll(headResult);
        result.putAll(structResult);
        result.putAll(contResult);
        return result;
    }

    public HashMap<StructureDate, Double> evaluateStructDate(ArrayList<StructureDate> structDates) {
        HashMap<StructureDate, Double> result = new HashMap<StructureDate, Double>();
        double rate;
        for (int i = 0; i < structDates.size(); i++) {
            StructureDate date = structDates.get(i);
            String keyword = date.getKeyword();
            if (keyword.equalsIgnoreCase("published") || keyword.equalsIgnoreCase("pubdate")
                    || keyword.equalsIgnoreCase("posted") || keyword.equalsIgnoreCase("released")
                    || keyword.equalsIgnoreCase("pdate")) {
                rate = 1;
            } else if (keyword.equalsIgnoreCase("update") || keyword.equalsIgnoreCase("changed")
                    || keyword.equalsIgnoreCase("modified")) {
                rate = 0.7; // TODO: rate bestimmen.
            } else {
                rate = 0;
            }
            result.put(date, rate);
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

        // Run through dates without keyword.
        for (int i = 0; i < nokeywordDates.size(); i++) {
            ContentDate date = nokeywordDates.get(i);
            nokeywordResult.put(date, 0.0);
        }

        result.putAll(attrResult);
        result.putAll(contResult);
        result.putAll(nokeywordResult);

        return result;
    }
}
