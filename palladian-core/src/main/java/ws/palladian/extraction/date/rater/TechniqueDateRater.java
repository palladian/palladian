package ws.palladian.extraction.date.rater;

import java.util.Collections;
import java.util.List;

import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.RatedDateComparator;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.helper.date.ExtractedDate;

/**
 * <p>
 * Template for rater classes. Each technique that evaluates dates should implements this.
 * </p>
 * 
 * @author Martin Gregor
 * 
 * @param <T> subtype of {@link ExtractedDate} which concrete rater implementations process.
 */
public abstract class TechniqueDateRater<E extends ExtractedDate> {

    protected final PageDateType dateType;
    
    public TechniqueDateRater() {
        this(null); // FIXME
    }
    
    public TechniqueDateRater(PageDateType dateType) {
        this.dateType = dateType;
    }

    /**
     * Enter a list of dates. <br>
     * These will be rated in dependency of date-technique.
     * 
     * @param dates
     * @return
     */
    public abstract List<RatedDate<E>> rate(List<E> dates);
    
    public RatedDate<E> getBest(List<E> dates) {
        List<RatedDate<E>> ratedDates = rate(dates);
        if (ratedDates.size() > 0) {
            Collections.sort(ratedDates, new RatedDateComparator());
            return ratedDates.get(0);
        }
        return null;
    }

//    /**
//     * Returns best rated date of property "ratedDates". <br>
//     * In case of more than one best date the first one will be returned. <br>
//     * For other function override this method in subclasses.
//     * 
//     * @return
//     */
//    public T getBestDate() {
//        T date = null;
//        if (this.ratedDates.size() > 0) {
//            double rate = DateArrayHelper.getHighestRate(this.ratedDates);
//            date = DateArrayHelper.getRatedDates(this.ratedDates, rate).get(0);
//        }
//        return date;
//    }

}