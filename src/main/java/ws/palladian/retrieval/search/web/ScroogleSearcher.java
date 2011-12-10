package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.NekoHtmlParser;
import ws.palladian.retrieval.parser.ParserException;

// TODO currently, paging/result count is not supported
/**
 * <p>
 * Web searcher which scrapes content from Scroogle.
 * </p>
 * 
 * @author Eduardo Jacobo Miranda
 * @author Philipp Katz
 */
public final class ScroogleSearcher extends WebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ScroogleSearcher.class);

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    private final DocumentParser parser;

    public ScroogleSearcher() {
        super();
        parser = new NekoHtmlParser();
    }

    @Override
    public List<WebResult> search(String query, int resultCount, WebSearcherLanguage language) {

        List<WebResult> result = new ArrayList<WebResult>();

        try {

            String requestUrl = "http://www.scroogle.org/cgi-bin/nbbwssl.cgi?Gw=" + UrlHelper.urlEncode(query);
            HttpResult httpResult = retriever.httpGet(requestUrl);
            Document document = parser.parse(httpResult);
            TOTAL_REQUEST_COUNT.incrementAndGet();

            List<Node> linkNodes = XPathHelper.getXhtmlNodes(document, "//font/blockquote/a");
            List<Node> infoNodes = XPathHelper.getXhtmlNodes(document, "//font/blockquote/ul/font");

            if (linkNodes.size() != infoNodes.size()) {
                throw new IllegalStateException(
                        "The returned document structure is not as expected, probably the scraper needs to be updated");
            }

            Iterator<Node> linkIterator = linkNodes.iterator();
            Iterator<Node> infoIterator = infoNodes.iterator();

            while (linkIterator.hasNext()) {
                Node linkNode = linkIterator.next();
                Node infoNode = infoIterator.next();

                String url = linkNode.getAttributes().getNamedItem("href").getTextContent();
                String title = linkNode.getTextContent();

                // the summary needs some cleaning; what we want is between "quotes",
                // we also remove double whitespaces
                String summary = infoNode.getTextContent();
                summary = StringHelper.getSubstringBetween(summary, "\"", "\"");
                summary = StringHelper.removeDoubleWhitespaces(summary);

                WebResult webResult = new WebResult(url, title, summary);
                result.add(webResult);

            }

        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (ParserException e) {
            LOGGER.error(e);
        }

        return result;

    }

    @Override
    public String getName() {
        return "Scroogle";
    }

    /**
     * Gets the number of HTTP requests sent to Scroogle.
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }

}
