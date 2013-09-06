package ws.palladian.retrieval.search.videos;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.JsonObjectWrapper;
import ws.palladian.retrieval.search.SearcherException;
import ws.palladian.retrieval.search.web.WebSearcher;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeSearcher.class);

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
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://gdata.youtube.com/feeds/api/videos?q=");
        urlBuilder.append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&orderby=relevance");
        urlBuilder.append("&start-index=1");
        urlBuilder.append("&max-results=" + Math.min(50, resultCount));
        urlBuilder.append("&v=2");
        urlBuilder.append("&alt=json");
        if (apiKey != null && !apiKey.isEmpty()) {
            urlBuilder.append("&key=").append(apiKey);
        }
        if (language != null) {
            urlBuilder.append("&lr=").append(language.getIso6391());
        }
        return urlBuilder.toString();
    }

    @Override
    public List<WebVideoResult> search(String query, int resultCount, Language language) throws SearcherException {

        // TODO pagination available? Currently I get only 50 results max.

        String url = getRequestUrl(query, resultCount, language);

        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(url);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName()
                    + " (request URL: \"" + url + "\"): " + e.getMessage(), e);
        }

        List<WebVideoResult> webResults = new ArrayList<WebVideoResult>();
        String jsonString = httpResult.getStringContent();

        try {
            JsonObjectWrapper root = new JsonObjectWrapper(jsonString);
            TOTAL_REQUEST_COUNT.incrementAndGet();
            JsonObjectWrapper feed = root.getJSONObject("feed");

            JSONArray entries = feed.getJSONArray("entry");
            if (entries == null) {
                return webResults;
            }

            for (int i = 0; i < entries.length(); i++) {
                JsonObjectWrapper entry = new JsonObjectWrapper(entries.getJSONObject(i));
                String published = entry.get("published/$t", String.class);
                String title = entry.get("title/$t", String.class);
                String videoLink = entry.get("content/src", String.class);
                Date date = parseDate(published);
                String pageLink = getPageLink(entry.getJsonObject());
                Integer runtime = entry.get("media$group/yt$duration/seconds", Integer.class);
                Integer viewCount = entry.get("yt$statistics/viewCount", Integer.class);
                String description = entry.get("media$group/media$description/$t", String.class);
                String thumbnailUrl = entry.get("media$group/media$thumbnail[2]/url", String.class);

                JsonObjectWrapper ratingObject = entry.getJSONObject("yt$rating");
                Double rating = null;
                if (ratingObject != null) {
                    int numDislikes = ratingObject.getInt("numDislikes");
                    int numLikes = ratingObject.getInt("numLikes");
                    int total = numLikes + numDislikes;

                    if (total > 0) {
                        rating = numLikes / (double)total;
                    }
                }

                Long rtLong = null;
                if (runtime != null) {
                    rtLong = runtime.longValue();
                }

                WebVideoResult webResult = new WebVideoResult(pageLink, videoLink, title, description, rtLong, date);
                webResult.setViews(viewCount);
                webResult.setRating(rating);
                webResult.setThumbnail(thumbnailUrl);
                webResults.add(webResult);

                if (webResults.size() >= resultCount) {
                    break;
                }
            }

        } catch (Exception e) {
            throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                    + "\" with " + getName() + ", JSON was \"" + jsonString + "\": " + e, e);

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
    public long getTotalResultCount(String query, Language language) throws SearcherException {
        long hitCount = 0;
        try {
            HttpResult httpResult = retriever.httpGet(getRequestUrl(query, 1, language));
            JSONObject root = new JSONObject(httpResult.getStringContent());
            TOTAL_REQUEST_COUNT.incrementAndGet();

            hitCount = root.getJSONObject("feed").getJSONObject("openSearch$totalResults").getLong("$t");

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

    public static void main(String[] args) throws SearcherException {
        CollectionHelper.print(new YouTubeSearcher().search("Nokia Lumia 920", 5));
    }
}
