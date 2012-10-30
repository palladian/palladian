package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Web searcher which scrapes content from Google.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class GoogleScraperSearcher extends WebSearcher<WebResult> {

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    private final DocumentParser parser;

    private static final String LINK_XPATH = "//h3[@class='r']/a[@class='l']";
    private static final String INFORMATION_XPATH = "//span[@class='st']";

    public GoogleScraperSearcher() {
        super();
        parser = ParserFactory.createHtmlParser();
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebResult> result = new ArrayList<WebResult>();

        try {

            int entriesPerPage = 10;
            int numPages = resultCount / entriesPerPage;

            paging: for (int page = 0; page <= numPages; page++) {

                String requestUrl = "http://www.google.com/search?hl=en&safe=off&output=search&start="
                        + entriesPerPage * page + "&q=" + UrlHelper.urlEncode(query);
                HttpResult httpResult = retriever.httpGet(requestUrl);
                Document document = parser.parse(httpResult);
                TOTAL_REQUEST_COUNT.incrementAndGet();

                List<Node> linkNodes = XPathHelper.getXhtmlNodes(document, LINK_XPATH);
                List<Node> infoNodes = XPathHelper.getXhtmlNodes(document, INFORMATION_XPATH);

                if (linkNodes.size() != infoNodes.size()) {
                    throw new SearcherException(
                            "The returned document structure is not as expected, most likely the scraping implementation needs to be updated. (number of info items should be equal to number of links)");
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
                    summary = StringHelper.trim(summary);

                    WebResult webResult = new WebResult(url, title, summary, getName());
                    result.add(webResult);

                    if (result.size() >= resultCount) {
                        break paging;
                    }
                }

            }

        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        } catch (ParserException e) {
            throw new SearcherException("Error parsing the HTML response while searching for \"" + query + "\" with "
                    + getName() + ": " + e.getMessage(), e);
        }

        return result;

    }

    @Override
    public String getName() {
        return "Google Scraping";
    }

    /**
     * <p>
     * Gets the number of HTTP requests sent to Scroogle.
     * </p>
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }

    public static void main(String[] args) throws SearcherException {
        GoogleScraperSearcher scroogleSearcher = new GoogleScraperSearcher();
        // List<String> urls = scroogleSearcher.searchUrls("capital germany", 11);
        // List<String> urls = scroogleSearcher.searchUrls("\"the population of germany is\"", 5);
        // List<String> urls = scroogleSearcher.searchUrls("\"eelee.com/sagem-puma-phone\"", 5);
        List<String> urls = scroogleSearcher.searchUrls("\"eelee.com/wnd-wind-duo-2200-reviews\"", 5);
        CollectionHelper.print(urls);
    }
}
