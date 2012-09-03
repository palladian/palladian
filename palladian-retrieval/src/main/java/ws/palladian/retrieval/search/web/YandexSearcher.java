package ws.palladian.retrieval.search.web;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * {@link WebSearcher} for Russian search engine <a href="http://www.yandex.com/">Yandex</a>. To use Yandex, you need to
 * sign up for the service (see link below). Without verifying one's phone number, one can perform only 10 requests/day.
 * With a verified phone number, 1.000 requests/day are allowed.
 * </p>
 * 
 * @see <a href="http://help.yandex.com/xml/faq.xml?id=1116498">Necessary steps for sign up</a>
 * @see <a href="http://help.yandex.com/xml/?id=1116467">API documentation</a>
 * @author Philipp Katz
 */
public final class YandexSearcher extends WebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(YandexSearcher.class);

    /** Counter for the total number of HTTP requests sent to Yandex. */
    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();
    /** The name of this search engine. */
    private static final String SEARCHER_NAME = "Yandex";
    /** The pattern used for parsing the returned date strings. */
    private static final String DATE_PATTERN = "yyyyMMdd'T'HHmmss";
    /** The maximum count of results delivered with each request. */
    private static final int MAX_RESULTS_PER_PAGE = 100;
    /** The pattern of a valid search URL, containing user and key parameter. */
    private static final String SEARCH_URL_PATTERN = "http://xmlsearch.yandex.ru/xmlsearch\\?user=.+&key=.+";

    /** Key of the {@link Configuration} item which contains the custom search URL. */
    public static final String CONFIG_SEARCH_URL = "api.yandex.url";

    /** The API endpoint for accessing the searcher. */
    private final String yandexSearchUrl;
    /** The parser used for processing the returned XML data. */
    private final DocumentParser xmlParser;

    /**
     * <p>
     * Create a new {@link YandexSearcher} with the specified search URL, which can be obtained from <a
     * href="http://xml.yandex.ru/">here</a>. The search URL is account specific and can be obtained from the Yandex web
     * page. Keep in mind, that before searching, you must activate your IP at the web interface, elsewise Yandex
     * refuses to perform searches.
     * </p>
     * 
     * @param yandexSearchUrl The necessary endpoint URL from Yandex, not <code>null</code> or empty.
     */
    public YandexSearcher(String yandexSearchUrl) {
        checkSearchUrlValidity(yandexSearchUrl);
        this.yandexSearchUrl = yandexSearchUrl;
        this.xmlParser = ParserFactory.createXmlParser();
    }

    /**
     * <p>
     * Create a new {@link YandexSearcher} with the search URL specified in the {@link Configuration} as string via
     * {@value #CONFIG_SEARCH_URL}, which can be obtained from <a href="http://xml.yandex.ru/">here</a>. The search URL
     * is account specific and can be obtained from the Yandex web page. Keep in mind, that before searching, you must
     * activate your IP at the web interface, elsewise Yandex refuses to perform searches.
     * </p>
     * 
     * @param configuration The configuration which must provide an individual search URL for accessing Yandex as a
     *            string via key {@value #CONFIG_SEARCH_URL} in the configuration.
     */
    public YandexSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_SEARCH_URL));
    }

    /** Constructor only used for unit testing. */
    YandexSearcher() {
        this.yandexSearchUrl = null;
        this.xmlParser = ParserFactory.createXmlParser();
    }

    /**
     * <p>
     * Check the validity of the supplied search URL, throw an {@link IllegalArgumentException} if URL is not valid.
     * Package private to allow unit testing.
     * </p>
     * 
     * @param yandexSearchUrl
     * @throws IllegalArgumentException When provided URL is not valid, empty or <code>null</code>.
     */
    void checkSearchUrlValidity(String yandexSearchUrl) {
        if (yandexSearchUrl == null || yandexSearchUrl.isEmpty()) {
            throw new IllegalArgumentException("Search URL must be supplied.");
        }
        if (!yandexSearchUrl.matches(SEARCH_URL_PATTERN)) {
            throw new IllegalArgumentException(
                    "The supplied search URL is invalid. It must start with \"http://xmlsearch.yandex.ru/xmlsearch\" and contain a valid user and key parameter.");
        }
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        int necessaryPages = (int)Math.ceil((double)resultCount / MAX_RESULTS_PER_PAGE);
        int pageSize = Math.min(MAX_RESULTS_PER_PAGE, resultCount);
        List<WebResult> results = new ArrayList<WebResult>();

        for (int page = 0; page < necessaryPages; page++) {

            String requestUrl = buildRequestUrl(yandexSearchUrl, query, pageSize, page);
            LOGGER.debug("request URL: " + requestUrl);

            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(requestUrl);
                TOTAL_REQUEST_COUNT.incrementAndGet();
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                        + e.getMessage(), e);
            }
            try {
                Document document = xmlParser.parse(httpResult);
                List<WebResult> currentResults = parse(document);
                if (currentResults.isEmpty()) {
                    // we did not get any more results
                    break;
                }
                results.addAll(currentResults);
            } catch (ParserException e) {
                throw new SearcherException("Error parsing the XML response for query \"" + query + "\" with "
                        + getName() + " (request url: \"" + requestUrl + "\"): " + e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * <p>
     * Parse the supplied response document and return a list of {@link WebResult}s extracted from the document. In
     * case, the document contains an error message, throw a {@link SearcherException}. Package private to allow unit
     * testing.
     * </p>
     * 
     * @param document
     * @return
     * @throws SearcherException
     */
    List<WebResult> parse(Document document) throws SearcherException {
        Node responseNode = XPathHelper.getNode(document, "/yandexsearch/response");

        if (responseNode == null) {
            throw new SearcherException("The response data could not be parsed. Maybe the API has changed.");
        }

        // check, if we got an error response
        checkError(responseNode);

        List<Node> resultDocs = XPathHelper.getNodes(responseNode, "results/grouping/group/doc");
        List<WebResult> result = new ArrayList<WebResult>();
        for (Node resultDoc : resultDocs) {
            // required
            Node urlNode = XPathHelper.getChildNode(resultDoc, "url");
            Node titleNode = XPathHelper.getChildNode(resultDoc, "title");
            if (urlNode == null || titleNode == null) {
                throw new SearcherException("Expected element (url or title) was missing");
            }
            String url = urlNode.getTextContent();
            String title = titleNode.getTextContent();
            // optional
            Node headlineNode = XPathHelper.getChildNode(resultDoc, "headline");
            String headline = headlineNode == null ? null : headlineNode.getTextContent();
            Node timeNode = XPathHelper.getChildNode(resultDoc, "modtime");
            Date date = timeNode == null ? null : parseDate(timeNode.getTextContent());

            WebResult webResult = new WebResult(url, title, headline, date, getName());
            result.add(webResult);
        }
        return result;
    }

    /**
     * <p>
     * Check if we received an error response. The error codes are described <a
     * href="http://help.yandex.com/xml/?id=1116470">here</a>. If we receive an error code, throw a
     * {@link SearcherException}, except when error code is 15, which means "no results for query", which we do not
     * consider as real error.
     * </p>
     * 
     * @param responseNode
     * @throws SearcherException
     */
    void checkError(Node responseNode) throws SearcherException {
        Node errorNode = XPathHelper.getChildNode(responseNode, "error");
        if (errorNode != null) {
            Node errorAttribute = errorNode.getAttributes().getNamedItem("code");
            if (errorAttribute != null) {
                String errorCode = errorAttribute.getNodeValue();
                if (!"15".equals(errorCode)) {
                    throw new SearcherException(
                            "Encountered error (code "
                                    + errorCode
                                    + "). See \"http://help.yandex.com/xml/?id=1116470\" for a list of errors and their meanings.");
                }
            } else {
                throw new SearcherException("Encountered error (unspecified)");
            }
        }
    }

    /**
     * <p>
     * Parse the given date string using the {@link #DATE_PATTERN}.
     * </p>
     * 
     * @param dateString
     * @return The parsed date, or <code>null</code> when date could not be parsed.
     */
    Date parseDate(String dateString) {
        Date ret = null;
        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        try {
            ret = dateFormat.parse(dateString);
        } catch (ParseException e) {
            LOGGER.warn("Error parsing date \"" + dateString + "\" using pattern \"" + DATE_PATTERN + "\"");
        }
        return ret;
    }

    /**
     * <p>
     * Build the request URL for yandex. See <a href="http://help.yandex.com/xml/?id=1116461">here</a> for more
     * information.
     * </p>
     * 
     * @param query The query to search for (will be encoded by this method, no pre-escaping necessary.
     * @param count The number of items to retrieve.
     * @param page The page offset, 0 means no offset.
     * @return
     */
    String buildRequestUrl(String searchUrl, String query, int count, int page) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(searchUrl);
        urlBuilder.append("&query=").append(UrlHelper.urlEncode(query));
        if (page > 0) {
            urlBuilder.append("&page=").append(page);
        }
        urlBuilder.append("&groupby=groups-on-page%3D").append(count).append("docs-in-group%3D1");
        urlBuilder.append("&filter=none");
        return urlBuilder.toString();
    }

    /**
     * <p>
     * Gets the number of HTTP requests sent to Yandex.
     * </p>
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }

}
