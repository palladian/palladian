package ws.palladian.extraction.date.rater;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.comparators.RatedDateComparator;
import ws.palladian.extraction.date.dates.ArchiveDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.ExtractedDate;

/**
 * <p>This {@link TechniqueDateRater} rates an {@link ArchiveDate} in dependency of other dates.</p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class ArchiveDateRater extends TechniqueDateRater<ArchiveDate> {

    /**
     * Enter archive dates and other rated dates.<br>
     * Archive date will be rated in dependency of other rated dates.<br>
     * Archive date will be rated 1, if it is older then best rated date of the other dates or all the other dates are
     * rated 0.<br>
     * Otherwise it will be rate half of best rate of the other dates.
     * 
     * @param <T>
     * @param dates
     * @param allDates
     * @return
     */
    public List<RatedDate<ArchiveDate>> rate(List<ArchiveDate> dates, List<? extends RatedDate<?>> allDates) {
        List<RatedDate<ArchiveDate>> result = CollectionHelper.newArrayList();
        
        
        Map<ExtractedDate, Double> datesWeights = CollectionHelper.newHashMap();
        for (RatedDate<?> ratedDate : allDates) {
            datesWeights.put(ratedDate.getDate(), ratedDate.getRate());
        }
        
        double highestRate = DateExtractionHelper.getHighestRate(allDates);
        if (highestRate == 0.0) {
            result.add(RatedDate.create(dates.get(0), 1.0));
        } else {
            List<RatedDate<?>> sortedDates = new ArrayList<RatedDate<?>>(allDates);
            Collections.sort(sortedDates, RatedDateComparator.INSTANCE);
            DateComparator dateComparator = new DateComparator();
            
            if (dateComparator.compare(dates.get(0), sortedDates.get(0)) < 0) {
                result.add(RatedDate.create(dates.get(0), datesWeights.get(sortedDates.get(0)) / 2.0));
            } else {
                result.add(RatedDate.create(dates.get(0), 1.0));
            }
        }
        // dates.get(0).setRate(map.get(dates.get(0)));
        return result;
    }

    /**
     * Not usable for archive dates, because you need other dates for comparison.
     * Use {@link ArchiveDateRater#rate(ArrayList, HashMap)} instead.
     */
    @Override
    public List<RatedDate<ArchiveDate>> rate(List<ArchiveDate> list) {
        throw new UnsupportedOperationException("Not usable for ArchiveDates, as other dates are required for comparison. Use #rate(List<ArchiveDate>, List<RatedDate<T>>) instead.");
    }
}
