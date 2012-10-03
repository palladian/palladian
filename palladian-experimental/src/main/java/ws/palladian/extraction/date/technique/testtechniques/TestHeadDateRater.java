package ws.palladian.extraction.date.technique.testtechniques;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.extraction.date.rater.HeadDateRater;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.ExtractedDateImpl;

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
	protected List<RatedDate<MetaDate>> evaluateHeadDate(ArrayList<MetaDate> headDates) {
		ExtractedDate actualDate = new ExtractedDateImpl();
		return evaluateHeadDate(headDates, actualDate);
	}
	
	 /**
     * Evaluates the head-dates.<br>
     * Rating by check keywords and age difference between dates.
     * 
     * @param List of head-dates.
     * @return Hashmap with dates and rateings.
     */
	protected List<RatedDate<MetaDate>> evaluateHeadDate(ArrayList<MetaDate> headDates, ExtractedDate actualDate) {
        List<RatedDate<MetaDate>> result = CollectionHelper.newArrayList();
        for (int i = 0; i < headDates.size(); i++) {
            double rate;
            MetaDate date = headDates.get(i);
            byte keywordPriority = KeyWords.getKeywordPriority(date.getKeyword());
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
            result.addAll(DateExtractionHelper.setRate(middleRatedDates, 0));
            result.addAll(DateExtractionHelper.setRate(lowRatedDates, 0));

        } else if (middleRatedDates.size() > 0) {
            result.addAll(DateExtractionHelper.setRate(middleRatedDates, 1));
            result.addAll(DateExtractionHelper.setRate(lowRatedDates, 0));
        } else {
            DateComparator dc = new DateComparator();
            for (int i = 0; i < lowRatedDates.size(); i++) {
                double rate = 0.75;
                if (actualDate.getDifference(lowRatedDates.get(i), TimeUnit.HOURS) < 12) {
                    rate = 0.0;
                }
                result.add(RatedDate.create(lowRatedDates.get(i), rate));
            }
        }

        DateComparator dc = new DateComparator();
        List<RatedDate<MetaDate>> dates = dc.orderDates(result, false);
        RatedDate<MetaDate> tempDate;
        if(old){
        	tempDate = dc.getOldestDate(DateExtractionHelper.filterExactest(result));
        }else{
        	tempDate = dc.getYoungestDate(DateExtractionHelper.filterExactest(result));
        }

        for (int i = 0; i < dates.size(); i++) {
            double diff = tempDate.getDifference(dates.get(i), diffUnit);
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
	
	
	
//	@Override
//    public MetaDate getBestDate(){
//		double rate = DateArrayHelper.getHighestRate(this.ratedDates);
//		List<MetaDate> list = DateArrayHelper.getRatedDates(this.ratedDates, rate);
//		DateComparator dc = new DateComparator();
//		MetaDate date;
//		if(old){
//			date = dc.getOldestDate(list);
//		}else{
//			date = dc.getYoungestDate(list);
//		}
//		return date;
//	}
	
}
