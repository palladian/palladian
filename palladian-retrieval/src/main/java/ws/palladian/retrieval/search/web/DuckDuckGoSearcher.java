package ws.palladian.retrieval.search.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.helper.FixedIntervalRequestThrottle;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.SearcherException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Web searcher scrapes HTML from <a href="http://duckduckgo.com/">DuckDuckGo</a> search results.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class DuckDuckGoSearcher extends AbstractSearcher<WebContent> {

    private static final String NAME = "DuckDuckGo";

    public static final class DuckDuckGoSearcherMetaInfo implements SearcherMetaInfo<DuckDuckGoSearcher, WebContent> {
        @Override
        public String getSearcherName() {
            return NAME;
        }

        @Override
        public String getSearcherId() {
            return "duck_duck_go";
        }

        @Override
        public Class<WebContent> getResultType() {
            return WebContent.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Collections.emptyList();
        }

        @Override
        public DuckDuckGoSearcher create(Map<ConfigurationOption<?>, ?> config) {
            return new DuckDuckGoSearcher();
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return null;
        }

        @Override
        public String getSearcherDescription() {
            return "Scrape search results from <a href=\"https://duckduckgo.com/\">DuckDuckGo</a>.";
        }
    }

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DuckDuckGoSearcher.class);

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /**
     * Prevent over penetrating the searcher.
     */
    private static final FixedIntervalRequestThrottle THROTTLE = new FixedIntervalRequestThrottle(1000);

    /**
     * The JavaScript URL for the search results.
     */
    // private static final String JS_URL = "https://duckduckgo.com/d.js?q=%s&t=A&l=us-en&p=1&s=%s";
    private static final String HTML_URL = "https://html.duckduckgo.com/html/?q=%s";

    /**
     * DuckDuckGo answers a client it does not like with {@code 202 Accepted} and an anti-bot "anomaly" page instead
     * of results, so anything but a plain 200 means "we were refused", not "no hits".
     */
    private static final int HTTP_OK = 200;

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {
        DocumentRetriever documentRetriever = new DocumentRetriever();

        String requestUrl = String.format(HTML_URL, UrlHelper.encodeParameter(query));
        LOGGER.debug("Request URL = {}", requestUrl);

        Document document;
        try {
            THROTTLE.hold();
            document = documentRetriever.getWebDocument(requestUrl);
        } catch (Exception e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + " (request URL: \"" + requestUrl + "\"): " + e.getMessage(), e);
        }

        if (document == null) {
            throw new SearcherException("No result page from " + getName() + " while searching for \"" + query + "\" (request URL: \"" + requestUrl + "\")");
        }
        int statusCode = getStatusCode(document);
        if ((statusCode != -1 && statusCode != HTTP_OK) || isAntiBotPage(document)) {
            throw new SearcherException(getName() + " refused the request and served its anti-bot page instead of results (HTTP status " + statusCode + ") while searching for \""
                    + query + "\" (request URL: \"" + requestUrl + "\")");
        }

        return search(document, resultCount);
    }

    public List<WebContent> search(Document document, int resultCount) throws SearcherException {
        List<WebContent> result = new ArrayList<>();
        if (document == null) {
            return result;
        }

        List<Node> resultBodies = XPathHelper.getXhtmlNodes(document, "//div[contains(@class,'result__body')]");

        for (Node node : resultBodies) {
            BasicWebContent.Builder builder = new BasicWebContent.Builder();
            builder.setSummary(XPathHelper.getXhtmlNodeTextContent(node, ".//a[contains(@class,'result__snippet')]"));
            builder.setTitle(StringHelper.trim(XPathHelper.getXhtmlNodeTextContent(node, ".//h2")));
            String url = XPathHelper.getXhtmlNodeTextContent(node, ".//h2/a/@href");
            builder.setUrl(url);
            if (!url.isEmpty() && !url.contains("?ad_domain=")) {
                result.add(builder.create());
            }
        }

        return CollectionHelper.getSublist(result, 0, resultCount);
    }

    /**
     * @return {@code true} if DuckDuckGo answered with the CAPTCHA-style "anomaly" page it serves to clients it does
     * not like (typically shared or datacenter egress IPs) rather than with results. A served result page never
     * carries that modal, so this does not hide a genuine "no hits" answer.
     */
    private static boolean isAntiBotPage(Document document) {
        return !XPathHelper.getXhtmlNodes(document, "//div[contains(@class,'anomaly-modal')]").isEmpty();
    }

    /** @return the HTTP status the result page came with, or -1 when it is not known. */
    private static int getStatusCode(Document document) {
        Object httpResult = document.getUserData(DocumentRetriever.HTTP_RESULT_KEY);
        return httpResult instanceof HttpResult ? ((HttpResult) httpResult).getStatusCode() : -1;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Gets the number of HTTP requests sent to DuckDuckGo.
     *
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }
}
