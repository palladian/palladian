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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebVideo;
import ws.palladian.retrieval.resources.WebVideo;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
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
public final class YouTubeSearcher extends AbstractMultifacetSearcher<WebVideo> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeSearcher.class);

    /** Name of this searcher. */
    private static final String SEARCHER_NAME = "YouTube";

    /** Key of the {@link Configuration} item which contains the API key. */
    public static final String CONFIG_API_KEY = "api.youtube.key";

    /** Counter for total number of requests sent to YouTube. */
    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /** The pattern for parsing the date. */
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /** The API key. */
    private final String apiKey;

    private final HttpRetriever retriever;

    /**
     * <p>
     * Create a new {@link YouTubeSearcher}.
     * </p>
     */
    public YouTubeSearcher() {
        this((String)null);
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
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Create a new {@link YouTubeSearcher}.
     * </p>
     * 
     * @param configuration The configuration which can provide an API key via key {@link #CONFIG_API_KEY}.
     */
    public YouTubeSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    private String getRequestUrl(MultifacetQuery query) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://gdata.youtube.com/feeds/api/videos?q=");
        urlBuilder.append(UrlHelper.encodeParameter(query.getText()));
        urlBuilder.append("&orderby=relevance");
        urlBuilder.append("&start-index=1");
        urlBuilder.append("&max-results=" + Math.min(50, query.getResultCount()));
        urlBuilder.append("&v=2");
        urlBuilder.append("&alt=json");
        if (apiKey != null && !apiKey.isEmpty()) {
            urlBuilder.append("&key=").append(apiKey);
        }
        Language language = query.getLanguage();
        if (language != null) {
            urlBuilder.append("&lr=").append(language.getIso6391());
        }
        // TODO geo search is currently not available.
        return urlBuilder.toString();
    }

    @Override
    public SearchResults<WebVideo> search(MultifacetQuery query) throws SearcherException {
        // TODO pagination available? Currently I get only 50 results max.

        String url = getRequestUrl(query);

        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(url);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName()
                    + " (request URL: \"" + url + "\"): " + e.getMessage(), e);
        }

        List<WebVideo> webResults = new ArrayList<WebVideo>();
        long numResults = 0;
        String jsonString = httpResult.getStringContent();

        try {
            JsonObject root = new JsonObject(jsonString);
            TOTAL_REQUEST_COUNT.incrementAndGet();
            JsonObject feed = root.getJsonObject("feed");
            numResults = feed.getJsonObject("openSearch$totalResults").getLong("$t");

            JsonArray entries = feed.getJsonArray("entry");

            if (entries != null) {

                for (int i = 0; i < entries.size(); i++) {
                    WebVideo webVideoResult = parseEntry(entries.getJsonObject(i));
                    webResults.add(webVideoResult);

                    if (webResults.size() >= query.getResultCount()) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                    + "\" with " + getName() + ", JSON was \"" + jsonString + "\": " + e, e);

        }
        return new SearchResults<WebVideo>(webResults, numResults);
    }

    private WebVideo parseEntry(JsonObject entry) throws JsonException {
        BasicWebVideo.Builder builder = new BasicWebVideo.Builder();
        String published = entry.queryString("published/$t");
        builder.setPublished(parseDate(published));
        builder.setTitle(entry.queryString("title/$t"));
        builder.setVideoUrl(entry.queryString("content/src"));
        builder.setUrl(getPageLink(entry));
        builder.setDuration(entry.queryLong("media$group/yt$duration/seconds"));
        builder.setViews(entry.queryInt("yt$statistics/viewCount"));
        builder.setSummary(entry.queryString("media$group/media$description/$t"));
        builder.setThumbnailUrl(entry.queryString("media$group/media$thumbnail[2]/url"));

        JsonObject ratingObject = entry.getJsonObject("yt$rating");
        if (ratingObject != null) {
            int numDislikes = ratingObject.getInt("numDislikes");
            int numLikes = ratingObject.getInt("numLikes");
            int total = numLikes + numDislikes;

            if (total > 0) {
                builder.setRating(numLikes / (double)total);
            }
        }

        if (entry.get("georss$where") != null) {
            String positionString = entry.queryString("georss$where/gml$Point/gml$pos/$t");
            if (positionString != null) {
                String[] longLat = positionString.split(" ");
                double lat = Double.valueOf(longLat[0]);
                double lng = Double.valueOf(longLat[1]);
                builder.setCoordinate(new ImmutableGeoCoordinate(lat, lng));
            }
        }

        return builder.create();
    }

    /**
     * <p>
     * Get the URL linking to the YouTube page where the video is shown.
     * </p>
     * 
     * @param entry
     * @return The URL, or <code>null</code> if no URL provided.
     * @throws JsonException
     */
    public String getPageLink(JsonObject entry) throws JsonException {
        JsonArray linkArray = entry.getJsonArray("link");
        for (int k = 0; k < linkArray.size(); k++) {
            JsonObject linkObject = linkArray.getJsonObject(k);
            String rel = linkObject.getString("rel");
            if (rel.equals("alternate")) {
                return linkObject.getString("href");
            }
        }
        return null;
    }

    private Date parseDate(String dateString) {
        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH);
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            LOGGER.error("Error parsing date " + dateString, e);
        }
        return null;
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
