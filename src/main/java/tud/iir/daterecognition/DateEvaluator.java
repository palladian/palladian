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
import tud.iir.knowledge.RegExp;

public class DateEvaluator {

    @SuppressWarnings("unchecked")
    public <T> HashMap<T, Double> evaluate(ArrayList<T> extractedDates) {
        HashMap<T, Double> evaluatedDates = new HashMap<T, Double>();
        ArrayList<T> dates = DateArrayHelper.filter(extractedDates, DateArrayHelper.FILTER_IS_IN_RANGE);
        ArrayList<ArrayList<T>> dateGroups = DateArrayHelper.arrangeByDate(dates);

        for (int i = 0; i < dateGroups.size(); i++) {
            HashMap<T, Double> temp = evaluateGroup(dateGroups.get(i));
            evaluatedDates.putAll(temp);
        }

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

        int groupFactor = 0;
        int urlFactor = 0;
        int httpFactor = 0;
        int headFactor = 0;
        int contentFactor = 0;
        int structureFactor = 0;
        if (urlResult.size() > 0) {

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
        double rate = 0;
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
            }
            result.put(date, rate);
        }
        return result;
    }

    public HashMap<HeadDate, Double> evaluateHeadDate(ArrayList<HeadDate> headDates) {
        HashMap<HeadDate, Double> result = new HashMap<HeadDate, Double>();
        double rate = 0;
        for (int i = 0; i < headDates.size(); i++) {
            HeadDate date = headDates.get(i);
            String keyword = date.getKeyword();
            if (keyword.equalsIgnoreCase("published") || keyword.equalsIgnoreCase("pubdate")
                    || keyword.equalsIgnoreCase("posted") || keyword.equalsIgnoreCase("released")
                    || keyword.equalsIgnoreCase("pdate")) {
                rate = 1;
            } else if (keyword.equalsIgnoreCase("update") || keyword.equalsIgnoreCase("changed")
                    || keyword.equalsIgnoreCase("modified")) {
                rate = 0.7; // TODO: rate bestimmen.
            }
            result.put(date, rate);
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

        for (int i = 0; i < dates.size(); i++) {
            ContentDate date = dates.get(i);
            Double value = 1.0;
            double factor = 0;

            String key = date.getKeyword();

            if (key != null) {
                if (key.equalsIgnoreCase("published") || key.equalsIgnoreCase("posted")
                        || key.equalsIgnoreCase("pubdate") || key.equalsIgnoreCase("released")
                        || key.equalsIgnoreCase("pdate")) {
                    factor = 1;
                } else {
                    factor = -1;
                }
                int keyLocation = date.get(ContentDate.KEYWORDLOCATION);
                switch (keyLocation) {
                    case ContentDate.KEY_LOC_ATTR:
                        factor = factor * 1;
                        break;
                    case ContentDate.KEY_LOC_CONTENT:
                        int distance = date.get(ContentDate.DISTANCE_DATE_KEYWORD);
                        // factor = factor * Math.round((Math.pow((-x + 40), (1 / 1.5)) / 11.69607) * 100) / 100;
                        double distanceFactor;
                        if (distance < 0) {
                            distanceFactor = 0;
                        } else if (distance < 7) {
                            distanceFactor = 1;
                        } else if (distance < 16) {
                            distanceFactor = 0.6;
                        } else if (distance < 30) {
                            distanceFactor = 0.4;
                        } else {
                            distanceFactor = 0;
                        }
                        factor = factor * distanceFactor;
                        break;
                    default:
                        factor = 0;
                }

            } else {
                factor = 0;
            }
            value = value * factor;
            result.put(date, value);
        }
        return result;
    }

    public void evaluateContentDateByNumber(HashMap<ContentDate, Double> dates) {
        ArrayList<ContentDate> temp = new ArrayList<ContentDate>();
        // number of contentDates
        double rateOthers = 0;
        for (Entry<ContentDate, Double> e : dates.entrySet()) {
            Double rate = e.getValue();
            if (rate == -1) {
                temp.add(e.getKey());
            }
        }
        if (temp != null) {
            double numberDates = temp.size();
            for (int i = 0; i < numberDates; i++) {
                dates.put(temp.get(i), (1 - rateOthers) / numberDates);
            }
        }
    }
}
