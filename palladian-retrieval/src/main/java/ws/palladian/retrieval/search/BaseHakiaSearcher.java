package ws.palladian.retrieval.search;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;

/**
 * <p>
 * Base implementation for Hakia searcher.
 * </p>
 * 
 * @see <a href="http://blog.hakia.com/?p=312">hakia Semantic Search – Now Available for Syndication [...]</a>
 * @author Philipp Katz
 */
public abstract class BaseHakiaSearcher extends AbstractSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseHakiaSearcher.class);

    /** Key of the {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.hakia.key";

    private static final String DATE_PATTERN = "MM-dd-yyyy HH:mm:ss";

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    private final String apiKey;

    private final DocumentParser xmlParser;
    
    private final HttpRetriever retriever;

    /**
     * <p>
     * Creates a new Hakia searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing Hakia, not <code>null</code> or empty.
     */
    public BaseHakiaSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        xmlParser = ParserFactory.createXmlParser();
        retriever = HttpRetrieverFactory.getHttpRetriever(); 
    }

    /**
     * <p>
     * Creates a new Hakia searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing Hakia, which must be provided
     *            as string via key {@value #CONFIG_API_KEY} in the configuration.
     */
    public BaseHakiaSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {

        String requestUrl = buildRequestUrl(query, resultCount);
        LOGGER.debug("Requesting {}", requestUrl);
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

        checkError(resultDocument);

        return extractWebResults(resultDocument, resultCount);
    }

    /**
     * Check, if an error occurred, and throw a {@link SearcherException} if applicable.
     * 
     * @param resultDocument The parsed result from Hakia.
     * @throws SearcherException In case the result returned an error.
     */
    private void checkError(Document resultDocument) throws SearcherException {
        Node errorNumNode = XPathHelper.getNode(resultDocument, "//Error/Num");
        if (errorNumNode == null) {
            // error num should be given in either case, also if request went fine.
            throw new SearcherException("Unexpected result");
        }
        if (errorNumNode.getTextContent().equals("0")) {
            // error num 0 means okay.
            return;
        }
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append("An error occured while searching with " + getName()).append(':');
        Node descNode = XPathHelper.getNode(resultDocument, "//Error/Desc");
        if (descNode != null) {
            errorMsg.append(' ').append(descNode.getTextContent());
        }
        errorMsg.append(" (").append(errorNumNode.getTextContent()).append(')');
        throw new SearcherException(errorMsg.toString());
    }

    /**
     * @param resultDocument
     * @param resultCount
     * @return
     * @throws SearcherException
     */
    private List<WebContent> extractWebResults(Document resultDocument, int resultCount) throws SearcherException {
        // TODO need to set correct TimeZone?
        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        List<WebContent> webResults = new ArrayList<WebContent>();
        List<Node> resultNodes = XPathHelper.getNodes(resultDocument, "//Result");

        for (Node resultNode : resultNodes) {

            BasicWebContent.Builder builder = new BasicWebContent.Builder();
            builder.setUrl(XPathHelper.getNode(resultNode, "Url").getTextContent());
            builder.setTitle(XPathHelper.getNode(resultNode, "Title").getTextContent());
            builder.setSummary(XPathHelper.getNode(resultNode, "Paragraph").getTextContent());

            // date is only available for hakia news
            Node dateNode = XPathHelper.getNode(resultNode, "Date");
            if (dateNode != null) {
                String dateString = dateNode.getTextContent();
                try {
                    Date date = dateFormat.parse(dateString);
                    builder.setPublished(date);
                } catch (ParseException e) {
                    throw new SearcherException("Error parsing the search result's date (" + dateString + ") at "
                            + getName() + ": " + e.getMessage(), e);
                }
            }

            WebContent webResult = builder.create();
            LOGGER.debug("hakia retrieved {}", webResult);
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
