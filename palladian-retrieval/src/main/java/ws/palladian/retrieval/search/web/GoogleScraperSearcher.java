package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
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

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleScraperSearcher.class);

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    private final DocumentParser parser;

    private final HttpRetriever httpRetriever;

    private static final String LINK_XPATH = "//div[@id='res']//li[@class='g' and not(./div/a/img)]//h3[@class='r']/a";
    private static final String INFORMATION_XPATH = "//div[@id='res']//li[@class='g']//span[@class='st']";
    
    /** Number of results returned on each page. */
    private static final int RESULTS_PER_PAGE = 10;

    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Google Scraping";
    
    public GoogleScraperSearcher() {
        parser = ParserFactory.createHtmlParser();
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        httpRetriever.setUserAgent("");
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebResult> result = new ArrayList<WebResult>();

        try {

            int numPages = (int)Math.ceil((double)resultCount / RESULTS_PER_PAGE);

            for (int page = 0; page <= numPages; page++) {

                String requestUrl = "http://www.google.com/search?hl=en&safe=off&output=search&start="
                        + RESULTS_PER_PAGE * page + "&q=" + UrlHelper.encodeParameter(query);
                LOGGER.debug("GET " + requestUrl);
                HttpResult httpResult = httpRetriever.httpGet(requestUrl);

                if (httpResult.getStatusCode() >= 500) {
                    throw new SearcherException("Google blocks the search requests");
                }

                Document document = parser.parse(httpResult);
                TOTAL_REQUEST_COUNT.incrementAndGet();
                
                List<WebResult> webResults = parseHtml(document);
                result.addAll(webResults);

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
    
    static List<WebResult> parseHtml(Document document) throws SearcherException {
        
        List<WebResult> result = CollectionHelper.newArrayList();
        
        List<Node> linkNodes = XPathHelper.getXhtmlNodes(document, LINK_XPATH);
        List<Node> infoNodes = XPathHelper.getXhtmlNodes(document, INFORMATION_XPATH);

        if (linkNodes.size() != infoNodes.size()) {
            throw new SearcherException(
                    "The returned document structure is not as expected, most likely the scraping implementation needs to be updated. (number of info items ["
                            + infoNodes.size() + "] should be equal to number of links [" + linkNodes.size() + "])");
        }

        Iterator<Node> linkIterator = linkNodes.iterator();
        Iterator<Node> infoIterator = infoNodes.iterator();

        while (linkIterator.hasNext()) {
            Node linkNode = linkIterator.next();
            Node infoNode = infoIterator.next();

            String url = linkNode.getAttributes().getNamedItem("href").getTextContent();
            
            // ignore Google internal links
            if (url.startsWith("/search")) {
                continue;
            }
            String extractedUrl = extractUrl(url);
            
            String title = linkNode.getTextContent();

            // the summary needs some cleaning; what we want is between "quotes",
            // we also remove double whitespaces
            String summary = infoNode.getTextContent();
            summary = StringHelper.trim(summary);
            summary = StringHelper.removeDoubleWhitespaces(summary);

            result.add(new WebResult(extractedUrl, title, summary, SEARCHER_NAME));
            
//            if (result.size() >= resultCount) {
//                break paging;
//            }
        }
        
        return result;
        
        
    }

    private static String extractUrl(String url) throws SearcherException {
        String originalUrl = StringHelper.getSubstringBetween(url, "q=", "&sa=");
        if (originalUrl.isEmpty()) {
            throw new SearcherException("Could not extract the original URL from " + url + "; probably the code needs to be updated.");
        }
        return UrlHelper.decodeParameter(originalUrl);
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
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
         List<String> urls = scroogleSearcher.searchUrls("capital germany", 11);
        // List<String> urls = scroogleSearcher.searchUrls("\"the population of germany is\"", 5);
        // List<String> urls = scroogleSearcher.searchUrls("\"eelee.com/sagem-puma-phone\"", 5);
//        List<String> urls = scroogleSearcher.searchUrls("\"http://eelee.com/htc-vivid\"", 5);
        CollectionHelper.print(urls);
    }
}
