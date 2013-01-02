package ws.palladian.retrieval.search.web;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search news on <a href="http://newsseecr.com">NewsSeecr</a>
 * </p>
 * 
 * @author Philipp Katz
 */
public final class NewsSeecrSearcher extends WebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(NewsSeecrSearcher.class);

    private static final String SEARCHER_NAME = "NewsSeecr";

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final String BASE_URL = "http://newsseecr.com/api/news/search";
    // private static final String BASE_URL = "http://localhost:8080/api/news/search";

    private static final Map<String, String> NAMESPACE_MAPPING = Collections.singletonMap("atom",
            "http://www.w3.org/2005/Atom");

    private static final int RESULTS_PER_REQUEST = 100;

    private final DocumentParser xmlParser = ParserFactory.createXmlParser();

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebResult> webResults = CollectionHelper.newArrayList();

        for (int offset = 0; offset < Math.ceil((double)resultCount / RESULTS_PER_REQUEST); offset++) {

            HttpRequest request = new HttpRequest(HttpMethod.GET, BASE_URL);
            request.addParameter("query", query);
            request.addParameter("page", offset);
            request.addParameter("numResults", Math.min(resultCount, RESULTS_PER_REQUEST));
            LOGGER.trace("Performing request: " + request);
            HttpResult result;
            try {
                result = retriever.execute(request);
            } catch (HttpException e) {
                throw new SearcherException("Encountered HTTP error when executing the request: " + request + ": "
                        + e.getMessage(), e);
            }
            LOGGER.trace("XML result: " + HttpHelper.getStringContent(result));
            Document document;
            try {
                document = xmlParser.parse(result);
            } catch (ParserException e) {
                throw new SearcherException("Error when parsing the XML result from request: " + request + ", XML: \""
                        + HttpHelper.getStringContent(result) + "\": " + e.getMessage(), e);
            }

            List<Node> entryNodes = XPathHelper.getNodes(document, "//atom:entry", NAMESPACE_MAPPING);
            if (entryNodes.size() == 0) {
                break;
            }

            for (Node node : entryNodes) {
                String url = XPathHelper.getNode(node, "./atom:link/@href", NAMESPACE_MAPPING).getTextContent();
                String title = XPathHelper.getNode(node, "./atom:title", NAMESPACE_MAPPING).getTextContent();
                String summary = XPathHelper.getNode(node, "./atom:summary", NAMESPACE_MAPPING).getTextContent();
                String dateString = XPathHelper.getNode(node, "./atom:updated", NAMESPACE_MAPPING).getTextContent();
                Date date = parseDate(dateString);
                webResults.add(new WebResult(url, title, summary, date, SEARCHER_NAME));

                if (webResults.size() == resultCount) {
                    break;
                }
            }
        }

        return webResults;
    }

    private Date parseDate(String dateString) {
        DateFormat dateParser = new SimpleDateFormat(DATE_FORMAT);
        try {
            return dateParser.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static void main(String[] args) throws SearcherException {
        NewsSeecrSearcher searcher = new NewsSeecrSearcher();
        List<WebResult> results = searcher.search("obama", 250);
        CollectionHelper.print(results);
    }

}
