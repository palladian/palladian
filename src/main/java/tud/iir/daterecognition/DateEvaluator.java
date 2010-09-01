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
import tud.iir.knowledge.RegExp;

public class DateEvaluator {

    public <T> HashMap<T, Double> evaluate(ArrayList<T> extractedDates) {
        HashMap<T, Double> evaluatedDates = new HashMap<T, Double>();
        ArrayList<T> dates = DateArrayHelper.filter(extractedDates, DateArrayHelper.FILTER_IS_IN_RANGE);
        ArrayList<ArrayList<T>> dateGroups = DateArrayHelper.arrangeByDate(dates);
        for (int i = 0; i < dateGroups.size(); i++) {
            HashMap<T, Double> temp = evaluateGroup(dateGroups.get(i));
            evaluatedDates.putAll(temp);
        }
        return evaluatedDates;
    }

    @SuppressWarnings("unchecked")
    public <T> HashMap<T, Double> evaluateGroup(ArrayList<T> array) {
        HashMap<T, Double> result = new HashMap<T, Double>();

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

        if (urlDates != null) {
            result.putAll((Map<? extends T, ? extends Double>) evaluateURLDate(urlDates));
        }
        if (contDates != null) {
            result.putAll((Map<? extends T, ? extends Double>) evaluateContentDate(contDates));
        }

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
                if (key.equalsIgnoreCase("published")) {
                    factor = 1;
                } else if (key.equalsIgnoreCase("posted")) {
                    factor = 1;
                } else if (key.equalsIgnoreCase("pubdate")) {
                    factor = 1;
                }
                int keyLocation = date.get(ContentDate.KEYWORDLOCATION);
                switch (keyLocation) {
                    case ContentDate.KEY_LOC_ATTR:
                        factor = factor * 1;
                        break;
                    case ContentDate.KEY_LOC_CONTENT:
                        int x = date.get(ContentDate.DISTANCE_DATE_KEYWORD);
                        factor = factor * Math.round((Math.pow((-x + 40), (1 / 1.5)) / 11.69607) * 100) / 100;
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
}
