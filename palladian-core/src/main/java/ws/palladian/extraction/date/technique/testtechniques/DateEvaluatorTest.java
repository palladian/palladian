package ws.palladian.extraction.date.technique.testtechniques;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.extraction.date.DateRaterHelper;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.ContentDateComparator;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.extraction.date.rater.ArchiveDateRater;
import ws.palladian.extraction.date.rater.ContentDateRater;
import ws.palladian.extraction.date.rater.MetaDateRater;
import ws.palladian.extraction.date.rater.ReferenceDateRater;
import ws.palladian.extraction.date.rater.StructureDateRater;
import ws.palladian.extraction.date.rater.UrlDateRater;
import ws.palladian.helper.date.dates.ArchiveDate;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.helper.date.dates.DateExactness;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.date.dates.MetaDate;
import ws.palladian.helper.date.dates.ReferenceDate;
import ws.palladian.helper.date.dates.StructureDate;
import ws.palladian.helper.date.dates.UrlDate;

/**
 * This class is responsible for rating dates. <br>
 * Therefore it uses for each technique a own rater-class. <br>
 * In the end all techniques a compared to each other by deploying methods.
 * 
 * @author Martin Gregor
 * 
 */
public class DateEvaluatorTest {

    private String url;
    private boolean referneceLookUp = false;

    private MetaDateRater mdr;
    private UrlDateRater udr;
    private StructureDateRater sdr;
    private ContentDateRater cdr;
    private ArchiveDateRater adr;
    private ReferenceDateRater rdr;
	private ExtractedDate actualDate;

    /**
     * Standard constructor.
     */
    public DateEvaluatorTest() {
    	setPubMod(PageDateType.publish);
    }
    
    /**
     * Standard constructor.
     */
    public DateEvaluatorTest(PageDateType pub_mod) {
    	setPubMod(pub_mod);
    }

    /**
     * Constructor setting url.
     * 
     * @param url
     */
    public DateEvaluatorTest(String url, PageDateType pub_mod) {
        this.url = url;
        setPubMod(pub_mod);
    }

    /**
     * Constructor setting url and activates getting and rating of references.
     * 
     * @param url
     * @param referenceLookUp
     */
    public DateEvaluatorTest(String url, boolean referenceLookUp, PageDateType pub_mod) {
        this.url = url;
        this.referneceLookUp = referenceLookUp;
        setPubMod(pub_mod);
    }
    private void setPubMod(PageDateType pub_mod){
		mdr = new MetaDateRater(pub_mod);
		udr = new UrlDateRater(pub_mod);
		sdr = new StructureDateRater(pub_mod);
		cdr = new ContentDateRater(pub_mod);
		adr = new ArchiveDateRater(pub_mod);
		rdr = new ReferenceDateRater(pub_mod);
    }

