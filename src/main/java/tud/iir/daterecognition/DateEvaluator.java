package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class DateEvaluator {

    public <T> HashMap<ExtractedDate, Double> evaluate(ArrayList<T> extractedDates) {
        HashMap<ExtractedDate, Double> evaluatedDates = new HashMap<ExtractedDate, Double>();
        ArrayList<T> dates = DateEvaluatorHelper.filter(extractedDates, DateEvaluatorHelper.FILTER_IS_IN_RANGE);
        ArrayList<URLDate> urlDate = (ArrayList<URLDate>) DateEvaluatorHelper.filter(dates, ExtractedDate.TECH_URL);
        Iterator<URLDate> url = urlDate.iterator();
        while (url.hasNext()) {
            evaluatedDates.put(url.next(), 1.0);
        }
        ArrayList<ContentDate> contenDate = (ArrayList<ContentDate>) DateEvaluatorHelper.filter(dates,
                ExtractedDate.TECH_HTML_CONT);
        Iterator<ContentDate> content = contenDate.iterator();
        while (url.hasNext()) {
            evaluatedDates.put(url.next(), 1.0);
        }

        return evaluatedDates;
    }

    public HashMap<ExtractedDate, Double> evaluateContentDate(ContentDate date) {
        HashMap<ExtractedDate, Double> result = new HashMap<ExtractedDate, Double>();
        Double value = 1.0;
        double factor = 0;

        String key = date.getKeyword();

        if (key != null) {
            if (key.equalsIgnoreCase("published")) {
                factor = 1;
            }
            int keyLocation = date.get(ContentDate.KEYWORDLOCATION);
            switch (keyLocation) {
                case ContentDate.KEY_LOC_ATTR:
                    factor = factor * 1;
                    break;
                case ContentDate.KEY_LOC_CONTENT:
                    int x = date.get(ContentDate.DISTANCE_DATE_KEYWORD);
                    factor = factor * (Math.pow((-x + 40), (1 / 1.5)) / 11.69607);
                    break;
                default:
                    factor = 0;
            }

        } else {
            factor = 0;
        }
        value = value * factor;
        result.put(date, value);
        return result;
    }
}
