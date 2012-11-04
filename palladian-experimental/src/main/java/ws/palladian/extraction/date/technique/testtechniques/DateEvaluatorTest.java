package ws.palladian.extraction.date.technique.testtechniques;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.ContentDateComparator;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.comparators.RatedDateComparator;
import ws.palladian.extraction.date.dates.ArchiveDate;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.dates.ReferenceDate;
import ws.palladian.extraction.date.dates.StructureDate;
import ws.palladian.extraction.date.dates.UrlDate;
import ws.palladian.extraction.date.getter.ReferenceDateGetter;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.extraction.date.rater.ArchiveDateRater;
import ws.palladian.extraction.date.rater.ContentDateRater;
import ws.palladian.extraction.date.rater.MetaDateRater;
import ws.palladian.extraction.date.rater.ReferenceDateRater;
import ws.palladian.extraction.date.rater.StructureDateRater;
import ws.palladian.extraction.date.rater.UrlDateRater;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.DateFormat;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.ExtractedDate;

/**
 * This class is responsible for rating dates. <br>
 * Therefore it uses for each technique a own rater-class. <br>
 * In the end all techniques a compared to each other by deploying methods.
 * 
 * @author Martin Gregor
 * 
 */
public class DateEvaluatorTest {

    private final String url;
    private final boolean referenceLookUp;

    private final MetaDateRater metaDateRater;
    private final UrlDateRater urlDateRater;
    private final StructureDateRater structureDateRater;
    private final ContentDateRater contentDateRater;
    private final ArchiveDateRater archiveDateRater;
    private final ReferenceDateRater referenceDateRater;
    
    // TODO ???
	private ExtractedDate actualDate;

    /**
     * Standard constructor.
     */
    public DateEvaluatorTest() {
        this(PageDateType.PUBLISH);
    }
    
    /**
     * Standard constructor.
     */
    public DateEvaluatorTest(PageDateType pageDateType) {
        this(null, pageDateType);
    }

    /**
     * Constructor setting url.
     * 
     * @param url
     */
    public DateEvaluatorTest(String url, PageDateType pageDateType) {
        this(url, pageDateType, false);
    }

    /**
     * Constructor setting url and activates getting and rating of references.
     * 
     * @param url
     * @param referenceLookUp
     */
    public DateEvaluatorTest(String url, PageDateType dateType, boolean referenceLookUp) {
        this.url = url;
        this.referenceLookUp = referenceLookUp;
        metaDateRater = new MetaDateRater(dateType);
        urlDateRater = new UrlDateRater();
        structureDateRater = new StructureDateRater(dateType);
        contentDateRater = new ContentDateRater(dateType);
        archiveDateRater = new ArchiveDateRater();
        referenceDateRater = new ReferenceDateRater();
    }

    /**
     * Main method of this class.<br>
     * Coordinates all rating techniques and deploying of rates to lower techniques.
     * 
     * @param <T>
     * @param extractedDates ArrayList of ExtractedDates.
     * @return HashMap of dates, with rate as value.
     */
    public List<RatedDate<? extends ExtractedDate>> rate(List<ExtractedDate> extractedDates) {
        List<RatedDate<? extends ExtractedDate>> evaluatedDates = CollectionHelper.newArrayList();

        List<ExtractedDate> dates = DateExtractionHelper.filterByRange(extractedDates);
        List<RatedDate<UrlDate>> urlResult = CollectionHelper.newArrayList();
        List<RatedDate<MetaDate>> metaResult = CollectionHelper.newArrayList();
        List<RatedDate<StructureDate>> structResult = CollectionHelper.newArrayList();
        List<RatedDate<ContentDate>> contResult = CollectionHelper.newArrayList();


        List<UrlDate> urlDates = DateExtractionHelper.filter(dates, UrlDate.class);

        List<MetaDate> metaDates = DateExtractionHelper.filter(dates, MetaDate.class);

        List<StructureDate> structDates = DateExtractionHelper.filter(dates, StructureDate.class);

        List<ContentDate> contDates = DateExtractionHelper.filter(dates, ContentDate.class);
        List<ContentDate> contFullDates = DateExtractionHelper.filterFullDate(contDates);

        List<ArchiveDate> archiveDates = DateExtractionHelper.filter(dates, ArchiveDate.class);

        List<ReferenceDate> referenceDates = DateExtractionHelper.filter(dates, ReferenceDate.class);

        if (urlDates.size() > 0) {
            urlResult.addAll(urlDateRater.rate(urlDates));
        }
        if (metaDates.size() > 0) {
        	if(actualDate != null){
        		metaDateRater.setActualDate(actualDate);
        	}
            metaResult.addAll(metaDateRater.rate(metaDates));
        }

        if (contFullDates.size() > 0) {
            contResult.addAll(contentDateRater.rate(contFullDates));
        } else if (contDates.size() > 0) {
            contResult.addAll(contentDateRater.rate(contDates));
        }
        if (urlResult.size() > 0 && contResult.size() > 0) {
            checkDayMonthYearOrder(urlResult.get(0), contResult);
        }

        if (structDates.size() > 0) {
            structResult.addAll(structureDateRater.rate(structDates));
        }

        evaluatedDates.addAll(urlResult);
        evaluatedDates.addAll(metaResult);
        evaluatedDates.addAll(structResult);
        evaluatedDates.addAll(contResult);


        evaluatedDates.addAll(deployStructureDates(contResult, structResult));

        evaluatedDates.addAll(deployMetaDates(metaResult, contResult));
        evaluatedDates.addAll(deployMetaDates(metaResult, structResult));

        evaluatedDates.addAll(deployUrlDate(urlResult, metaResult));
        evaluatedDates.addAll(deployUrlDate(urlResult, structResult));
        evaluatedDates.addAll(deployUrlDate(urlResult, contResult));

        if (referenceDates.size() > 0) {
            referenceDateRater.rate(referenceDates);
        } else if (referenceLookUp && url != null) {
            ReferenceDateGetter rdg = new ReferenceDateGetter();
            List<ReferenceDate> newRefDates = rdg.getDates(url);
            referenceDateRater.rate(newRefDates);
        }

        if (DateExtractionHelper.isAllZero(evaluatedDates)) {
            evaluatedDates.addAll(guessRate(contResult));
        }

        if (archiveDates.size() > 0) {
            evaluatedDates.addAll(archiveDateRater.rate(archiveDates, evaluatedDates));
        }

        return evaluatedDates;
    }