    /**
     * Main method of this class.<br>
     * Coordinates all rating techniques and deploying of rates to lower techniques.
     * 
     * @param <T>
     * @param extractedDates ArrayList of ExtractedDates.
     * @return HashMap of dates, with rate as value.
     */
    @SuppressWarnings("unchecked")
    public <T extends ExtractedDate> Map<T, Double> rate(List<T> extractedDates) {
        Map<T, Double> evaluatedDates = new HashMap<T, Double>();

        List<T> dates = DateArrayHelper.filter(extractedDates, DateArrayHelper.FILTER_IS_IN_RANGE);
        Map<UrlDate, Double> urlResult = new HashMap<UrlDate, Double>();
        Map<T, Double> metaResult = new HashMap<T, Double>();
        Map<T, Double> structResult = new HashMap<T, Double>();
        Map<T, Double> contResult = new HashMap<T, Double>();


        List<UrlDate> urlDates = (List<UrlDate>) DateArrayHelper.filter(dates, UrlDate.class);

        List<MetaDate> metaDates = (List<MetaDate>) DateArrayHelper.filter(dates, MetaDate.class);

        List<StructureDate> structDates = (List<StructureDate>) DateArrayHelper.filter(dates, StructureDate.class);

        List<ContentDate> contDates = (List<ContentDate>) DateArrayHelper.filter(dates, ContentDate.class);
        List<ContentDate> contFullDates = (List<ContentDate>) DateArrayHelper.filter(contDates,
                DateArrayHelper.FILTER_FULL_DATE);

        List<ArchiveDate> archiveDate = (List<ArchiveDate>) DateArrayHelper.filter(dates, ArchiveDate.class);

        List<ReferenceDate> referenceDate = (List<ReferenceDate>) DateArrayHelper.filter(dates, ReferenceDate.class);

        if (urlDates != null && urlDates.size() > 0) {
            urlResult.putAll((Map<UrlDate, Double>) udr.rate(urlDates));
        }
        if (metaDates != null && metaDates.size() > 0) {
        	if(actualDate != null){
        		mdr.setActualDate(actualDate);
        	}
            metaResult.putAll((Map<? extends T, ? extends Double>) mdr.rate(metaDates));
        }

        if (contFullDates != null && contFullDates.size() > 0) {
            contResult.putAll((Map<? extends T, ? extends Double>) cdr.rate(contFullDates));
        } else if (contDates != null && contDates.size() > 0) {
            contResult.putAll((Map<? extends T, ? extends Double>) cdr.rate(contDates));
        }
        if (urlResult.size() > 0 && contResult.size() > 0) {
            checkDayMonthYearOrder(DateArrayHelper.getFirstElement(urlResult), contResult);
        }

        if (structDates != null && structDates.size() > 0) {
            structResult.putAll((Map<? extends T, ? extends Double>) sdr.rate(structDates));
        }

        evaluatedDates.putAll((Map<? extends T, ? extends Double>)urlResult);

        evaluatedDates.putAll(metaResult);
        evaluatedDates.putAll(structResult);
        evaluatedDates.putAll(contResult);


        evaluatedDates.putAll(deployStructureDates(contResult, (HashMap<StructureDate, Double>) structResult));

        evaluatedDates.putAll(deployMetaDates(metaResult, contResult));
        evaluatedDates.putAll(deployMetaDates(metaResult, structResult));

        evaluatedDates.putAll(deployUrlDate(urlResult, metaResult));
        evaluatedDates.putAll(deployUrlDate(urlResult, structResult));
        evaluatedDates.putAll(deployUrlDate(urlResult, contResult));

        if (referenceDate != null && referenceDate.size() > 0) {
            rdr.rate(referenceDate);
        } else if (referneceLookUp && url != null) {
            rdr.rate(url);
        }

        if (DateArrayHelper.isAllZero(evaluatedDates)) {
            // evaluatedDates.putAll(reRateIfAllZero(contResult));
            evaluatedDates
                    .putAll((Map<? extends T, ? extends Double>) guessRate((HashMap<ContentDate, Double>) contResult));

        }

        DateRaterHelper.writeRateInDate(evaluatedDates);

        if (archiveDate != null && archiveDate.size() > 0) {
            evaluatedDates.putAll((Map<? extends T, ? extends Double>) adr.rate(archiveDate, evaluatedDates));
        }

        return evaluatedDates;
    }

    /**
     * @see DateRaterHelper.checkDayMonthYearOrder
     * 
     */
    public <T extends ExtractedDate, U extends ExtractedDate> void checkDayMonthYearOrder(T orginalDate, Map<U, Double> toCheckDates) {
        if (orginalDate != null) {
            for (Entry<U, Double> e : toCheckDates.entrySet()) {
                if (e.getKey() != null) {
                    DateRaterHelper.checkDayMonthYearOrder(orginalDate, e.getKey());
                }

            }
        }
    }

