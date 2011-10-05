package ws.palladian.extraction.date.technique;

import java.util.ArrayList;
import java.util.HashMap;

import ws.palladian.extraction.date.DateRaterHelper;
import ws.palladian.extraction.date.ExtractedDateHelper;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.dates.ExtractedDate;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.helper.date.DateComparator;

/**
 * This class evaluates date of HTML-head.<br>
 * 
 * @author Martin Gregor
 * 
 */
public class HeadDateRater extends TechniqueDateRater<MetaDate> {

	protected byte hightPriority;
	protected byte middlePriority;
	protected byte lowPriority;
	
	private ExtractedDate actualDate;
	
    public HeadDateRater(PageDateType dateType) {
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
    public HashMap<MetaDate, Double> rate(ArrayList<MetaDate> list) {
        HashMap<MetaDate, Double> returnDates = evaluateHeadDate(list);
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
     * @param List of head-dates.
     * @return Hashmap with dates and rateings.
     */
    protected HashMap<MetaDate, Double> evaluateHeadDate(ArrayList<MetaDate> headDates) {
        HashMap<MetaDate, Double> result = new HashMap<MetaDate, Double>();
        double rate;
        MetaDate date;
        for (int i = 0; i < headDates.size(); i++) {
            date = headDates.get(i);
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
        ArrayList<MetaDate> highRatedDates = DateArrayHelper.getRatedDates(result, 1, true);
        ArrayList<MetaDate> middleRatedDates = DateArrayHelper.getRatedDates(result, -1, true);
        ArrayList<MetaDate> lowRatedDates = DateArrayHelper.getRatedDates(result, -2, true);
        if (highRatedDates.size() > 0) {
            DateRaterHelper.setRateToZero(middleRatedDates, result);
            DateRaterHelper.setRateToZero(lowRatedDates, result);

        } else if (middleRatedDates.size() > 0) {
            DateRaterHelper.setRat(middleRatedDates, result, 1.0);
            DateRaterHelper.setRateToZero(lowRatedDates, result);

        } else {

        	if(actualDate == null){
        		actualDate = ExtractedDateHelper.createActualDate();
        	}
            DateComparator dc = new DateComparator();
            for (int i = 0; i < lowRatedDates.size(); i++) {
                rate = 0.75;
                if (dc.getDifference(actualDate, lowRatedDates.get(i), DateComparator.MEASURE_HOUR) < 12) {
                    rate = 0.0;
                }
                result.put(lowRatedDates.get(i), rate);
            }
        }

        DateComparator dc = new DateComparator();
        ArrayList<MetaDate> dates = dc.orderDates(result);
        MetaDate tempDate;
        switch(dateType){
	        case publish:
	        	tempDate = dc.getOldestDate(DateArrayHelper.getExactestMap(result));
	        	break;
	        case last_modified:
	        	tempDate = dc.getYoungestDate(DateArrayHelper.getExactestMap(result));
	        	break;
        	default:
        		tempDate = dc.getOldestDate(DateArrayHelper.getExactestMap(result));
        		break;
        }

        double diff;
        double oldRate;
        double newRate;

        for (int i = 0; i < dates.size(); i++) {
            diff = dc.getDifference(tempDate, dates.get(i), DateComparator.MEASURE_HOUR);
            if (diff > 24) {
                diff = 24;
            }
            date = dates.get(i);
            oldRate = result.get(date);
            newRate = oldRate - (oldRate * (diff / 24.0));
            result.put(date, Math.round(newRate * 10000) / 10000.0);
        }
        
        return result;
    }

    public void setActualDate(ExtractedDate actualDate){
    	this.actualDate = actualDate;
    }
}
