package ws.palladian.extraction.date.rater;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.DateRaterHelper;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.dates.UrlDate;

/**
 * 
 * This class evaluates an url-date and rates it in dependency of found format.<br>
 * 
 * @author Martin Gregor
 * 
 */
public class UrlDateRater extends TechniqueDateRater<UrlDate> {

    public UrlDateRater(PageDateType dateType) {
		super(dateType);
	}

	@Override
    public Map<UrlDate, Double> rate(List<UrlDate> list) {
        return evaluateURLDate(list);
    }

    /**
     * Evaluates the URL dates.<br>
     * Evaluated rate depends on format of date.<br>
     * 
     * @param dates
     * @return
     */
    private Map<UrlDate, Double> evaluateURLDate(List<UrlDate> dates) {
        HashMap<UrlDate, Double> evaluate = new HashMap<UrlDate, Double>();
        for (UrlDate date : dates) {
            double rate = 0;
            if (date != null && DateRaterHelper.isDateInRange(date)) {
                String format = date.getFormat();
                if (format != null) {
                    if (format.equalsIgnoreCase(RegExp.DATE_URL_D.getFormat())) {
                        rate = 0.95;
                    } else if (format.equalsIgnoreCase(RegExp.DATE_URL_SPLIT.getFormat())) {
                        rate = 0.98;
                    } else if (format.equalsIgnoreCase(RegExp.DATE_URL.getFormat())) {
                        rate = 0.99;
                    } else if (format.equalsIgnoreCase(RegExp.DATE_URL_MMMM_D.getFormat())) {
                        rate = 1.0;
                    } else {
                        rate = 0.88; // TODO: rate genau bestimmen.
                    }
                }
            }
            evaluate.put(date, rate);
        }
        this.ratedDates = evaluate;
        return evaluate;
    }

}