    /**
     * Method with calculations for new rating of dates by consideration of meta-dates like HTTP and Head.
     * 
     * @param <T>
     * @param metaDates HashMap with HTTP or head-Dates.
     * @param dates Dates to be rated.
     * @return
     */
    public static <T extends ExtractedDate> Map<T, Double> deployMetaDates(Map<T, Double> metaDates, Map<T, Double> dates) {

        Map<T, Double> result = dates;
        Map<T, Double> temp = dates; // Where worked dates can be removed.
        Map<T, Double> tempContentDates; // only dates that are equal to metaDate.
        Map<T, Double> tempResult = new HashMap<T, Double>(); // worked dates can be put in.

        Entry<T, Double>[] orderedMetaDates = DateArrayHelper.orderHashMap(metaDates, true);
        for (int stopcounter = 0; stopcounter < 3; stopcounter++) {
            // int stopFlag = DateComparator.STOP_MINUTE - stopcounter;
            int stopFlag = DateExactness.MINUTE.getValue() - stopcounter;

            for (int i = 0; i < orderedMetaDates.length; i++) {
                T metaDate = orderedMetaDates[i].getKey();
                // DateComparator.STOP_MINUTE instead of stopFlag, because original dates should be distinguished up to
                // minute.
                int countFactor = DateArrayHelper.countDates(metaDate, metaDates, DateExactness.MINUTE.getValue()) + 1;
                double metaDateFactor = metaDates.get(metaDate);
                tempContentDates = DateArrayHelper.getSameDatesMap(metaDate, temp, DateExactness.byValue(stopFlag));
                for (Entry<T, Double> date : tempContentDates.entrySet()) {
                    double weight = 1.0 * countFactor / metaDates.size();
                    double oldRate = date.getValue();
                    double excatnesFactor = stopFlag / (1.0 * (date.getKey()).getExactness().getValue());
                    double newRate = (1 - oldRate) * metaDateFactor * weight * excatnesFactor + oldRate;
                    tempResult.put(date.getKey(), Math.round(newRate * 10000) / 10000.0);
                    temp.remove(date.getKey());
                }

            }
        }
        result.putAll(tempResult);
        return result;
    }

//    /**
//     * Returns joint map of head and http, where rates are recalculated by cross-dependency.
//     * 
//     * @param <T>
//     * @param httpMap
//     * @param headMap
//     * @return
//     */
//    private <T> HashMap<T, Double> influenceHttpAndHead(HashMap<T, Double> httpMap, HashMap<T, Double> headMap) {
//        HashMap<T, Double> result = new HashMap<T, Double>();
//        HashMap<T, Double> resultHTTP;
//        HashMap<T, Double> resultHead;
//
//        resultHead = recalc(httpMap, headMap);
//        resultHTTP = recalc(headMap, httpMap);
//
//        result.putAll(resultHead);
//        result.putAll(resultHTTP);
//
//        return result;
//    }

//    /**
//     * Returns map2 with new values, calculated in dependency of map1.
//     * 
//     * @param <T>
//     * @param map1
//     * @param map2
//     * @return
//     */
//    private <T> HashMap<T, Double> recalc(HashMap<T, Double> map1, HashMap<T, Double> map2) {
//        HashMap<T, Double> result = new HashMap<T, Double>();
//        List<Map<T, Double>> arrangedMap1 = DateArrayHelper.arrangeMapByDate(map1, DateComparator.STOP_MINUTE);
//        for (int i = 0; i < arrangedMap1.size(); i++) {
//            Map<T, Double> tempMap1 = arrangedMap1.get(i);
//            double map1Rate = DateArrayHelper.getHighestRate(tempMap1);
//            T map1Date = DateArrayHelper.getFirstElement(tempMap1);
//            Map<T, Double> sameMap2 = DateArrayHelper.getSameDatesMap((ExtractedDate) map1Date, map2,
//                    DateComparator.STOP_DAY);
//
//            for (Entry<T, Double> e : sameMap2.entrySet()) {
//                double newRate = ((1 - e.getValue()) * map1Rate * (tempMap1.size() * 1.0 / map1.size())) + e.getValue();
//                result.put(e.getKey(), Math.round(newRate * 1000) / 10000.0);
//            }
//        }
//        return result;
//    }

    /**
     * Recalculates dates in dependency of url-dates. <br>
     * 
     * @param <T>
     * @param urlDates
     * @param dates
     * @return
     */
    private <T extends ExtractedDate> Map<T, Double> deployUrlDate(Map<UrlDate, Double> urlDates, Map<T, Double> dates) {
        Map<T, Double> result = dates;
        for (Entry<UrlDate, Double> url : urlDates.entrySet()) {
            UrlDate urlDate = url.getKey();
            double urlRate = url.getValue();
            DateExactness compareDepth = urlDate.getExactness();
            double urlFactor = Math.min(compareDepth.getValue(), 3) / 3.0;
            Map<T, Double> temp = DateArrayHelper.getSameDatesMap(urlDate, dates, compareDepth);
            for (Entry<T, Double> date : temp.entrySet()) {
                double newRate = (1 - date.getValue()) * (urlRate * urlFactor) + date.getValue();
                result.put(date.getKey(), Math.round(newRate * 10000) / 10000.0);
            }
            /**
             * temp = DateArrayHelper.getDifferentDatesMap(urlDate, dates, urlDate.getExactness());
             * for (Entry<T, Double> date : temp.entrySet()) {
             * double newRate = date.getValue() - (0.2 * date.getValue() * (urlRate * urlFactor));
             * result.put(date.getKey(), Math.round(newRate * 10000) / 10000.0);
             * }
             */
        }
        return result;
    }

//    /**
//     * Recalculate rates, if all are zero.<br>
//     * First get the dates with most dateparts (year, month, day, hour, minute, second). Oldest get the rate of <br>
//     * (all oldest dates)/(all dates). <br>
//     * Second get the oldest dates of all. New rate is <br>
//     * ((1-value)*(all oldest dates)/(all dates))+(value). <br>
//     * 
//     * @param <T>
//     * @param dates
//     * @return
//     */
//    private <T> HashMap<T, Double> reRateIfAllZero(HashMap<T, Double> dates) {
//        HashMap<T, Double> result = dates;
//
//        Map<T, Double> exactestDates = DateArrayHelper.getExactestMap(dates);
//        DateComparator dc = new DateComparator();
//        T date = dc.getOldestDate(exactestDates);
//
//        Map<T, Double> sameDates = DateArrayHelper.getSameDatesMap((ExtractedDate) date, result);
//        for (Entry<T, Double> e : sameDates.entrySet()) {
//            double newRate = (0.1 * sameDates.size()) / result.size();
//            result.put(e.getKey(), Math.round(newRate * 10000) / 10000.0);
//        }
//
//        return result;
//
//    }

