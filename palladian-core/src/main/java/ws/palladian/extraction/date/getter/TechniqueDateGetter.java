package ws.palladian.extraction.date.getter;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;

import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * Base implementation for a date extractor supplying {@link ExtractedDate}s. Subclasses can search for dates using
 * different techniques. <b>Implementation note:</b> Derived classes must at least implement {@link #getDates(Document)}
 * , which extracts dates from a W3C {@link Document}. The methods {@link #getDates(String)} and
 * {@link #getDates(HttpResult)} delegate to {@link #getDates(Document)} per default, but may be overridden, in case the
 * specific date extractor is intended to provide specific logic.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 * 
 * @param <T> Subtype of {@link ExtractedDate} which concrete technique implementations extract.
 */
public abstract class TechniqueDateGetter<T extends ExtractedDate> {

    /** Used for HTTP communication. */
    protected final HttpRetriever httpRetriever;

    /** Used for parsing HTML pages. */
    protected final DocumentParser htmlParser;

    public TechniqueDateGetter() {
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        htmlParser = ParserFactory.createHtmlParser();
    }

    /**
     * <p>
     * Extract dates using a URL as source.
     * </p>
     * 
     * @param url The URL from which to extract dates, not <code>null</code> or empty.
     * @return A {@link List} of extracted dates from the specified URL, or an empty List if no dates could be extracted
     *         or an error occurred. Never <code>null</code>.
     */
    public List<T> getDates(String url) {
        Validate.notEmpty(url, "url must not be empty");

        try {
            return getDates(httpRetriever.httpGet(url));
        } catch (HttpException e) {
            return Collections.emptyList();
        }
    }

    /**
     * <p>
     * Extract dates using an {@link HttpResult} as source.
     * </p>
     * 
     * @param httpResult The HttpResult from which to extract dates, not <code>null</code>.
     * @return A {@link List} of extracted dates from the specified URL, or an empty List if no dates could be extracted
     *         or an error occurred. Never <code>null</code>.
     */
    public List<T> getDates(HttpResult httpResult) {
        Validate.notNull(httpResult, "httpResult must not be null");

        try {
            return getDates(htmlParser.parse(httpResult));
        } catch (ParserException e) {
            return Collections.emptyList();
        }
    }

    /**
     * <p>
     * Extract dates using a {@link Document} as source.
     * </p>
     * 
     * @param document The Documnt from which to extract dates, not <code>null</code>.
     * @return A {@link List} of extracted dates from the specified URL, or an empty List if no dates could be extracted
     *         or an error occurred. Never <code>null</code>.
     */
    public abstract List<T> getDates(Document document);

    /**
     * <p>
     * Utility method to obtain a URL from a {@link Document}. Some {@link TechniqueDateGetter} implementations require
     * an URL, this method can be used to get the URL back from the Document. If no URL is assigned to the Document, an
     * {@link IllegalArgumentException} is thrown.
     * </p>
     * 
     * @param document The Document from which to retrieve the URL, not <code>null</code>.
     * @return The Document's URL.
     */
    protected static final String getUrl(Document document) {
        Validate.notNull(document, "document must not be null");
        String documentUrl = document.getDocumentURI();
        Validate.isTrue(documentUrl != null, "The document must supply its original URL (Document#getDocumentURI)");
        return documentUrl;
    }

}
