package ws.palladian.extraction.date.rater;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.DateRaterHelper;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.dates.StructureDate;

/**
 * This class rates structure dates by keywords.
 * 
 * @author Martin Gregor
 * 
 */
public class StructureDateRater extends TechniqueDateRater<StructureDate> {

	//protected double minRate = 0.3;
	
	protected byte hightPriority;
	protected byte middlePriority;
	protected byte lowPriority;
	
    public StructureDateRater(PageDateType dateType) {
		super(dateType);
		if(this.dateType.equals(PageDateType.publish)){
			hightPriority = KeyWords.PUBLISH_KEYWORD;
			middlePriority = KeyWords.MODIFIED_KEYWORD;
			lowPriority = KeyWords.OTHER_KEYWORD;
		}else{
			hightPriority = KeyWords.MODIFIED_KEYWORD;
			middlePriority = KeyWords.PUBLISH_KEYWORD;
			lowPriority = KeyWords.OTHER_KEYWORD;
		}
	}

	@Override
    public Map<StructureDate, Double> rate(List<StructureDate> list) {
    	Map<StructureDate, Double> returnDates = evaluateStructDate(list); 
    	this.ratedDates = returnDates;
    	return returnDates;
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
    private Map<StructureDate, Double> evaluateStructDate(List<StructureDate> structDates) {
        HashMap<StructureDate, Double> result = new HashMap<StructureDate, Double>();
        double rate;
        for (int i = 0; i < structDates.size(); i++) {
            StructureDate date = structDates.get(i);
            byte keywordPriority = DateRaterHelper.getKeywordPriority(date);
            if (keywordPriority == hightPriority) {
                rate = 1;
            } else if (keywordPriority == middlePriority) {
                rate = -1; // TODO: rate bestimmen.

            } else if (keywordPriority == lowPriority) {
                rate = -2; // TODO: rate bestimmen.

            } else {
                rate = 0;
            }
            result.put(date, rate);
        }

        List<StructureDate> highRatedDates = DateArrayHelper.getRatedDates(result, 1);
        List<StructureDate> middleRatedDates = DateArrayHelper.getRatedDates(result, -1);
        List<StructureDate> lowRatedDates = DateArrayHelper.getRatedDates(result, -2);
        if (highRatedDates.size() > 0) {
            DateRaterHelper.setRateWhightedByGroups(highRatedDates, result, DateExactness.MINUTE);

            DateRaterHelper.setRateToZero(middleRatedDates, result);
            DateRaterHelper.setRateToZero(lowRatedDates, result);
        } else if (middleRatedDates.size() > 0) {
            DateRaterHelper.setRateWhightedByGroups(middleRatedDates, result, DateExactness.MINUTE);

            DateRaterHelper.setRateToZero(lowRatedDates, result);
        } else {
            DateRaterHelper.setRateWhightedByGroups(lowRatedDates, result, DateExactness.MINUTE);
        }
        return result;
    }

}
