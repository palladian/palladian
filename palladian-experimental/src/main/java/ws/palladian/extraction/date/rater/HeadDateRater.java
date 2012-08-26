package ws.palladian.extraction.date.rater;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ws.palladian.extraction.date.DateRaterHelper;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.date.ExtractedDate;

/**
 * This class evaluates date of HTML-head.<br>
 * 
 * @author Martin Gregor
 * 
 */
public class HeadDateRater extends TechniqueDateRater<MetaDate> {

	protected final byte hightPriority;
	protected final byte middlePriority;
	protected final byte lowPriority;
	
	private ExtractedDate currentDate;
	
    public HeadDateRater(PageDateType dateType) {
		super(dateType);
		if(this.dateType.equals(PageDateType.PUBLISH)){
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
    public Map<MetaDate, Double> rate(List<MetaDate> list) {
        Map<MetaDate, Double> returnDates = evaluateMetaDates(list);
        this.ratedDates = returnDates;
        return returnDates;
    }

    // zuerst high prio keywors bewerten - darumter das ältestte datum wählen rest abwerten
    // mittlere prio nur bewerten, wenn keine high prio -älteste datum auf 1, rest abwerten
    // rest daten nur wenn andere nicht vorhanden - bewertung 1/anz.

    /**
     * Evaluates the head-dates.<br>
     * Rating by check keywords and age difference between dates.
     * 
     * @param {@link List} of {@link MetaDate}s.
     * @return {@link Map} with dates and ratings.
     */
    protected Map<MetaDate, Double> evaluateMetaDates(List<MetaDate> metaDates) {
        Map<MetaDate, Double> result = new HashMap<MetaDate, Double>();
        for (int i = 0; i < metaDates.size(); i++) {
            double rate;
            MetaDate date = metaDates.get(i);
            byte keywordPriority = DateRaterHelper.getKeywordPriority(date);
            if (keywordPriority == hightPriority) {
                rate = 1;
            } else if (keywordPriority == middlePriority) {
                rate = -1;
            } else {
                rate = -2;
            }
            result.put(date, rate);
        }
        List<MetaDate> highRatedDates = DateArrayHelper.getRatedDates(result, 1, true);
        List<MetaDate> middleRatedDates = DateArrayHelper.getRatedDates(result, -1, true);
        List<MetaDate> lowRatedDates = DateArrayHelper.getRatedDates(result, -2, true);
        if (highRatedDates.size() > 0) {
            DateRaterHelper.setRateToZero(middleRatedDates, result);
            DateRaterHelper.setRateToZero(lowRatedDates, result);

        } else if (middleRatedDates.size() > 0) {
            DateRaterHelper.setRate(middleRatedDates, result, 1.0);
            DateRaterHelper.setRateToZero(lowRatedDates, result);

        } else {
        	if(currentDate == null){
        		currentDate = new ExtractedDate();
        	}
            for (int i = 0; i < lowRatedDates.size(); i++) {
                double rate = 0.75;
                if (currentDate.getDifference(lowRatedDates.get(i), TimeUnit.HOURS) < 12) {
                    rate = 0.0;
                }
                result.put(lowRatedDates.get(i), rate);
            }
        }

        DateComparator dc = new DateComparator();
        List<MetaDate> dates = dc.orderDates(result.keySet(), false);
        MetaDate tempDate;
        switch(dateType){
	        case PUBLISH:
	        	tempDate = dc.getOldestDate(DateArrayHelper.getExactestMap(result).keySet());
	        	break;
	        case LAST_MODIFIED:
	        	tempDate = dc.getYoungestDate(DateArrayHelper.getExactestMap(result).keySet());
	        	break;
        	default:
        		tempDate = dc.getOldestDate(DateArrayHelper.getExactestMap(result).keySet());
        		break;
        }

        for (int i = 0; i < dates.size(); i++) {
            double diff = tempDate.getDifference(dates.get(i), TimeUnit.HOURS);
            if (diff > 24) {
                diff = 24;
            }
            MetaDate date = dates.get(i);
            double oldRate = result.get(date);
            double newRate = oldRate - oldRate * (diff / 24.0);
            result.put(date, Math.round(newRate * 10000) / 10000.0);
        }
        
        return result;
    }

    public void setCurrentDate(ExtractedDate actualDate){
    	this.currentDate = actualDate;
    }
}
