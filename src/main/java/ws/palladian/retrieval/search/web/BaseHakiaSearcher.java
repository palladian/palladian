package ws.palladian.retrieval.search.web;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.XmlParser;

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
    public BaseHakiaSearcher(PropertiesConfiguration configuration) {
        this(configuration.getString("api.hakia.key"));
    }

    @Override
    public List<WebResult> search(String query, int resultCount, WebSearcherLanguage language) {

        List<WebResult> webResults = new ArrayList<WebResult>();

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(getEndpoint());
        urlBuilder.append("&search.pid=").append(apiKey);
        urlBuilder.append("&search.query=").append(query);
        urlBuilder.append("&search.language=en");
        urlBuilder.append("&search.numberofresult=").append(resultCount);

        // TODO need to set correct TimeZone?
        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

        try {

            HttpResult httpResult = retriever.httpGet(urlBuilder.toString());
            TOTAL_REQUEST_COUNT.incrementAndGet();
            Document resultDocument = xmlParser.parse(httpResult);

            List<Node> resultNodes = XPathHelper.getNodes(resultDocument, "//Result");

            for (Node resultNode : resultNodes) {

                String url = XPathHelper.getChildNode(resultNode, "Url").getTextContent();
                String title = XPathHelper.getChildNode(resultNode, "Title").getTextContent();
                String summary = XPathHelper.getChildNode(resultNode, "Paragraph").getTextContent();

                // date is only available for hakia news
                Node dateNode = XPathHelper.getChildNode(resultNode, "Date");
                Date date = null;
                if (dateNode != null) {
                    String dateString = dateNode.getTextContent();
                    date = dateFormat.parse(dateString);
                }

                WebResult webResult = new WebResult(url, title, summary, date);
                LOGGER.debug("hakia retrieved " + webResult);
                webResults.add(webResult);

                if (webResults.size() >= resultCount) {
                    break;
                }
            }

        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (DOMException e) {
            LOGGER.error(e);
        } catch (ParserException e) {
            LOGGER.error(e);
        } catch (ParseException e) {
            LOGGER.error(e);
        }
        return webResults;
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
