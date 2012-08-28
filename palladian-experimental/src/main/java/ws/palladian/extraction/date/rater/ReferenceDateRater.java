package ws.palladian.extraction.date.rater;

import java.util.List;

import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.dates.ReferenceDate;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * This class rates {@link ReferenceDate}s. From a List of ReferenceDates, simply the youngest date is returned.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class ReferenceDateRater extends TechniqueDateRater<ReferenceDate> {

    private final DateComparator dateComparator = new DateComparator();

    @Override
    public List<RatedDate<ReferenceDate>> rate(List<ReferenceDate> list) {
        List<RatedDate<ReferenceDate>> result = CollectionHelper.newArrayList();
        ReferenceDate youngestDate = dateComparator.getYoungestDate(list);
        if (youngestDate != null) {
            result.add(RatedDate.create(youngestDate, 0));
        }
        return result;
    }

}
