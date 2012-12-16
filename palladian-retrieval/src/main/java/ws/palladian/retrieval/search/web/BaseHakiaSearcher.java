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
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.XmlParser;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Base implementation for Hakia searcher.
 * </p>
 * 
 * @see http://blog.hakia.com/?p=312
 * @author Philipp Katz
 */
abstract class BaseHakiaSearcher extends WebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BaseHakiaSearcher.class);

    /** Key of the {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.hakia.key";

    private static final String DATE_PATTERN = "MM-dd-yyyy HH:mm:ss";

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    private final String apiKey;

    private final XmlParser xmlParser;

    /**
     * <p>
     * Creates a new Hakia searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing Hakia.
     */
    public BaseHakiaSearcher(String apiKey) {
        super();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("The required API key is missing");
        }
        this.apiKey = apiKey;
        xmlParser = new XmlParser();
    }

    /**
     * <p>
     * Creates a new Hakia searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing Hakia, which must be provided
     *            as string via key <tt>api.hakia.key</tt> in the configuration.
     */
    public BaseHakiaSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        String requestUrl = buildRequestUrl(query, resultCount);
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName()
                    + "(request url: \"" + requestUrl + "\"): " + e.getMessage(), e);
        }

        TOTAL_REQUEST_COUNT.incrementAndGet();
        Document resultDocument;
        try {
            resultDocument = xmlParser.parse(httpResult);
        } catch (ParserException e) {
            throw new SearcherException("Error parsing the XML response for query \"" + query + "\" with " + getName()
                    + "(request url: \"" + requestUrl + "\"): " + e.getMessage(), e);
        }

        return extractWebResults(resultDocument, resultCount);
    }

    /**
     * @param resultDocument
     * @param resultCount
     * @return
     * @throws SearcherException
     */
    private List<WebResult> extractWebResults(Document resultDocument, int resultCount) throws SearcherException {
        // TODO need to set correct TimeZone?
        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        List<WebResult> webResults = new ArrayList<WebResult>();
        List<Node> resultNodes = XPathHelper.getNodes(resultDocument, "//Result");

        for (Node resultNode : resultNodes) {

            String url = XPathHelper.getNode(resultNode, "Url").getTextContent();
            String title = XPathHelper.getNode(resultNode, "Title").getTextContent();
            String summary = XPathHelper.getNode(resultNode, "Paragraph").getTextContent();

            // date is only available for hakia news
            Node dateNode = XPathHelper.getNode(resultNode, "Date");
            Date date = null;
            if (dateNode != null) {
                String dateString = dateNode.getTextContent();
                try {
                    date = dateFormat.parse(dateString);
                } catch (ParseException e) {
                    throw new SearcherException("Error parsing the search result's date (" + dateString + ") at "
                            + getName() + ": " + e.getMessage(), e);
                }
            }

            WebResult webResult = new WebResult(url, title, summary, date);
            LOGGER.debug("hakia retrieved " + webResult);
            webResults.add(webResult);

            if (webResults.size() >= resultCount) {
                break;
            }
        }
        return webResults;
    }

    /**
     * @param query
     * @param resultCount
     * @return
     */
    private String buildRequestUrl(String query, int resultCount) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(getEndpoint());
        urlBuilder.append("&search.pid=").append(apiKey);
        urlBuilder.append("&search.query=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&search.language=en");
        urlBuilder.append("&search.numberofresult=").append(resultCount);
        return urlBuilder.toString();
    }

    protected abstract String getEndpoint();

    /**
     * Gets the number of HTTP requests sent to Hakia.
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }

}
