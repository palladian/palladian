package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * WebSearcher for <a href="http://www.youtube.com/">YouTube</a>.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class YouTubeSearcher extends WebSearcher<WebResult> {

    /** Key of the {@link Configuration} item which contains the API key. */
    private static final String CONFIG_API_KEY = "api.youtube.key";

    /** Counter for total number of requests sent to YouTube. */
    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /** The API key. */
    protected final String apiKey;

    /**
     * <p>
     * Create a new {@link YouTubeSearcher}.
     * </p>
     * 
     * @param apiKey (Optional) API key for accessing YouTube.
     */
    public YouTubeSearcher(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * <p>
     * Create a new {@link YouTubeSearcher}.
     * </p>
     * 
     * @param configuration The configuration which can provide an API key via key {@link #CONFIG_API_KEY}.
     */
    public YouTubeSearcher(Configuration configuration) {
        this.apiKey = configuration.getString(CONFIG_API_KEY);
    }

    @Override
    public String getName() {
        return "YouTube";
    }

    private String getRequestUrl(String query, int resultCount, Language language) {
        String url = "https://gdata.youtube.com/feeds/api/videos?q=" + UrlHelper.urlEncode(query);
        url += "&orderby=relevance";
        url += "&start-index=1";
        url += "&max-results=" + Math.min(50, resultCount);
        url += "&v=2";
        url += "&alt=json";
        if (apiKey != null && !apiKey.isEmpty()) {
            url += "&key=" + apiKey;
        }

        return url;
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        String url = getRequestUrl(query, resultCount, language);

        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(url);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName()
                    + " (request URL: \"" + url + "\"): " + e.getMessage(), e);
        }

        List<WebResult> webResults = new ArrayList<WebResult>();
        try {
            JSONObject root = new JSONObject(new String(httpResult.getContent()));
            TOTAL_REQUEST_COUNT.incrementAndGet();
            JSONObject feed = root.getJSONObject("feed");
            JSONArray entries = feed.getJSONArray("entry");

            for (int i = 0; i < entries.length(); i++) {

                JSONObject entry = entries.getJSONObject(i);
                String published = entry.getJSONObject("published").getString("$t");
                ExtractedDate date = DateGetterHelper.findDate(published);

                String title = entry.getJSONObject("title").getString("$t");
                String link = entry.getJSONObject("content").getString("src");

                WebResult webResult = new WebResult(link, title, "", date.getNormalizedDate());
                webResults.add(webResult);

                if (webResults.size() >= resultCount) {
                    break;
                }

            }
        } catch (JSONException e) {
            throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                    + "\" with " + getName() + ": " + e.getMessage(), e);

        }
        return webResults;
    }

    @Override
    public int getTotalResultCount(String query, Language language) throws SearcherException {
        int hitCount = 0;
        try {
            HttpResult httpResult = retriever.httpGet(getRequestUrl(query, 1, language));
            JSONObject root = new JSONObject(new String(httpResult.getContent()));
            TOTAL_REQUEST_COUNT.incrementAndGet();

            hitCount = root.getJSONObject("feed").getJSONObject("openSearch$totalResults").getInt("$t");

        } catch (JSONException e) {
            throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                    + "\" with " + getName() + ": " + e.getMessage(), e);
        } catch (HttpException e) {
            throw new SearcherException("HTTP exception while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        }
        return hitCount;
    }

    /**
     * <p>
     * Get the number of HTTP requests sent to YouTube.
     * </p>
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }
}
