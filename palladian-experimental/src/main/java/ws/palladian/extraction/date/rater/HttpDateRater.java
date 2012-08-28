package ws.palladian.extraction.date.rater;

import java.util.List;
import java.util.concurrent.TimeUnit;

import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.ExtractedDateImpl;

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
    public List<RatedDate<MetaDate>> rate(List<MetaDate> list) {
    	return evaluateHTTPDate(list);
    }

    /**
     * Evaluates HTTP dates.<br>
     * Therefore, a date older then 12 hours from this point of time will be rated with 0.75.
     * If more then one date exists, a weight reduces each rating in dependency of age.
     * 
     * @param httpDates
     * @return
     */
    private List<RatedDate<MetaDate>> evaluateHTTPDate(List<MetaDate> httpDates) {
    	ExtractedDate current = actualDate;
    	if(current == null){
    		current = new ExtractedDateImpl();
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
    public List<RatedDate<MetaDate>> evaluateHTTPDate(List<MetaDate> httpDates,ExtractedDate downloadedDate) {
        List<RatedDate<MetaDate>> result = CollectionHelper.newArrayList();
        double rate = 0;
        for (int i = 0; i < httpDates.size(); i++) {
            MetaDate date = httpDates.get(i);
            
            double timedifference = httpDates.get(i).getDifference(downloadedDate, TimeUnit.HOURS);

            if (timedifference > 12) {
                rate = 0.75;// 75% aller Webseiten haben richtigen last modified tag, aber bei dif. von 3h ist dies zu
                // nah an
                // expiere
            } else {
                rate = 0;
            }
            result.add(RatedDate.create(date, rate));
        }

        DateComparator dc = new DateComparator();
        List<RatedDate<MetaDate>> dates = dc.orderDates(result, false);
        RatedDate<MetaDate> oldest = dc.getOldestDate(DateExtractionHelper.filterExactest(result));

        for (int i = 0; i < dates.size(); i++) {
            double diff = oldest.getDifference(dates.get(i), TimeUnit.HOURS);
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
    public void setActualDate(ExtractedDate actualDate){
    	this.actualDate = actualDate;
    }
}