    /**
     * This method calculates new rates for content-dates in dependency of position in document and age of date.
     * 
     * @param dates
     * @return New rated dates.
     */
    private HashMap<ContentDate, Double> guessRate(HashMap<ContentDate, Double> dates) {
        HashMap<ContentDate, Double> result = dates;
        if (result.size() > 0) {
            List<ContentDate> orderAge = DateArrayHelper.mapToList(dates);
            List<ContentDate> orderPosInDoc = orderAge;

            DateComparator dc = new DateComparator();
            Collections.sort(orderAge, dc);
            Collections.sort(orderPosInDoc, new ContentDateComparator());

            double factorAge;
            double factorPos;
            double factorRate;
            double newRate;
            double oldRate;

            int ageSize = orderAge.size();
            int maxPos = orderPosInDoc.get(orderPosInDoc.size() - 1).get(ContentDate.DATEPOS_IN_DOC);
            int counter = 0;

            ContentDate temp = orderAge.get(0);
            ContentDate actDate;

            for (int i = 0; i < ageSize; i++) {
                actDate = orderAge.get(i);
                if (dc.compare(temp, actDate) != 0) {
                    temp = orderAge.get(i);
                    counter++;
                }
                factorAge = (ageSize - counter) * 1.0 / (ageSize * 1.0);
                factorPos = 1 - actDate.get(ContentDate.DATEPOS_IN_DOC) * 1.0 / (maxPos * 1.0);
                factorPos += 0.01;
                factorRate = factorAge * factorPos;
                oldRate = dates.get(actDate);
                newRate = oldRate + (1 - oldRate) * factorRate;
                result.put(actDate, Math.round(newRate * 10000) / 10000.0);
            }
        }
        normalizeRate(result);
        return result;
    }

    /**
     * If some rates are greater then one, use this method to normalize them.
     * 
     * @param <T>
     * @param dates
     */
    private <T> void normalizeRate(HashMap<T, Double> dates) {
        double highestRate = DateArrayHelper.getHighestRate(dates);
        if (highestRate > 1.0) {
            for (Entry<T, Double> e : dates.entrySet()) {
                dates.put(e.getKey(), Math.round(e.getValue() / highestRate * 10000) / 10000.0);
            }
        }

    }

    /**
     * Content-dates get a new rate in dependency of structure dates.
     * 
     * @param <C>
     * @param contentDates
     * @param structDates
     * @return
     */
    private <C> Map<C, Double> deployStructureDates(Map<C, Double> contentDates,
            Map<StructureDate, Double> structDates) {
        DateComparator dc = new DateComparator();
        List<StructureDate> structureDates = dc.orderDates(structDates.keySet(), true);
        Map<C, Double> result = contentDates;
        Map<C, Double> temp = contentDates;
        Map<C, Double> tempContentDates = new HashMap<C, Double>();
        HashMap<C, Double> tempResult = new HashMap<C, Double>();
        for (int i = 0; i < structureDates.size(); i++) {
            tempContentDates = DateArrayHelper.getSameDatesMap(structureDates.get(i), temp,
                    DateExactness.MINUTE);
            if (tempContentDates.size() == 0) {
                tempContentDates = DateArrayHelper.getSameDatesMap(structureDates.get(i), temp,
                        DateExactness.HOUR);
            }
            if (tempContentDates.size() == 0) {
                tempContentDates = DateArrayHelper.getSameDatesMap(structureDates.get(i), temp,
                        DateExactness.DAY);
            }
            for (Entry<C, Double> cDate : tempContentDates.entrySet()) {
                String cDateTag = ((ContentDate) cDate.getKey()).getTagNode();
                String eTag = structureDates.get(i).getTagNode();
                if (cDateTag.equalsIgnoreCase(eTag)) {
                    double structValue = structDates.get(structureDates.get(i));
                    double newRate = (1 - cDate.getValue()) * structValue + cDate.getValue();
                    tempResult.put(cDate.getKey(), Math.round(newRate * 10000) / 10000.0);
                    temp.remove(cDate.getKey());
                }
            }

        }
        result.putAll(tempResult);
        return result;
    }

    /**
     * Set url.
     * 
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Getter for url.
     * 
     * @return Url as a String.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Activate or disable the possibility of using reference-technique.
     * 
     * @param referneceLookUp
     */
    public void setReferneceLookUp(boolean referneceLookUp) {
        this.referneceLookUp = referneceLookUp;
    }
}
