package ws.palladian.extraction.date.rater;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.DateGetter;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.getter.ReferenceDateGetter;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.date.dates.ReferenceDate;

/**
 * This class rates reference dates.
 * 
 * @author Martin Gregor
 * 
 */
public class ReferenceDateRater extends TechniqueDateRater<ReferenceDate> {

    public ReferenceDateRater(PageDateType dateType) {
        super(dateType);
    }

    private String url;

    @Override
    public Map<ReferenceDate, Double> rate(List<ReferenceDate> list) {
        HashMap<ReferenceDate, Double> evaluatedDates = new HashMap<ReferenceDate, Double>();
        ReferenceDate date = getYoungest(list);
        evaluatedDates.put(date, date.getRate());
        this.ratedDates = evaluatedDates;
        return evaluatedDates;
    }

    /**
     * Use this method if there are no reference-dates jet.<br>
     * Will use standard {@link DateGetter} for getting reference dates.<br>
     * 
     * @param url
     * @return
     */
    public Map<ReferenceDate, Double> rate(String url) {
        this.url = url;
        return getRefDates();
    }

    /**
     * Get and rates referencedates.
     * 
     * @return
     */
    private Map<ReferenceDate, Double> getRefDates() {
        ReferenceDateGetter rdg = new ReferenceDateGetter();
        rdg.setUrl(url);
        List<ReferenceDate> newRefDates = rdg.getDates();

        List<ReferenceDate> refDates = DateArrayHelper.filter(newRefDates, ReferenceDate.class);

        return rate(refDates);
    }

    /**
     * 
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns youngest date of given list.
     * 
     * @param refDates List of dates.
     * @return
     */
    private ReferenceDate getYoungest(List<ReferenceDate> refDates) {
        DateComparator dc = new DateComparator();
        return dc.getYoungestDate(refDates);
    }

}
