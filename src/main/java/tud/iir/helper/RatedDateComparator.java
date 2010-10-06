package tud.iir.helper;

import java.util.Comparator;

import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.daterecognition.dates.ExtractedDate;

/**
 * Compare rated dates.<br>
 * First parameter are rates of dates. <br>
 * If these are equal. Contentdates will be compared by position in document. <br>
 * All other dates will be compared by technique. For order check out static TECH properties of {@link ExtractedDate}.<br>
 * If these are equal too, last comparison is age.<br>
 * <br>
 * Be careful to set rates before using this comparator. If no rates are set, the all will be equal with -1.
 * 
 * 
 * @author Martin Gregor
 * 
 * @param <T>
 */
public class RatedDateComparator<T> implements Comparator<T> {

    @Override
    public int compare(T date1, T date2) {
        int result = compareRate((ExtractedDate) date1, (ExtractedDate) date2);
        if (result == 0) {
            if (((ExtractedDate) date1).getType() == ExtractedDate.TECH_HTML_CONT
                    && ((ExtractedDate) date2).getType() == ExtractedDate.TECH_HTML_CONT) {
                result = comparePosInDoc((ContentDate) date1, (ContentDate) date2);
            } else {
                result = compareTechniqe((ExtractedDate) date1, (ExtractedDate) date2);
            }
        }
        if (result == 0) {
            result = compareAge((ExtractedDate) date1, (ExtractedDate) date2);
        }
        return result;
    }

    /**
     * Compare by rate.
     * 
     * @param date1
     * @param date2
     * @return
     */
    private int compareRate(ExtractedDate date1, ExtractedDate date2) {
        double rate1 = date1.getRate();
        double rate2 = date2.getRate();
        return (rate1 < rate2 ? 1 : (rate1 > rate2 ? -1 : 0));
    }

    /**
     * Compare by positionin document.<br>
     * Only content dates.
     * 
     * @param date1
     * @param date2
     * @return
     */
    private int comparePosInDoc(ContentDate date1, ContentDate date2) {
        int pos1 = date1.get(ContentDate.DATEPOS_IN_DOC);
        int pos2 = date2.get(ContentDate.DATEPOS_IN_DOC);
        return pos1 > pos2 ? 1 : (pos1 < pos2 ? -1 : 0);
    }

    /**
     * Compare technique.
     * 
     * @param date1
     * @param date2
     * @return
     */
    private int compareTechniqe(ExtractedDate date1, ExtractedDate date2) {
        int tech1 = date1.getType();
        int tech2 = date2.getType();
        if (tech1 == 0) {
            tech1 = 99;
        }
        if (tech2 == 0) {
            tech2 = 99;
        }
        return tech1 > tech2 ? 1 : (tech1 < tech2 ? -1 : 0);
    }

    /**
     * Compare by age.
     * 
     * @param date1
     * @param date2
     * @return
     */
    private int compareAge(ExtractedDate date1, ExtractedDate date2) {
        DateComparator dc = new DateComparator();
        int stopFlag = Math.min(date1.getExactness(), date2.getExactness());
        return dc.compare(date1, date2, stopFlag);

    }
}
