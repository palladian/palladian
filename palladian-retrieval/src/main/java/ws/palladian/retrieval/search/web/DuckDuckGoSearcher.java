package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.RequestThrottle;
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

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /** The number of entries which are returned for each page. */
    private static final int ENTRIES_PER_PAGE = 10;

    /** Prevent over penetrating the searcher. */
    private static final RequestThrottle THROTTLE = new RequestThrottle(1000);
    
    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebContent> result = new ArrayList<WebContent>();
        Set<String> urlDeduplication = new HashSet<String>();

        paging: for (int page = 0; page <= 999; page++) {

            String requestUrl = "http://duckduckgo.com/d.js?l=us-en&p=1&s=" + ENTRIES_PER_PAGE * page + "&q="
                    + UrlHelper.encodeParameter(query);

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
                JSONArray jsonArray = new JSONArray(jsonContent);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject object = jsonArray.getJSONObject(i);

                    if (!object.has("u")) {
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
            } catch (JSONException e) {
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
