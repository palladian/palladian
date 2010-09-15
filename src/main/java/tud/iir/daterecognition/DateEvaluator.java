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

    private String url;
    private boolean referneceLookUp = false;

    public DateEvaluator() {

    }

    public DateEvaluator(String url) {
        this.url = url;
    }

    public DateEvaluator(String url, boolean referenceLookUp) {
        this.url = url;
        this.referneceLookUp = referenceLookUp;
    }

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
        if (urlResult.size() > 0 && contResult.size() > 0) {
            checkDayMonthYearOrder(DateArrayHelper.getFirstElement(urlResult), contResult);
        }

        if (structDates != null && structDates.size() > 0) {
            structResult.putAll((Map<? extends T, ? extends Double>) evaluateStructDate(structDates));
        }

        evaluatedDates.putAll(urlResult);

        evaluatedDates.putAll(httpResult);
        evaluatedDates.putAll(headResult);
        evaluatedDates.putAll(structResult);
        evaluatedDates.putAll(contResult);

        evaluatedDates.putAll(influenceHttpAndHead(httpResult, headResult));

        evaluatedDates.putAll(deployStructureDates(contResult, (HashMap<StructureDate, Double>) structResult));

        evaluatedDates.putAll(deployMetaDates(httpResult, contResult));
        evaluatedDates.putAll(deployMetaDates(headResult, contResult));

        // evaluatedDates.putAll(deployURLDate(urlResult, httpResult));
        // evaluatedDates.putAll(deployURLDate(urlResult, headResult));
        // evaluatedDates.putAll(deployURLDate(urlResult, structResult));
        // evaluatedDates.putAll(deployURLDate(urlResult, contResult));

        if (referneceLookUp && url != null) {
            System.out.println(1);
            DateGetter dg = new DateGetter(url);
            dg.setAllFalse();
            dg.setTechReference(true);

            ArrayList<T> newRefDates = dg.getDate();
            DateComparator dc = new DateComparator();
            ArrayList<ReferenceDate> refDates = (ArrayList<ReferenceDate>) DateArrayHelper.filter(newRefDates,
                    ExtractedDate.TECH_REFERENCE);
            ReferenceDate refDate = dc.getYoungestDate(refDates);
            evaluatedDates.put((T) refDate, refDate.get(ReferenceDate.RATE) / 100.0);

        }
        if (DateArrayHelper.isAllZero(evaluatedDates)) {
            evaluatedDates.putAll(reRateIfAllZero(contResult));
        }

        return evaluatedDates;
    }

    /**
     * @see DateEvaluatorHelper.checkDayMonthYearOrder
     * 
     */
    public <T> void checkDayMonthYearOrder(T orginalDate, HashMap<T, Double> toCheckcDates) {
        if (orginalDate != null) {
            for (Entry<T, Double> e : toCheckcDates.entrySet()) {
                if (e.getKey() != null) {
                    DateEvaluatorHelper.checkDayMonthYearOrder(orginalDate, (ExtractedDate) e.getKey());
                }

            }
        }
    }

    public static <T> HashMap<T, Double> deployMetaDates(HashMap<T, Double> metaDates, HashMap<T, Double> dates) {

        HashMap<T, Double> result = dates;
        HashMap<T, Double> temp = dates; // Where worked dates can be removed.
        HashMap<T, Double> tempContentDates = new HashMap<T, Double>(); // only dates that are equal to metaDate.
        HashMap<T, Double> tempResult = new HashMap<T, Double>(); // worked dates can be put in.

        Entry<T, Double>[] orderedMetaDates = DateArrayHelper.orderHashMap(metaDates, true);
        for (int stopcounter = 0; stopcounter < 3; stopcounter++) {
            int stopFlag = DateComparator.STOP_MINUTE - stopcounter;

            for (int i = 0; i < orderedMetaDates.length; i++) {
                T metaDate = orderedMetaDates[i].getKey();
                // DateComparator.STOP_MINUTE instead of stopFlag, because original dates should be distinguished up to
                // minute.
                int countFactor = DateArrayHelper.countDates(metaDate, metaDates, DateComparator.STOP_MINUTE) + 1;
                double metaDateFactor = metaDates.get(metaDate);
                tempContentDates = DateArrayHelper.getSameDatesMap((ExtractedDate) metaDate, temp, stopFlag);
                for (Entry<T, Double> date : tempContentDates.entrySet()) {
                    double weight = (1.0 * countFactor / metaDates.size());
                    double oldRate = date.getValue();
                    double newRate = ((1 - oldRate) * metaDateFactor * weight) + oldRate;
                    tempResult.put(date.getKey(), Math.round(newRate * 100) / 100.0);
                    temp.remove(date.getKey());

                }

            }
        }
        result.putAll(tempResult);
        return result;
    }

    /**
     * Returns joint map of head and http, where rates are recalculated by cross-dependency.
     * 
     * @param <T>
     * @param httpMap
     * @param headMap
     * @return
     */
    private <T> HashMap<T, Double> influenceHttpAndHead(HashMap<T, Double> httpMap, HashMap<T, Double> headMap) {
        HashMap<T, Double> result = new HashMap<T, Double>();
        HashMap<T, Double> resultHTTP = new HashMap<T, Double>();
        HashMap<T, Double> resultHead = new HashMap<T, Double>();

        resultHead = recalc(httpMap, headMap);
        resultHTTP = recalc(headMap, httpMap);

        result.putAll(resultHead);
        result.putAll(resultHTTP);

        return result;
    }

    /**
     * Returns map2 with new values, calculated in dependency of map1.
     * 
     * @param <T>
     * @param map1
     * @param map2
     * @return
     */
    private <T> HashMap<T, Double> recalc(HashMap<T, Double> map1, HashMap<T, Double> map2) {
        HashMap<T, Double> result = new HashMap<T, Double>();
        ArrayList<HashMap<T, Double>> arrangedMap1 = DateArrayHelper.arrangeMapByDate(map1, DateComparator.STOP_MINUTE);
        for (int i = 0; i < arrangedMap1.size(); i++) {
            HashMap<T, Double> tempMap1 = arrangedMap1.get(i);
            double map1Rate = DateArrayHelper.getHighestRate(tempMap1);
            T map1Date = DateArrayHelper.getFirstElement(tempMap1);
            HashMap<T, Double> sameMap2 = DateArrayHelper.getSameDatesMap((ExtractedDate) map1Date, map2,
                    DateComparator.STOP_DAY);

            for (Entry<T, Double> e : sameMap2.entrySet()) {
                double newRate = ((1 - e.getValue()) * map1Rate * (tempMap1.size() * 1.0 / map1.size())) + e.getValue();
                result.put(e.getKey(), Math.round(newRate * 100) / 100.0);
            }
        }
        return result;
    }

    /**
     * Recalculates dates in dependency of url-dates. <br>
     * 
     * @param <T>
     * @param urlDates
     * @param dates
     * @return
     */
    private <T> HashMap<T, Double> deployURLDate(HashMap<T, Double> urlDates, HashMap<T, Double> dates) {
        HashMap<T, Double> result = dates;
        for (Entry<T, Double> url : urlDates.entrySet()) {
            URLDate urlDate = (URLDate) url.getKey();
            double urlRate = url.getValue();
            double urlFactor = (Math.min(urlDate.getExactness(), 3)) / 3.0;
            HashMap<T, Double> temp = DateArrayHelper.getSameDatesMap(urlDate, dates, urlDate.getExactness());
            for (Entry<T, Double> date : temp.entrySet()) {
                double newRate = ((1 - date.getValue()) * (urlRate * urlFactor)) + date.getValue();
                result.put(date.getKey(), Math.round(newRate * 100) / 100.0);
            }
            temp = DateArrayHelper.getDifferentDatesMap(urlDate, dates, urlDate.getExactness());
            for (Entry<T, Double> date : temp.entrySet()) {
                double newRate = date.getValue() - (0.2 * date.getValue() * (urlRate * urlFactor));
                result.put(date.getKey(), Math.round(newRate * 100) / 100.0);
            }
        }
        return result;
    }

    /**
     * Recalculate rates, if all are zero.<br>
     * First get the dates with most dateparts (year, month, day, hour, minute, second). Oldest get the rate of <br>
     * (all oldest dates)/(all dates). <br>
     * Second get the oldest dates of all. New rate is <br>
     * ((1-value)*(all oldest dates)/(all dates))+(value). <br>
     * 
     * @param <T>
     * @param dates
     * @return
     */
    private <T> HashMap<T, Double> reRateIfAllZero(HashMap<T, Double> dates) {
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

    /**
     * Evaluates the structure-dates.
     * 
     * @param structDates
     * @return
     */
    private HashMap<StructureDate, Double> evaluateStructDate(ArrayList<StructureDate> structDates) {
        HashMap<StructureDate, Double> result = new HashMap<StructureDate, Double>();
        double rate;
        for (int i = 0; i < structDates.size(); i++) {
            StructureDate date = structDates.get(i);
            byte keywordPriority = DateEvaluatorHelper.getKeywordPriority(date);
            if (keywordPriority == KeyWords.FIRST_PRIORITY) {
                rate = 1;
            } else if (keywordPriority == KeyWords.SECOND_PRIORITY) {
                rate = -1; // TODO: rate bestimmen.

            } else {
                rate = -2;
            }
            result.put(date, rate);
        }

        ArrayList<StructureDate> highRatedDates = DateArrayHelper.getRatedDates(result, 1);
        ArrayList<StructureDate> middleRatedDates = DateArrayHelper.getRatedDates(result, -1);
        ArrayList<StructureDate> lowRatedDates = DateArrayHelper.getRatedDates(result, -2);
        if (highRatedDates.size() > 0) {
            DateEvaluatorHelper.setRateWhightedByGroups(highRatedDates, result, DateComparator.STOP_MINUTE);

            DateEvaluatorHelper.setRateToZero(middleRatedDates, result);
            DateEvaluatorHelper.setRateToZero(lowRatedDates, result);
        } else if (middleRatedDates.size() > 0) {
            DateEvaluatorHelper.setRateWhightedByGroups(middleRatedDates, result, DateComparator.STOP_MINUTE);

            DateEvaluatorHelper.setRateToZero(lowRatedDates, result);
        } else {
            DateEvaluatorHelper.setRateWhightedByGroups(lowRatedDates, result, DateComparator.STOP_MINUTE);
        }

        return result;
    }

    // zuerst high prio keywors bewerten - darumter das ältestte datum wählen rest abwerten
    // mittlere prio nur bewerten, wenn keine high prio -älteste datum auf 1, rest abwerten
    // rest daten nur wenn andere nicht vorhanden - bewertung 1/anz.

    /**
     * Evaluates the head-dates.
     * 
     */
    private HashMap<HeadDate, Double> evaluateHeadDate(ArrayList<HeadDate> headDates) {
        HashMap<HeadDate, Double> result = new HashMap<HeadDate, Double>();
        double rate;
        HeadDate date;
        for (int i = 0; i < headDates.size(); i++) {
            date = headDates.get(i);
            byte keywordPriority = DateEvaluatorHelper.getKeywordPriority(date);

            if (keywordPriority == KeyWords.FIRST_PRIORITY) {
                rate = 1;
            } else if (DateEvaluatorHelper.getKeywordPriority(date) == KeyWords.SECOND_PRIORITY) {
                rate = -1;
            } else {
                rate = -2;
            }
            result.put(date, rate);
        }
        ArrayList<HeadDate> highRatedDates = DateArrayHelper.getRatedDates(result, 1, false);
        ArrayList<HeadDate> middleRatedDates = DateArrayHelper.getRatedDates(result, -1, true);
        ArrayList<HeadDate> lowRatedDates = DateArrayHelper.getRatedDates(result, -2, true);
        if (highRatedDates.size() > 0) {
            DateEvaluatorHelper.setRateToZero(middleRatedDates, result);
            DateEvaluatorHelper.setRateToZero(lowRatedDates, result);

        } else if (middleRatedDates.size() > 0) {
            DateEvaluatorHelper.setRat(middleRatedDates, result, 1.0);
            DateEvaluatorHelper.setRateToZero(lowRatedDates, result);

        } else {

            ExtractedDate actualDate = ExtractedDateHelper.createActualDate();
            DateComparator dc = new DateComparator();
            for (int i = 0; i < lowRatedDates.size(); i++) {
                rate = 0.7;
                if (dc.getDifference(actualDate, lowRatedDates.get(i), DateComparator.MEASURE_HOUR) < 3) {
                    rate = 0.0;
                }
                result.put(lowRatedDates.get(i), rate);
            }
        }

        DateComparator dc = new DateComparator();
        ArrayList<HeadDate> dates = dc.orderDates(result);
        HeadDate oldest = dc.getOldestDate(result);
        double diff;
        double oldRate;
        double newRate;

        for (int i = 1; i < dates.size(); i++) {
            diff = dc.getDifference(oldest, dates.get(i), DateComparator.MEASURE_HOUR);
            if (diff > 24) {
                diff = 24;
            }
            date = dates.get(i);
            oldRate = result.get(date);
            newRate = oldRate - (oldRate * (diff / 24.0));
            result.put(date, Math.round(newRate * 100) / 100.0);
        }

        return result;
    }

    /**
     * Evaluates HTTP dates.
     * 
     * @param httpDates
     * @return
     */
    private HashMap<HTTPDate, Double> evaluateHTTPDate(ArrayList<HTTPDate> httpDates) {
        HTTPDate date = httpDates.get(0);
        HashMap<HTTPDate, Double> result = new HashMap<HTTPDate, Double>();
        double rate = 0;
        if (date != null) {

            ExtractedDate current = ExtractedDateHelper.createActualDate();
            DateComparator dc = new DateComparator();
            double timedifference = dc.getDifference(httpDates.get(0), current, DateComparator.MEASURE_HOUR);

            if (timedifference > 3) {
                rate = 0.75;// 75% aller Webseiten haben richtigen last modified tag, aber bei dif. von 3h ist dies zu
                // nah an
                // expiere
            } else {
                rate = 0;
            }
        }

        result.put(date, rate);
        return result;
    }

    /**
     * Evaluates the URL dates.
     * 
     * @param dates
     * @return
     */
    public static HashMap<URLDate, Double> evaluateURLDate(ArrayList<URLDate> dates) {
        HashMap<URLDate, Double> evaluate = new HashMap<URLDate, Double>();
        for (int i = 0; i < dates.size(); i++) {
            double rate = 0;
            URLDate date = dates.get(i);
            if (date != null && DateEvaluatorHelper.isDateInRange(date)) {
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
                        rate = 1; // TODO: rate genau bestimmen.
                    }
                }
            }
            evaluate.put(date, rate);
        }
        return evaluate;
    }

    /**
     * Evaluates content.dates.
     * 
     * @param dates
     * @return
     */
    private HashMap<ExtractedDate, Double> evaluateContentDate(ArrayList<ContentDate> dates) {
        HashMap<ExtractedDate, Double> result = new HashMap<ExtractedDate, Double>();

        ArrayList<ContentDate> attrDates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_KEYLOC_ATTR);
        ArrayList<ContentDate> contDates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_KEYLOC_CONT);
        ArrayList<ContentDate> nokeywordDates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_KEYLOC_NO);

        HashMap<ContentDate, Double> attrResult = DateEvaluatorHelper.evaluateKeyLocAttr(attrDates);
        HashMap<ContentDate, Double> contResult = DateEvaluatorHelper.evaluateKeyLocCont(contDates);
        HashMap<ContentDate, Double> nokeywordResult = new HashMap<ContentDate, Double>();

        // Run through dates without keyword.
        double newRate;
        for (int i = 0; i < nokeywordDates.size(); i++) {
            ContentDate date = nokeywordDates.get(i);
            String tag = date.getTag();
            String[] keys = KeyWords.allKeywords;

            newRate = 0;
            for (int j = 0; j < keys.length; j++) {
                if (tag.equalsIgnoreCase(keys[j])) {
                    newRate = 1.0 / dates.size();
                    break;
                }
            }
            nokeywordResult.put(date, Math.round(newRate * 100) / 100.0);
        }

        // increase rate, if tag is a headline tag. (h1..h6)
        attrResult = DateEvaluatorHelper.evaluateTag(attrResult);
        contResult = DateEvaluatorHelper.evaluateTag(contResult);
        nokeywordResult = DateEvaluatorHelper.evaluateTag(nokeywordResult);

        result.putAll(attrResult);
        result.putAll(contResult);
        result.putAll(nokeywordResult);

        return result;
    }

    private <C> HashMap<C, Double> deployStructureDates(HashMap<C, Double> contentDates,
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

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setReferneceLookUp(boolean referneceLookUp) {
        this.referneceLookUp = referneceLookUp;
    }
}