    /**
     * @see DateRaterHelper.checkDayMonthYearOrder
     * 
     */
    public void checkDayMonthYearOrder(RatedDate<?> orginalDate, List<? extends RatedDate<?>> checkDates) {
        if (orginalDate != null) {
            for (RatedDate<?> e : checkDates) {
                checkDayMonthYearOrder(orginalDate, e);
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
    public static <T extends ExtractedDate> List<RatedDate<T>> deployMetaDates(List<RatedDate<MetaDate>> metaDates, List<RatedDate<T>> dates) {

        List<RatedDate<T>> result = dates;
        List<RatedDate<T>> temp = dates; // Where worked dates can be removed.
        List<RatedDate<T>> tempContentDates; // only dates that are equal to metaDate.
        List<RatedDate<T>> tempResult = CollectionHelper.newArrayList(); // worked dates can be put in.
        
        List<RatedDate<MetaDate>> sortedMetaDates = new ArrayList<RatedDate<MetaDate>>(metaDates);
        RatedDateComparator ratedDateComparator = new RatedDateComparator();
        Collections.sort(sortedMetaDates, ratedDateComparator);

        for (int stopCounter = 0; stopCounter < 3; stopCounter++) {
            int stopFlag = DateExactness.MINUTE.getValue() - stopCounter;

            for (RatedDate<MetaDate> ratedMetaDate : sortedMetaDates) {
                // DateComparator.STOP_MINUTE instead of stopFlag, because original dates should be distinguished up to
                // minute.
                int countFactor = DateExtractionHelper.countDates(ratedMetaDate, metaDates, DateExactness.MINUTE) + 1;
                double metaDateFactor = ratedMetaDate.getRate();
                tempContentDates = DateExtractionHelper.getSameDatesMap(ratedMetaDate, temp, DateExactness.byValue(stopFlag));
                for (RatedDate<T> date : tempContentDates) {
                    double weight = (double) countFactor / metaDates.size();
                    double oldRate = date.getRate();
                    double excatnesFactor = (double) stopFlag / (date.getExactness().getValue());
                    double newRate = (1 - oldRate) * metaDateFactor * weight * excatnesFactor + oldRate;
                    tempResult.add(RatedDate.create(date.getDate(), Math.round(newRate * 10000) / 10000.0));
                    temp.remove(date);
                }
            }
        }
        result.addAll(tempResult);
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
    private static <T extends ExtractedDate> List<RatedDate<T>> deployUrlDate(List<RatedDate<UrlDate>> urlDates, List<RatedDate<T>> dates) {
        List<RatedDate<T>> result = dates;
        for (RatedDate<UrlDate> urlDate : urlDates) {
            DateExactness compareDepth = urlDate.getExactness();
            double urlFactor = Math.min(compareDepth.getValue(), 3) / 3.0;
            List<RatedDate<T>> temp = DateExtractionHelper.getSameDatesMap(urlDate, dates, compareDepth);
            for (RatedDate<T> date : temp) {
                double newRate = (1 - date.getRate()) * (urlDate.getRate() * urlFactor) + date.getRate();
                result.add(RatedDate.create(date.getDate(), Math.round(newRate * 10000) / 10000.0));
            }
        }
        return result;
    }

    /**
     * This method calculates new rates for content-dates in dependency of position in document and age of date.
     * 
     * @param dates
     * @return New rated dates.
     */
    private List<RatedDate<ContentDate>> guessRate(List<RatedDate<ContentDate>> dates) {
        List<RatedDate<ContentDate>> result = dates;
        if (result.size() > 0) {
            List<RatedDate<ContentDate>> orderAge = new ArrayList<RatedDate<ContentDate>>(dates);
            List<RatedDate<ContentDate>> orderPosInDoc = orderAge;

            DateComparator dc = new DateComparator();
            Collections.sort(orderAge, dc);
            Collections.sort(orderPosInDoc, new Comparator<RatedDate<ContentDate>>() {
                ContentDateComparator contentDateComparator = new ContentDateComparator();
                public int compare(RatedDate<ContentDate> o1, RatedDate<ContentDate> o2) {
                    return contentDateComparator.compare(o1.getDate(), o2.getDate());
                }
            });

            int ageSize = orderAge.size();
            int maxPos = orderPosInDoc.get(orderPosInDoc.size() - 1).get(ContentDate.DATEPOS_IN_DOC);
            int counter = 0;

            RatedDate<ContentDate> temp = orderAge.get(0);

            for (int i = 0; i < ageSize; i++) {
                RatedDate<ContentDate> actDate = orderAge.get(i);
                if (dc.compare(temp, actDate) != 0) {
                    temp = orderAge.get(i);
                    counter++;
                }
                double factorAge = (double) (ageSize - counter) / ageSize;
                double factorPos = 1.01 - (double) actDate.get(ContentDate.DATEPOS_IN_DOC) / maxPos;
                double factorRate = factorAge * factorPos;
                double oldRate = actDate.getRate();
                double newRate = oldRate + (1 - oldRate) * factorRate;
                result.add(RatedDate.create(actDate.getDate(), Math.round(newRate * 10000) / 10000.0));
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
     * @return 
     */
    private <T extends ExtractedDate> List<RatedDate<T>> normalizeRate(List<RatedDate<T>> dates) {
        double highestRate = DateExtractionHelper.getHighestRate(dates);
        List<RatedDate<T>> result = CollectionHelper.newArrayList();
        if (highestRate > 1.0) {
            for (RatedDate<T> date : dates) {
                result.add(RatedDate.create(date.getDate(), Math.round(date.getRate() / highestRate * 10000) / 10000.0));
            }
        }
        return result;
    }

    /**
     * Content-dates get a new rate in dependency of structure dates.
     * 
     * @param <C>
     * @param contentDates
     * @param structDates
     * @return
     */
    private static List<RatedDate<ContentDate>> deployStructureDates(List<RatedDate<ContentDate>> contentDates,
            List<RatedDate<StructureDate>> structDates) {
        DateComparator dc = new DateComparator();
        List<RatedDate<StructureDate>> structureDates = dc.orderDates(structDates, true);
        List<RatedDate<ContentDate>> result = contentDates;
        List<RatedDate<ContentDate>> temp = contentDates;
        List<RatedDate<ContentDate>> tempContentDates = CollectionHelper.newArrayList();
        
        Map<StructureDate, Double> structureWeights = CollectionHelper.newHashMap();
        for (RatedDate<StructureDate> structureDate : structDates) {
            structureWeights.put(structureDate.getDate(), structureDate.getRate());
        }
        
        for (int i = 0; i < structureDates.size(); i++) {
            tempContentDates = DateExtractionHelper.getSameDatesMap(structureDates.get(i), temp,
                    DateExactness.MINUTE);
            if (tempContentDates.size() == 0) {
                tempContentDates = DateExtractionHelper.getSameDatesMap(structureDates.get(i), temp,
                        DateExactness.HOUR);
            }
            if (tempContentDates.size() == 0) {
                tempContentDates = DateExtractionHelper.getSameDatesMap(structureDates.get(i), temp,
                        DateExactness.DAY);
            }
            for (RatedDate<ContentDate> cDate : tempContentDates) {
                String cDateTag = cDate.getDate().getTag();
                String eTag = structureDates.get(i).getDate().getTag();
                if (cDateTag.equalsIgnoreCase(eTag)) {
                    double structRate = structureWeights.get(structureDates.get(i));
                    double newRate = (1 - cDate.getRate()) * structRate + cDate.getRate();
                    result.add(RatedDate.create(cDate.getDate(), Math.round(newRate * 10000) / 10000.0));
                    temp.remove(cDate);
                }
            }

        }
        return result;
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
    private static <T extends ExtractedDate> void checkDayMonthYearOrder(T orginalDate, ExtractedDate toCheckDate) {
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

}
