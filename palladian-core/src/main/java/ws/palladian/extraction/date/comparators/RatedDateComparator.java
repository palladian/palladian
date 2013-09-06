package ws.palladian.extraction.date.comparators;

import java.util.Comparator;

import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.dates.StructureDate;
import ws.palladian.extraction.date.dates.UrlDate;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.ExtractedDate;

/**
 * <p>
 * Comparator for {@link RatedDate}s. Comparison is done in the following order:
 * <ul>
 * <li>Rate of the date.</li>
 * <li>{@link ContentDate}s are compared by their position in the document.</li>
 * <li>Other {@link ExtractedDate}s are compared by their technique.</li>
 * <li>Age.</li>
 * </ul>
 * <br>
 * <b>Be careful to set rates before using this comparator.</b>
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class RatedDateComparator implements Comparator<RatedDate<? extends ExtractedDate>> {

    /** The singleton instance. */
    public static final RatedDateComparator INSTANCE = new RatedDateComparator();

    private RatedDateComparator() {
        // singleton instance.
    }

    @Override
    public int compare(RatedDate<?> ratedDate1, RatedDate<?> ratedDate2) {
        ExtractedDate date1 = ratedDate1.getDate();
        ExtractedDate date2 = ratedDate2.getDate();

        int result = compareRate(ratedDate1, ratedDate2);
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
     * @param ratedDate1
     * @param ratedDate2
     * @return
     */
    private static int compareRate(RatedDate<?> ratedDate1, RatedDate<?> ratedDate2) {
        double rate1 = ratedDate1.getRate();
        double rate2 = ratedDate2.getRate();
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
