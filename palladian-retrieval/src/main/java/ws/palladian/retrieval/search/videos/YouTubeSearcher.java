package ws.palladian.retrieval.search.videos;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
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
import ws.palladian.retrieval.search.Facet;
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
 * @see <a href="https://developers.google.com/youtube/v3/docs/">API documentation</a>
 * @see <a href="https://developers.google.com/youtube/v3/docs/search/list">API Query Parameters</a>
 * @see <a href="https://developers.google.com/youtube/v3/docs/videos">Videos resource</a>
 */
public final class YouTubeSearcher extends AbstractMultifacetSearcher<WebVideo> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeSearcher.class);

    /** Name of this searcher. */
    public static final String SEARCHER_NAME = "YouTube";

    /** Key of the {@link Configuration} item which contains the API key. */
    public static final String CONFIG_API_KEY = "api.youtube.key";

    /** The pattern for parsing the date. */
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /** The maximum number of results we can get per request. */
    private static final int MAX_RESULTS_PER_PAGE = 50;

    /** The pattern for parsing durations, such as "PT15M51S". */
    private static final Pattern DURATION_PATTERN = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?");

    /**
     * <p>
     * Order parameter for result list.
     * </p>
     * 
     * @author pk
     */
    public static enum OrderBy implements Facet {
        /** Resources are sorted in reverse chronological order based on the date they were created. */
        DATE("date"),
        /** Resources are sorted from highest to lowest rating. */
        RATING("rating"),
        /**
         * Resources are sorted based on their relevance to the search query. This is the default value for this
         * parameter.
         */
        RELEVANCE("relevance"),
        /** Resources are sorted alphabetically by title. */
        TITLE("title"),
        /** Channels are sorted in descending order of their number of uploaded videos. */
        VIDEO_COUNT("videoCount"),
        /** Resources are sorted from highest to lowest number of views. */
        VIEW_COUNT("viewCount"),
        /** @deprecated Use {@link #DATE} instead. */
        @Deprecated
        PUBLISHED("date");

        private static final String YOUTUBE_RESULT_ORDER = "youtube.resultOrder";

        private final String parameterValue;

        private OrderBy(String parameterValue) {
            this.parameterValue = parameterValue;
        }

        @Override
        public String getIdentifier() {
            return YOUTUBE_RESULT_ORDER;
        }

        public String getValue() {
            return parameterValue;
        }

    }

    /** The API key. */
    private final String apiKey;

    private final HttpRetriever retriever;

    /**
     * <p>
     * Create a new {@link YouTubeSearcher}.
     * </p>
     * 
     * @param apiKey API key for accessing YouTube.
     */
    public YouTubeSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
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

    @Override
    public SearchResults<WebVideo> search(MultifacetQuery query) throws SearcherException {
        List<WebVideo> webResults = CollectionHelper.newArrayList();
        Long numResults = null;
        String url = null;
        String jsonString = null;
        int numRequests = 0;
        try {
            List<String> videoIds = CollectionHelper.newArrayList();
            String nextPageToken = null;
            // retrieve the video IDs matching the search
            while (videoIds.size() < query.getResultCount()) {
                url = buildSearchUrl(query, nextPageToken, MAX_RESULTS_PER_PAGE);
                LOGGER.debug("Requesting URL {}", url);
                HttpResult httpResult = retriever.httpGet(url);
                numRequests++;
                checkForHttpError(httpResult);
                jsonString = httpResult.getStringContent();
                JsonObject root = new JsonObject(jsonString);
                numResults = root.queryLong("pageInfo/totalResults");
                nextPageToken = root.tryQueryString("nextPageToken");
                JsonArray entries = root.getJsonArray("items");
                if (entries != null) {
                    for (int i = 0; i < entries.size() && videoIds.size() < query.getResultCount(); i++) {
                        videoIds.add(entries.getJsonObject(i).queryString("id/videoId"));
                    }
                }
                if (nextPageToken == null) {
                    break;
                }
            }
            // retrieve data about the found video IDs
            for (int i = 0; i < videoIds.size(); i += MAX_RESULTS_PER_PAGE) {
                List<String> currentChunk = videoIds.subList(i, Math.min(i + MAX_RESULTS_PER_PAGE, videoIds.size()));
                url = buildListUrl(currentChunk);
                LOGGER.debug("Requesting URL {}", url);
                HttpResult httpResult = retriever.httpGet(url);
                numRequests++;
                checkForHttpError(httpResult);
                jsonString = httpResult.getStringContent();
                JsonObject root = new JsonObject(jsonString);
                JsonArray itemsArray = root.getJsonArray("items");
                for (int j = 0; j < itemsArray.size(); j++) {
                    webResults.add(parseSnippet((JsonObject)itemsArray.get(j)));
                }
            }
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName()
                    + " (request URL: \"" + url + "\"): " + e.getMessage(), e);
        } catch (JsonException e) {
            throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                    + "\" with " + getName() + ", JSON was \"" + jsonString + "\": " + e, e);
        }
        LOGGER.debug("Query {} took {} requests", query, numRequests);
        return new SearchResults<WebVideo>(webResults, numResults);
    }

    /**
     * Check, if {@link HttpResult} had an error status and throw a {@link SearcherException} if so.
     * 
     * @param httpResult The HttpResult.
     * @throws SearcherException In case the result had a non-success status.
     */
    private static void checkForHttpError(HttpResult httpResult) throws SearcherException {
        if (httpResult.errorStatus()) {
            throw new SearcherException("Encountered HTTP status code " + httpResult.getStatusCode() + " for URL \""
                    + httpResult.getUrl() + "\": " + httpResult.getStringContent());
        }
    }

    private String buildSearchUrl(MultifacetQuery query, String pageToken, int numResults) throws SearcherException {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://www.googleapis.com/youtube/v3/search");
        urlBuilder.append("?part=id");
        if (StringUtils.isNotBlank(query.getText())) {
            urlBuilder.append("&q=").append(UrlHelper.encodeParameter(query.getText()));
        }
        Facet facet = query.getFacet(ws.palladian.retrieval.search.videos.YouTubeSearcher.OrderBy.YOUTUBE_RESULT_ORDER);
        if (facet != null) {
            ws.palladian.retrieval.search.videos.YouTubeSearcher.OrderBy orderByFacet = (ws.palladian.retrieval.search.videos.YouTubeSearcher.OrderBy)facet;
            urlBuilder.append("&order=").append(orderByFacet.getValue());
        }
        if (StringUtils.isNotBlank(pageToken)) {
            urlBuilder.append("&pageToken=").append(pageToken);
        }
        urlBuilder.append("&maxResults=").append(numResults);
        urlBuilder.append("&key=").append(apiKey);
//        Language language = query.getLanguage();
//        if (language != null) {
//            urlBuilder.append("&lr=").append(language.getIso6391());
//        }
        if (query.getCoordinate() != null) {
            LOGGER.warn("Searching by coordinates is currently not supported by YouTube API V3; see: "
                    + "https://code.google.com/p/gdata-issues/issues/detail?id=4234");
        }
        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        if (query.getStartDate() != null) {
            urlBuilder.append("&publishedAfter=").append(dateFormat.format(query.getStartDate()));
        }
        if (query.getEndDate() != null) {
            urlBuilder.append("&publishedBefore=").append(dateFormat.format(query.getEndDate()));
        }
        urlBuilder.append("&safeSearch=none");
        urlBuilder.append("&type=video");
        return urlBuilder.toString();
    }

    private String buildListUrl(List<String> ids) {
        StringBuilder urlBuilder = new StringBuilder();
        // https://developers.google.com/youtube/v3/docs/videos/list
        urlBuilder.append("https://www.googleapis.com/youtube/v3/videos");
        urlBuilder.append("?part=snippet,contentDetails,statistics,recordingDetails");
        urlBuilder.append("&key=").append(apiKey);
        urlBuilder.append("&id=").append(StringUtils.join(ids, ','));
        return urlBuilder.toString();
    }

    private WebVideo parseSnippet(JsonObject entry) throws JsonException {
        BasicWebVideo.Builder builder = new BasicWebVideo.Builder();
        String published = entry.queryString("snippet/publishedAt");
        builder.setPublished(parseDate(published));
        // recorded time is not available in most cases
        // String recorded = entry.tryQueryString("recordingDetails/recordingDate");
        builder.setTitle(entry.queryString("snippet/title"));
        String videoId = entry.queryString("id");
        builder.setIdentifier(videoId);
        builder.setSource(SEARCHER_NAME);
        builder.setUrl("http://www.youtube.com/watch?v=" + videoId);
        builder.setDuration(parseIso8601Duration(entry.queryString("contentDetails/duration")));
        builder.setViews(entry.tryQueryInt("statistics/viewCount"));
        builder.setSummary(entry.queryString("snippet/description"));
        builder.setThumbnailUrl(entry.queryString("snippet/thumbnails/high/url"));
        // like/dislike statistics
        long numDislikes = entry.queryInt("statistics/dislikeCount");
        long numLikes = entry.queryInt("statistics/likeCount");
        long total = numLikes + numDislikes;
        if (total > 0) {
            builder.setRating(numLikes / (double)total);
        }
        // geographic location
        Double latitude = entry.tryQueryDouble("recordingDetails/location/latitude");
        Double longitude = entry.tryQueryDouble("recordingDetails/location/longitude");
        if (latitude != null && longitude != null) {
            builder.setCoordinate(latitude, longitude);
        }
        // no tags available ):
        // see: http://stackoverflow.com/questions/12501957/video-tags-no-longer-available-via-youtube-api
        return builder.create();
    }

    /**
     * Parse the duration which is given as ISO8601 (this method does not implement the complete standard, but only what
     * is necessary for YouTube).
     * 
     * @param iso8601string The duration string.
     * @return The duration in seconds, or <code>null</code> in case the duration string could not be parsed.
     */
    private static Integer parseIso8601Duration(String iso8601string) {
        if (StringUtils.isBlank(iso8601string)) {
            return null;
        }
        Matcher matcher = DURATION_PATTERN.matcher(iso8601string);
        if (matcher.matches()) {
            int hours = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
            int minutes = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            int seconds = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            int duration = 60 * (60 * hours + minutes) + seconds;
            return duration;
        } else {
            LOGGER.warn("Error while parsing duration string \"{}\".", iso8601string);
            return null;
        }
    }

    private Date parseDate(String dateString) {
        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH);
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            LOGGER.error("Error parsing date {}", dateString, e);
        }
        return null;
    }

}
