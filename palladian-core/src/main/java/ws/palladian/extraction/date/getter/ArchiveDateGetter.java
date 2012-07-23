package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.date.dates.ArchiveDate;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * This class uses www.archive.org to get a date of a page.
 * 
 * @author Martin Gregor
 * 
 */
public class ArchiveDateGetter extends TechniqueDateGetter<ArchiveDate> {

    @Override
    public ArrayList<ArchiveDate> getDates() {
        ArrayList<ArchiveDate> result = new ArrayList<ArchiveDate>();
        if (url != null) {
            result.add(getArchiveDates(this.url));
        }
        return result;
    }

    /**
     * Looks up in www.archive.org for given url.<br>
     * Searches all content-dates and discards oldest date. Because this is since when archive.org is crawling.
     * Second oldest is first found of url.<br>
     * <br>
     * Uses http://web.archive.org/web/ * / + url;
     * 
     * @param url
     * @return
     */
    private ArchiveDate getArchiveDates(String url) {
        ArchiveDate oldest = null;
        DocumentRetriever c = new DocumentRetriever();
        String archiveUrl = "http://web.archive.org/web/*/" + url;
        Document document = c.getWebDocument(archiveUrl);
        if (document != null) {
            ContentDateGetter cdg = new ContentDateGetter();
            cdg.setDocument(document);
            List<ContentDate> contentDates = cdg.getDates();
            contentDates = DateArrayHelper.filter(contentDates, DateArrayHelper.FILTER_FULL_DATE);
            DateComparator dc = new DateComparator();
            ContentDate cDate = dc.getOldestDate(contentDates);
            //oldest = DateConverter.convert(cDate, DateType.ArchiveDate);
            //oldest = new ArchiveDate(cDate);
            contentDates.remove(cDate);
            //oldest = DateConverter.convert(dc.getOldestDate(contentDates), DateType.ArchiveDate);
            oldest = new ArchiveDate(dc.getOldestDate(contentDates));
        }
        return oldest;
    }
}
