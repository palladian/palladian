package ws.palladian.retrieval.search.services;

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

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.XmlParser;
import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.WebResult;

/**
 * <p>
 * Base implementation for Hakia searcher.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class BaseHakiaSearcher extends BaseWebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BaseHakiaSearcher.class);

    private static final String DATE_PATTERN = "MM-dd-yyyy HH:mm:ss";

    private static final AtomicInteger requestCount = new AtomicInteger();

    private final String apiKey;

    private final XmlParser xmlParser;

    public BaseHakiaSearcher() {
        super();
        ConfigHolder configHolder = ConfigHolder.getInstance();
        PropertiesConfiguration config = configHolder.getConfig();
        this.apiKey = config.getString("api.hakia.key");
        xmlParser = new XmlParser();
    }

    public BaseHakiaSearcher(String apiKey) {
        super();
        this.apiKey = apiKey;
        xmlParser = new XmlParser();
    }

    @Override
    public List<WebResult> search(String query) {

        List<WebResult> webResults = new ArrayList<WebResult>();

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(getEndpoint());
        urlBuilder.append("&search.pid=").append(apiKey);
        urlBuilder.append("&search.query=").append(query);
        urlBuilder.append("&search.language=en");
        urlBuilder.append("&search.numberofresult=").append(getResultCount());

        // TODO need to set correct TimeZone?
        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

        try {

            HttpResult httpResult = retriever.httpGet(urlBuilder.toString());
            requestCount.incrementAndGet();
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
                    date = dateFormat.parse(dateNode.getTextContent());
                }

                WebResult webResult = new WebResult(url, title, summary, date);
                LOGGER.debug("hakia retrieved " + webResult);
                webResults.add(webResult);

                if (webResults.size() >= getResultCount()) {
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

    public static void main(String[] args) {
        Searcher<WebResult> searcher = new HakiaNewsSearcher();
        List<WebResult> result = searcher.search("apple");
        CollectionHelper.print(result);
    }

}
