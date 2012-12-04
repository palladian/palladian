package ws.palladian.extraction.date.rater;

import java.util.List;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.dates.StructureDate;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.ExtractedDate;

/**
 * This class rates structure dates by keywords.
 * 
 * @author Martin Gregor
 * 
 */
public class StructureDateRater extends TechniqueDateRater<StructureDate> {

    protected byte hightPriority;
    protected byte middlePriority;
    protected byte lowPriority;

    public StructureDateRater(PageDateType dateType) {
        super(dateType);
        if (this.dateType.equals(PageDateType.PUBLISH)) {
            hightPriority = KeyWords.PUBLISH_KEYWORD;
            middlePriority = KeyWords.MODIFIED_KEYWORD;
            lowPriority = KeyWords.OTHER_KEYWORD;
        } else {
            hightPriority = KeyWords.MODIFIED_KEYWORD;
            middlePriority = KeyWords.PUBLISH_KEYWORD;
            lowPriority = KeyWords.OTHER_KEYWORD;
        }
    }

    @Override
    public List<RatedDate<StructureDate>> rate(List<StructureDate> list) {
        List<RatedDate<StructureDate>> result = evaluateStructDate(list);
        return result;
    }

    /**
     * Evaluates the structure-dates.<br>
     * There for keywords are distinct into classes. See {@link KeyWords#getKeywordPriority(String)}.<br>
     * Only best class will be rated, all other get value of 0. <br>
     * Also a weight in dependency of number of dates will be set.
     * 
     * @param structDates
     * @return
     */
    private List<RatedDate<StructureDate>> evaluateStructDate(List<StructureDate> structDates) {
        List<RatedDate<StructureDate>> result = CollectionHelper.newArrayList();
        for (StructureDate structDate : structDates) {
            byte keywordPriority = KeyWords.getKeywordPriority(structDate.getKeyword());
            double rate;
            if (keywordPriority == hightPriority) {
                rate = 1;
            } else if (keywordPriority == middlePriority) {
                rate = -1; // TODO: rate bestimmen.
            } else if (keywordPriority == lowPriority) {
                rate = -2; // TODO: rate bestimmen.
            } else {
                rate = 0;
            }
            result.add(RatedDate.create(structDate, rate));
        }

        List<StructureDate> highRatedDates = DateExtractionHelper.getRatedDates(result, 1);
        List<StructureDate> middleRatedDates = DateExtractionHelper.getRatedDates(result, -1);
        List<StructureDate> lowRatedDates = DateExtractionHelper.getRatedDates(result, -2);
        if (highRatedDates.size() > 0) {
            setRateWheightedByGroups(highRatedDates, result, DateExactness.MINUTE);
            result.addAll(DateExtractionHelper.setRate(middleRatedDates, 0.0));
            result.addAll(DateExtractionHelper.setRate(lowRatedDates, 0.0));
        } else if (middleRatedDates.size() > 0) {
            setRateWheightedByGroups(middleRatedDates, result, DateExactness.MINUTE);
            result.addAll(DateExtractionHelper.setRate(lowRatedDates, 0.0));
        } else {
            setRateWheightedByGroups(lowRatedDates, result, DateExactness.MINUTE);
        }
        return result;
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
    private static <T extends ExtractedDate> void setRateWheightedByGroups(List<T> datesToSet, List<RatedDate<T>> dates,
            DateExactness compareDepth) {
        List<List<T>> groupedDates = DateExtractionHelper.cluster(datesToSet, compareDepth);
        for (int k = 0; k < groupedDates.size(); k++) {
            for (int i = 0; i < groupedDates.get(k).size(); i++) {
                double newRate = 1.0 * groupedDates.get(k).size() / datesToSet.size();
                dates.add(RatedDate.create(groupedDates.get(k).get(i), Math.round(newRate * 10000) / 10000.0));
            }
        }
    }

}
