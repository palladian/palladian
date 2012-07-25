package ws.palladian.extraction.date.rater;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.comparators.RatedDateComparator;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.dates.ArchiveDate;

/**
 * 
 * This class rates an archive date in dependency of oterh dates.<br>
 * 
 * @author Martin Gregor
 * 
 */
public class ArchiveDateRater extends TechniqueDateRater<ArchiveDate> {

    public ArchiveDateRater(PageDateType dateType) {
		super(dateType);
	}

	/**
     * Enter archive dates and other rated dates.<br>
     * Archive date will be rated in dependency of other rated dates.<br>
     * Archive date will be rated 1, if it is older then best rated date of the other dates or all the other dates are
     * rated 0.<br>
     * Otherwise it will be rate half of best rate of the other dates.
     * 
     * @param <T>
     * @param list
     * @param allDates
     * @return
     */
    public <T extends ExtractedDate> Map<ArchiveDate, Double> rate(List<ArchiveDate> list, Map<T, Double> allDates) {
        double highestRate = DateArrayHelper.getHighestRate(allDates);
        HashMap<ArchiveDate, Double> map = new HashMap<ArchiveDate, Double>();
        if (highestRate == 0.0) {
            map.put(list.get(0), 1.0);
        } else {
            List<T> sort = DateArrayHelper.mapToList(allDates);
            DateComparator dc = new DateComparator();
            Collections.sort(sort, new RatedDateComparator());
            if (dc.compare(list.get(0), (ExtractedDate) sort.get(0)) < 0) {
                map.put(list.get(0), allDates.get(sort.get(0)) / 2.0);
            } else {
                map.put(list.get(0), 1.0);
            }
        }
        list.get(0).setRate(map.get(list.get(0)));
        this.ratedDates = map;
        return map;
    }

    /**
     * Not usable for archive dates, because you need other dates for comparison.
     * Use {@link ArchiveDateRater#rate(ArrayList, HashMap)} instead.
     */
    @Override
    public Map<ArchiveDate, Double> rate(List<ArchiveDate> list) {
        // TODO Auto-generated method stub
        return null;
    }
}
