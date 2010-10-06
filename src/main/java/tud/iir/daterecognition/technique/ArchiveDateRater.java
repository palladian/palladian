package tud.iir.daterecognition.technique;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import tud.iir.daterecognition.dates.ArchiveDate;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;
import tud.iir.helper.RatedDateComparator;

/**
 * 
 * This class rates an archive date in dependency of oterh dates.<br>
 * 
 * @author Martin Gregor
 * 
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
     * @param list
     * @param allDates
     * @return
     */
    public <T> HashMap<ArchiveDate, Double> rate(ArrayList<ArchiveDate> list, HashMap<T, Double> allDates) {
        double highestRate = DateArrayHelper.getHighestRate(allDates);
        HashMap<ArchiveDate, Double> map = new HashMap<ArchiveDate, Double>();
        if (highestRate == 0.0) {
            map.put(list.get(0), 1.0);
        } else {
            ArrayList<T> sort = DateArrayHelper.hashMapToArrayList(allDates);
            DateComparator dc = new DateComparator();
            Collections.sort(sort, new RatedDateComparator<T>());
            if (dc.compare(list.get(0), (ExtractedDate) sort.get(0)) < 0) {
                map.put(list.get(0), allDates.get(sort.get(0)) / 2.0);
            } else {
                map.put(list.get(0), 1.0);
            }
        }
        list.get(0).setRate(map.get(list.get(0)));
        return map;
    }

    /**
     * Not usable for archive dates, because you need other dates for comparison.
     * Use {@link ArchiveDateRater#rate(ArrayList, HashMap)} instead.
     */
    @Override
    public HashMap<ArchiveDate, Double> rate(ArrayList<ArchiveDate> list) {
        // TODO Auto-generated method stub
        return null;
    }

}
