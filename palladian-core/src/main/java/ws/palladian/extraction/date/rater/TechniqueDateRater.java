package ws.palladian.extraction.date.rater;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

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
     * <p>
     * Rate the supplied {@link List} of {@link ExtractedDate}s and return a List of rated dates.
     * </p>
     * 
     * @param dates The List of ExtractedDates to rate, not <code>null</code>.
     * @return A List of RatedDates, or an empty List of an empty List was supplied, never <code>null</code>.
     */
    public abstract List<RatedDate<E>> rate(List<E> dates);

    /**
     * <p>
     * Rate the supplied {@link List} of {@link ExtractedDate}s and return the top rated date.
     * </p>
     * 
     * @param dates The List of ExtractedDates to rate, not <code>null</code>.
     * @return The {@link RatedDate} with the highest rate, or <code>null</code> no date was rated.
     */
    public RatedDate<E> getBest(List<E> dates) {
        Validate.notNull(dates, "dates must not be null");

        List<RatedDate<E>> ratedDates = rate(dates);
        if (ratedDates.size() > 0) {
            Collections.sort(ratedDates, RatedDateComparator.INSTANCE);
            return ratedDates.get(0);
        }
        return null;
    }

}