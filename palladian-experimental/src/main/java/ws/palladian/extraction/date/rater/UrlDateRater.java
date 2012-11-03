package ws.palladian.extraction.date.rater;

import java.util.List;

import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.dates.UrlDate;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.RegExp;

/**
 * 
 * This class evaluates an url-date and rates it in dependency of found format.<br>
 * 
 * @author Martin Gregor
 * 
 */
public class UrlDateRater extends TechniqueDateRater<UrlDate> {

	@Override
    public List<RatedDate<UrlDate>> rate(List<UrlDate> list) {
        return evaluateURLDate(list);
    }

    /**
     * Evaluates the URL dates.<br>
     * Evaluated rate depends on format of date.<br>
     * 
     * @param dates
     * @return
     */
    private List<RatedDate<UrlDate>> evaluateURLDate(List<UrlDate> dates) {
        List<RatedDate<UrlDate>> evaluate = CollectionHelper.newArrayList();
        for (UrlDate date : dates) {
            double rate = 0;
            if (date != null && DateExtractionHelper.isDateInRange(date)) {
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
            evaluate.add(RatedDate.create(date, rate));
        }
        return evaluate;
    }

}
