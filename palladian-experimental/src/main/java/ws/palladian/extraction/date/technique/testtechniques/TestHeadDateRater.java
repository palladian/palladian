package ws.palladian.extraction.date.technique.testtechniques;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import ws.palladian.extraction.date.DateRaterHelper;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.extraction.date.rater.HeadDateRater;
import ws.palladian.helper.date.ExtractedDate;

public class TestHeadDateRater extends HeadDateRater {
	
    public static final byte PUB_DATE_PARAMETER = 0;
    public static final byte MOD_DATE_PARAMETER = 1;
    public static final byte TIME_DIFF_PARAMETER = 2;
    public static final byte MEASURE_PARAMETER = 3;
    public static final byte OLDEST_PARAMETER = 4;
    public static final byte YOUNGEST_PARAMERT = 5;
    
	public TestHeadDateRater(PageDateType dateType) {
		super(dateType);
	}
	
	private byte hightPriority = KeyWords.PUBLISH_KEYWORD;
	private byte middlePriority = KeyWords.MODIFIED_KEYWORD;
	//private int diffMeasure = DateComparator.MEASURE_HOUR;
	private TimeUnit diffUnit = TimeUnit.HOURS;
	private boolean old = true;
	
	public void setParameter(HashMap<Byte, Integer[]> parameter){
		for(Entry<Byte, Integer[]> e : parameter.entrySet()){
			switch(e.getKey()){
			case PUB_DATE_PARAMETER:
				setPubParameter();
				break;
			case MOD_DATE_PARAMETER:
				setModParameter();
				break;
			case MEASURE_PARAMETER:
//				setDiffMeasure(e.getValue()[0]);
				break;
			case OLDEST_PARAMETER:
				old=true;
				break;
			case YOUNGEST_PARAMERT:
				old=false;
				break;
			}
		}
	}
	
	private void setPubParameter(){
		hightPriority = KeyWords.PUBLISH_KEYWORD;
		middlePriority = KeyWords.MODIFIED_KEYWORD;
	}
	private void setModParameter(){
		hightPriority = KeyWords.MODIFIED_KEYWORD;
		middlePriority = KeyWords.PUBLISH_KEYWORD;
	}
//	private void setDiffMeasure(TimeUnit timeUnit){
//	    diffUnit = timeUnit;
//	}
	
	/**
     * Evaluates the head-dates.<br>
     * Rating by check keywords and age difference between dates.
     * 
     * @param List of head-dates.
     * @return Hashmap with dates and rateings.
     */
	protected HashMap<MetaDate, Double> evaluateHeadDate(ArrayList<MetaDate> headDates) {
		ExtractedDate actualDate = new ExtractedDate();
		return evaluateHeadDate(headDates, actualDate);
	}
	
	 /**
     * Evaluates the head-dates.<br>
     * Rating by check keywords and age difference between dates.
     * 
     * @param List of head-dates.
     * @return Hashmap with dates and rateings.
     */
	protected HashMap<MetaDate, Double> evaluateHeadDate(ArrayList<MetaDate> headDates, ExtractedDate actualDate) {
        HashMap<MetaDate, Double> result = new HashMap<MetaDate, Double>();
        for (int i = 0; i < headDates.size(); i++) {
            double rate;
            MetaDate date = headDates.get(i);
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
            DateComparator dc = new DateComparator();
            for (int i = 0; i < lowRatedDates.size(); i++) {
                double rate = 0.75;
                if (actualDate.getDifference(lowRatedDates.get(i), TimeUnit.HOURS) < 12) {
                    rate = 0.0;
                }
                result.put(lowRatedDates.get(i), rate);
            }
        }

        DateComparator dc = new DateComparator();
        List<MetaDate> dates = dc.orderDates(result.keySet(), false);
        MetaDate tempDate;
        if(old){
        	tempDate = dc.getOldestDate(DateArrayHelper.getExactestMap(result).keySet());
        }else{
        	tempDate = dc.getYoungestDate(DateArrayHelper.getExactestMap(result).keySet());
        }

        for (int i = 0; i < dates.size(); i++) {
            double diff = tempDate.getDifference(dates.get(i), diffUnit);
            if (diff > 24) {
                diff = 24;
            }
            MetaDate date = dates.get(i);
            double oldRate = result.get(date);
            double newRate = oldRate - oldRate * (diff / 24.0);
            result.put(date, Math.round(newRate * 10000) / 10000.0);
        }
        
        this.ratedDates = result;
        return result;
    }
	
	
	
	@Override
    public MetaDate getBestDate(){
		double rate = DateArrayHelper.getHighestRate(this.ratedDates);
		List<MetaDate> list = DateArrayHelper.getRatedDates(this.ratedDates, rate);
		DateComparator dc = new DateComparator();
		MetaDate date;
		if(old){
			date = dc.getOldestDate(list);
		}else{
			date = dc.getYoungestDate(list);
		}
		return date;
	}
	
}
