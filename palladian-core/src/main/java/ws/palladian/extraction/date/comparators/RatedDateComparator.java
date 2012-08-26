package ws.palladian.extraction.date.comparators;

import java.util.Comparator;

import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.StructureDate;
import ws.palladian.extraction.date.dates.UrlDate;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.ExtractedDate;

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
 * @author Philipp Katz
 * 
 * @param <T>
 */
public class RatedDateComparator implements Comparator<ExtractedDate> {

    @Override
    public int compare(ExtractedDate date1, ExtractedDate date2) {
        int result = compareRate(date1, date2);
        if (result == 0) {
            if (date1 instanceof ContentDate && date2 instanceof ContentDate) {
                result = compareDocumentPosition((ContentDate)date1, (ContentDate)date2);
            } else {
                result = compareTechnique(date1, date2);
            }
        }
        if (result == 0) {
            result = compareAge(date1, date2);
        }
        return result;
    }

    /**
     * <p>
     * Compare by rate.
     * </p>
     * 
     * @param date1
     * @param date2
     * @return
     */
    private static int compareRate(ExtractedDate date1, ExtractedDate date2) {
        double rate1 = date1.getRate();
        double rate2 = date2.getRate();
        return Double.valueOf(rate2).compareTo(rate1);
    }

    /**
     * <p>
     * Compare {@link ContentDate}s by position in document.
     * </p>
     * 
     * @param date1
     * @param date2
     * @return
     */
    private static int compareDocumentPosition(ContentDate date1, ContentDate date2) {
        int pos1 = date1.get(ContentDate.DATEPOS_IN_DOC);
        int pos2 = date2.get(ContentDate.DATEPOS_IN_DOC);
        return Integer.valueOf(pos1).compareTo(pos2);
    }

    /**
     * <p>
     * Compare by technique.
     * </p>
     * 
     * @param date1
     * @param date2
     * @return
     */
    private static int compareTechnique(ExtractedDate date1, ExtractedDate date2) {
        int tech1 = getTypeValue(date1);
        int tech2 = getTypeValue(date2);
        return Integer.valueOf(tech1).compareTo(tech2);
    }

    private static int getTypeValue(ExtractedDate date) {
        if (date instanceof StructureDate) {
            return 4;
        }
        if (date instanceof MetaDate) {
            return 3;
        }
        if (date instanceof ContentDate) {
            return 2;
        }
        if (date instanceof UrlDate) {
            return 1;
        }
        return 99;
    }

    /**
     * <p>
     * Compare by age.
     * </p>
     * 
     * @param date1
     * @param date2
     * @return
     */
    private static int compareAge(ExtractedDate date1, ExtractedDate date2) {
        DateExactness compareDepth = DateExactness.getCommonExactness(date1.getExactness(), date2.getExactness());
        DateComparator dateComparator = new DateComparator(compareDepth);
        return dateComparator.compare(date1, date2);
    }
}
