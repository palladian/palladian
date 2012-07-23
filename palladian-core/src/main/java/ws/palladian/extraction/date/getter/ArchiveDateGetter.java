package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.date.dates.ArchiveDate;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * This class uses "The Internet Wayback Machine" aka. <a href="http://archive.org">archive.org</a> to determine the
 * date of a page.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class ArchiveDateGetter extends TechniqueDateGetter<ArchiveDate> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ArchiveDateGetter.class);
    /** Used for HTTP communication. */
    private final HttpRetriever httpRetriever;
    /** Used for parsing HTML pages. */
    private final DocumentParser htmlParser;

    public ArchiveDateGetter() {
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        htmlParser = ParserFactory.createHtmlParser();
    }

    @Override
    public List<ArchiveDate> getDates() {
        List<ArchiveDate> result = new ArrayList<ArchiveDate>();
        if (url != null) {
            result.add(getArchiveDates(url));
        }
        return result;
    }

    private ArchiveDate getArchiveDates(String url) {
        ArchiveDate oldest = null;
        String archiveUrl = "http://web.archive.org/web/*/" + url;

        try {
            HttpResult httpResult = httpRetriever.httpGet(archiveUrl);
            Document document = htmlParser.parse(httpResult);

            ContentDateGetter contentDateGetter = new ContentDateGetter();
            contentDateGetter.setDocument(document);
            List<ContentDate> contentDates = contentDateGetter.getDates();

            contentDates = DateArrayHelper.filter(contentDates, DateArrayHelper.FILTER_FULL_DATE);
            DateComparator dateComparator = new DateComparator();

            oldest = new ArchiveDate(dateComparator.getOldestDate(contentDates));

        } catch (HttpException e) {
            LOGGER.error("HttpException while getting date for \"" + url + "\": " + e.getMessage(), e);
        } catch (ParserException e) {
            LOGGER.error("ParseException while getting date for \"" + url + "\": " + e.getMessage(), e);
        }

        return oldest;
    }
}
