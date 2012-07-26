package ws.palladian.extraction.date.rater;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.dates.MetaDate;

/**
 * This class rates HTTP-dates by constant and age of date.
 * 
 * @author Martin Greogr
 * 
 */
public class HttpDateRater extends TechniqueDateRater<MetaDate> {

    private ExtractedDate actualDate;
	public HttpDateRater(PageDateType dateType) {
		super(dateType);
	}

	@Override
    public Map<MetaDate, Double> rate(List<MetaDate> list) {
    	Map<MetaDate, Double> returnDates = evaluateHTTPDate(list);
    	this.ratedDates = returnDates;
        return returnDates;
    }

    /**
     * Evaluates HTTP dates.<br>
     * Therefore, a date older then 12 hours from this point of time will be rated with 0.75.
     * If more then one date exists, a weight reduces each rating in dependency of age.
     * 
     * @param httpDates
     * @return
     */
    private Map<MetaDate, Double> evaluateHTTPDate(List<MetaDate> httpDates) {
    	ExtractedDate current = actualDate;
    	if(current == null){
    		current = new ExtractedDate();
    	}
    	return evaluateHTTPDate(httpDates, current);
    }

    /**
     * Evaluates HTTP dates.<br>
     * Therefore, a date older then 12 hours from this point of time will be rated with 0.75.
     * If more then one date exists, a weight reduces each rating in dependency of age.
     * 
     * @param httpDates
     * @return
     */
    public HashMap<MetaDate, Double> evaluateHTTPDate(List<MetaDate> httpDates,ExtractedDate downloadedDate) {
    	MetaDate date = null;
        HashMap<MetaDate, Double> result = new HashMap<MetaDate, Double>();
        double rate = 0;
        for (int i = 0; i < httpDates.size(); i++) {
            date = httpDates.get(i);
            
            double timedifference = httpDates.get(i).getDifference(downloadedDate, TimeUnit.HOURS);

            if (timedifference > 12) {
                rate = 0.75;// 75% aller Webseiten haben richtigen last modified tag, aber bei dif. von 3h ist dies zu
                // nah an
                // expiere
            } else {
                rate = 0;
            }
            result.put(date, rate);
        }

        DateComparator dc = new DateComparator();
        List<MetaDate> dates = dc.orderDates(result.keySet(), false);
        MetaDate oldest = dc.getOldestDate(DateArrayHelper.getExactestMap(result).keySet());

        double diff;
        double oldRate;
        double newRate;

        for (int i = 0; i < dates.size(); i++) {
            diff = oldest.getDifference(dates.get(i), TimeUnit.HOURS);
            if (diff > 24) {
                diff = 24;
            }
            date = dates.get(i);
            oldRate = result.get(date);
            newRate = oldRate - oldRate * (diff / 24.0);
            result.put(date, Math.round(newRate * 10000) / 10000.0);
        }

        return result;
    }
    public void setActualDate(ExtractedDate actualDate){
    	this.actualDate = actualDate;
    }
}
