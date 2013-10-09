package ws.palladian.extraction.date.comparators;

import java.util.Comparator;

import ws.palladian.extraction.date.dates.ContentDate;

/**
 * Comparator for content-dates.<br>
 * Comparison only by position in document.<br>
 * 
 * Be carefully, if a content date has no position in document, the standard value is -1. So it will be put on first
 * place of list. <br>
 * So check your dates!
 * 
 * @author Martin Gregor
 * 
 */
public class ContentDateComparator implements Comparator<ContentDate> {

    /** The singleton instance. */
    public static final ContentDateComparator INSTANCE = new ContentDateComparator();

    private ContentDateComparator() {
        // singleton instance.
    }

    @Override
    public int compare(ContentDate date1, ContentDate date2) {

        return (date1.get(ContentDate.DATEPOS_IN_DOC) < date2.get(ContentDate.DATEPOS_IN_DOC) ? -1 : (date1
                .get(ContentDate.DATEPOS_IN_DOC) > date2.get(ContentDate.DATEPOS_IN_DOC) ? 1 : 0));

    }

}
