package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.FixedIntervalRequestThrottle;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Web searcher uses an unofficial JavaScript call to get <a href="http://duckduckgo.com/">DuckDuckGo</a> search
 * results.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class DuckDuckGoSearcher extends AbstractSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DuckDuckGoSearcher.class);

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /** The number of entries which are returned for each page. */
    private static final int ENTRIES_PER_PAGE = 10;

    /** Prevent over penetrating the searcher. */
    private static final FixedIntervalRequestThrottle THROTTLE = new FixedIntervalRequestThrottle(1000);

    /** The JavaScript URL for the search results. */
    private static final String URL = "https://duckduckgo.com/d.js?q=%s&t=A&l=us-en&p=1&s=%s";

    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebContent> result = new ArrayList<WebContent>();
        Set<String> urlDeduplication = new HashSet<String>();

        paging: for (int page = 0; page <= 999; page++) {

            String requestUrl = String.format(URL, UrlHelper.encodeParameter(query), ENTRIES_PER_PAGE * page);
            LOGGER.debug("Request URL = {}", requestUrl);

            HttpResult httpResult;
            try {
                THROTTLE.hold();
                httpResult = retriever.httpGet(requestUrl);
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName()
                        + " (request URL: \"" + requestUrl + "\"): " + e.getMessage(), e);
            }
            String content = httpResult.getStringContent();
            int indexOf = content.indexOf("[{\"a\":");
            if (indexOf < 0) {
                throw new SearcherException("Parse error while searching for \"" + query + "\" with " + getName()
                        + " (request URL: \"" + requestUrl + "\", result String: \"" + content + "\")");
            }
            String jsonContent = content.substring(indexOf);
            jsonContent = jsonContent.replace("}]);", "}]");
            TOTAL_REQUEST_COUNT.incrementAndGet();

            try {
                JsonArray jsonArray = new JsonArray(jsonContent);

                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject object = jsonArray.getJsonObject(i);

                    if (object.get("u") == null) {
                        continue; // if object in array contains no URL, it is paging information
                    }

                    if (!urlDeduplication.add(object.getString("u"))) {
                        break paging;
                    }
                    BasicWebContent.Builder builder = new BasicWebContent.Builder();
                    builder.setSummary(stripAndUnescape(object.getString("a")));
                    builder.setTitle(stripAndUnescape(object.getString("t")));
                    builder.setUrl(object.getString("u"));
                    result.add(builder.create());

                    if (result.size() >= resultCount) {
                        break paging;
                    }
                }
            } catch (JsonException e) {
                throw new SearcherException("Parse error while searching for \"" + query + "\" with " + getName()
                        + " (request URL: \"" + requestUrl + "\", result String: \"" + content + "\"): "
                        + e.getMessage(), e);
            }
        }

        return result;
    }

    @Override
    public String getName() {
        return "DuckDuckGo";
    }

    /**
     * <p>
     * Gets the number of HTTP requests sent to DuckDuckGo.
     * </p>
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }

    private static String stripAndUnescape(String html) {
        return HtmlHelper.stripHtmlTags(StringEscapeUtils.unescapeHtml4(html));
    }

}
