package ws.palladian.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;

import ws.palladian.daterecognition.DateRaterHelper;
import ws.palladian.daterecognition.KeyWords;
import ws.palladian.daterecognition.dates.StructureDate;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.helper.date.DateComparator;

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
    public HashMap<StructureDate, Double> rate(ArrayList<StructureDate> list) {
    	HashMap<StructureDate, Double> returnDates = evaluateStructDate(list); 
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
    private HashMap<StructureDate, Double> evaluateStructDate(ArrayList<StructureDate> structDates) {
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

        ArrayList<StructureDate> highRatedDates = DateArrayHelper.getRatedDates(result, 1);
        ArrayList<StructureDate> middleRatedDates = DateArrayHelper.getRatedDates(result, -1);
        ArrayList<StructureDate> lowRatedDates = DateArrayHelper.getRatedDates(result, -2);
        if (highRatedDates.size() > 0) {
            DateRaterHelper.setRateWhightedByGroups(highRatedDates, result, DateComparator.STOP_MINUTE);

            DateRaterHelper.setRateToZero(middleRatedDates, result);
            DateRaterHelper.setRateToZero(lowRatedDates, result);
        } else if (middleRatedDates.size() > 0) {
            DateRaterHelper.setRateWhightedByGroups(middleRatedDates, result, DateComparator.STOP_MINUTE);

            DateRaterHelper.setRateToZero(lowRatedDates, result);
        } else {
            DateRaterHelper.setRateWhightedByGroups(lowRatedDates, result, DateComparator.STOP_MINUTE);
        }
        return result;
    }

}
