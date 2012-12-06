package ws.palladian.retrieval.search.web;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * WebSearcher for <a href="http://www.youtube.com/">YouTube</a>.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @see <a href="https://developers.google.com/youtube/2.0/developers_guide_protocol">API documentation</a>
 */
public final class YouTubeSearcher extends WebSearcher<WebVideoResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(YouTubeSearcher.class);

    /** Key of the {@link Configuration} item which contains the API key. */
    public static final String CONFIG_API_KEY = "api.youtube.key";

    /** Counter for total number of requests sent to YouTube. */
    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /** The pattern for parsing the date. */
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /** The API key. */
    private final String apiKey;

    /**
     * <p>
     * Create a new {@link YouTubeSearcher}.
     * </p>
     */
    public YouTubeSearcher() {
        this.apiKey = null;
    }

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
        String url = "https://gdata.youtube.com/feeds/api/videos?q=" + UrlHelper.encodeParameter(query);
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
    public List<WebVideoResult> search(String query, int resultCount, Language language) throws SearcherException {

        // TODO pagination available? Currenty I get only 50 results max.

        String url = getRequestUrl(query, resultCount, language);

        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(url);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName()
                    + " (request URL: \"" + url + "\"): " + e.getMessage(), e);
        }

        List<WebVideoResult> webResults = new ArrayList<WebVideoResult>();
        try {
            JSONObject root = new JSONObject(HttpHelper.getStringContent(httpResult));
            TOTAL_REQUEST_COUNT.incrementAndGet();
            JSONObject feed = root.getJSONObject("feed");

            if (feed.has("entry")) {

                JSONArray entries = feed.getJSONArray("entry");

                for (int i = 0; i < entries.length(); i++) {

                    JSONObject entry = entries.getJSONObject(i);
                    String published = entry.getJSONObject("published").getString("$t");

                    String title = entry.getJSONObject("title").getString("$t");
                    String videoLink = entry.getJSONObject("content").getString("src");
                    Date date = parseDate(published);
                    String pageLink = getPageLink(entry);

                    WebVideoResult webResult = new WebVideoResult(pageLink, videoLink, title, null, date);
                    webResults.add(webResult);

                    if (webResults.size() >= resultCount) {
                        break;
                    }

                }

            }
        } catch (JSONException e) {
            throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                    + "\" with " + getName() + ": " + e.getMessage(), e);

        }
        return webResults;
    }

    /**
     * <p>
     * Get the URL linking to the YouTube page where the video is shown.
     * </p>
     * 
     * @param entry
     * @return The URL, or <code>null</code> if no URL provided.
     * @throws JSONException
     */
    public String getPageLink(JSONObject entry) throws JSONException {
        JSONArray linkArray = entry.getJSONArray("link");
        for (int k = 0; k < linkArray.length(); k++) {
            JSONObject linkObject = linkArray.getJSONObject(k);
            String rel = linkObject.getString("rel");
            if (rel.equals("alternate")) {
                return linkObject.getString("href");
            }
        }
        return null;
    }

    private Date parseDate(String dateString) {
        Date date = null;
        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH);
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            LOGGER.error("Error parsing date " + dateString, e);
        }
        return date;
    }

    @Override
    public int getTotalResultCount(String query, Language language) throws SearcherException {
        int hitCount = 0;
        try {
            HttpResult httpResult = retriever.httpGet(getRequestUrl(query, 1, language));
            JSONObject root = new JSONObject(HttpHelper.getStringContent(httpResult));
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
