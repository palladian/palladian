package ws.palladian.extraction.date.rater;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.ExtractedDateImpl;

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
    public List<RatedDate<MetaDate>> rate(List<MetaDate> list) {
	    List<RatedDate<MetaDate>> returnDates = evaluateMetaDates(list);
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
    protected List<RatedDate<MetaDate>> evaluateMetaDates(List<MetaDate> metaDates) {
        List<RatedDate<MetaDate>> result = CollectionHelper.newArrayList();
        for (int i = 0; i < metaDates.size(); i++) {
            double rate;
            MetaDate date = metaDates.get(i);
            byte keywordPriority = KeyWords.getKeywordPriority(date);
            if (keywordPriority == hightPriority) {
                rate = 1;
            } else if (keywordPriority == middlePriority) {
                rate = -1;
            } else {
                rate = -2;
            }
            result.add(RatedDate.create(date, rate));
        }
        List<MetaDate> highRatedDates = DateExtractionHelper.getRatedDates(result, 1);
        List<MetaDate> middleRatedDates = DateExtractionHelper.getRatedDates(result, -1);
        List<MetaDate> lowRatedDates = DateExtractionHelper.getRatedDates(result, -2);
        if (highRatedDates.size() > 0) {
            result.addAll(DateExtractionHelper.setRate(middleRatedDates, 0.0));
            result.addAll(DateExtractionHelper.setRate(lowRatedDates, 0.0));

        } else if (middleRatedDates.size() > 0) {
            result.addAll(DateExtractionHelper.setRate(middleRatedDates, 1.0));
            result.addAll(DateExtractionHelper.setRate(lowRatedDates, 0.0));

        } else {
        	if(currentDate == null){
        		currentDate = new ExtractedDateImpl();
        	}
            for (int i = 0; i < lowRatedDates.size(); i++) {
                double rate = 0.75;
                if (currentDate.getDifference(lowRatedDates.get(i), TimeUnit.HOURS) < 12) {
                    rate = 0.0;
                }
                result.add(RatedDate.create(lowRatedDates.get(i), rate));
            }
        }

        DateComparator dc = new DateComparator();
        RatedDate<MetaDate> tempDate;
        switch(dateType){
	        case PUBLISH:
	        	tempDate = dc.getOldestDate(DateExtractionHelper.filterExactest(result));
	        	break;
	        case LAST_MODIFIED:
	        	tempDate = dc.getYoungestDate(DateExtractionHelper.filterExactest(result));
	        	break;
        	default:
        		tempDate = dc.getOldestDate(DateExtractionHelper.filterExactest(result));
        		break;
        }

        List<RatedDate<MetaDate>> dates = dc.orderDates(result, false);
        for (int i = 0; i < dates.size(); i++) {
            double diff = tempDate.getDifference(dates.get(i), TimeUnit.HOURS);
            if (diff > 24) {
                diff = 24;
            }
            RatedDate<MetaDate> date = dates.get(i);
            double oldRate = date.getRate();
            double newRate = oldRate - oldRate * (diff / 24.0);
            result.add(RatedDate.create(date.getDate(), Math.round(newRate * 10000) / 10000.0));
        }
        
        return result;
    }

    public void setCurrentDate(ExtractedDate actualDate){
    	this.currentDate = actualDate;
    }
}
