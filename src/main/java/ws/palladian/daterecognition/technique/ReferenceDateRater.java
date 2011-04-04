package ws.palladian.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;

import ws.palladian.daterecognition.DateGetter;
import ws.palladian.daterecognition.dates.DateType;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.ReferenceDate;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.helper.date.DateComparator;

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
    public HashMap<ReferenceDate, Double> rate(ArrayList<ReferenceDate> list) {
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
    public HashMap<ReferenceDate, Double> rate(String url) {
        this.url = url;
        return getRefDates();
    }

    /**
     * Get and rates referencedates.
     * 
     * @return
     */
    private HashMap<ReferenceDate, Double> getRefDates() {
        DateGetter dg = new DateGetter(url);
        dg.setAllFalse();
        dg.setTechReference(true);

        ReferenceDateGetter rdg = new ReferenceDateGetter();
        rdg.setUrl(url);
        ArrayList<ReferenceDate> newRefDates = rdg.getDates();

        ArrayList<ReferenceDate> refDates = (ArrayList<ReferenceDate>) DateArrayHelper.filter(newRefDates, DateType.ReferenceDate);

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
    private ReferenceDate getYoungest(ArrayList<ReferenceDate> refDates) {
        DateComparator dc = new DateComparator();
        return dc.getYoungestDate(refDates);
    }

}
