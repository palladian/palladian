package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.dates.ArchiveDate;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.ParserException;

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

    /** The base URL of the archive web page. */
    private static final String ARCHIVE_BASE_URL = "http://web.archive.org/web/*/";

    @Override
    public List<ArchiveDate> getDates(String url) {
        List<ArchiveDate> result = new ArrayList<ArchiveDate>();
        String archiveUrl = ARCHIVE_BASE_URL + url;

        try {

            HttpResult httpResult = httpRetriever.httpGet(archiveUrl);
            Document document = htmlParser.parse(httpResult);

            ContentDateGetter contentDateGetter = new ContentDateGetter();
            List<ContentDate> contentDates = contentDateGetter.getDates(document);

            contentDates = DateArrayHelper.filterFullDate(contentDates);
            DateComparator dateComparator = new DateComparator();

            result.add(new ArchiveDate(dateComparator.getOldestDate(contentDates)));

        } catch (HttpException e) {
            LOGGER.error("HttpException while getting date for \"" + url + "\": " + e.getMessage(), e);
        } catch (ParserException e) {
            LOGGER.error("ParseException while getting date for \"" + url + "\": " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public List<ArchiveDate> getDates(HttpResult httpResult) {
        return getDates(httpResult.getUrl());
    }

    @Override
    public List<ArchiveDate> getDates(Document document) {
        return getDates(getUrl(document));
    }
}
