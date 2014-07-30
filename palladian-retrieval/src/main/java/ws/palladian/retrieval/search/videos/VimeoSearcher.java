package ws.palladian.retrieval.search.videos;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.OAuthParams;
import ws.palladian.retrieval.OAuthUtil;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebVideo;
import ws.palladian.retrieval.resources.WebVideo;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.RateLimitedException;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * WebSearcher for <a href="http://vimeo.com/">Vimeo</a>.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://developer.vimeo.com/apis/advanced/methods/vimeo.videos.search">API documentation</a>
 */
public final class VimeoSearcher extends AbstractMultifacetSearcher<WebVideo> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(VimeoSearcher.class);

    /** Constant for the name of this searcher. */
    private static final String SEARCHER_NAME = "Vimeo";

    /** Pattern for parsing the returned date strings. */
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** The identifier for the {@link Configuration} key with the OAuth consumer key. */
    public static final String CONFIG_CONSUMER_KEY = "api.vimeo.consumerKey";
    /** The identifier for the {@link Configuration} key with the OAuth consumer secret. */
    public static final String CONFIG_CONSUMER_SECRET = "api.vimeo.consumerSecret";
    /** The identifier for the {@link Configuration} key with the OAuth access token. */
    public static final String CONFIG_ACCESS_TOKEN = "api.vimeo.accessToken";
    /** The identifier for the {@link Configuration} key with the OAuth access token secret. */
    public static final String CONFIG_ACCESS_TOKEN_SECRET = "api.vimeo.accessTokenSecret";

    /** Authentication data. */
    private final OAuthParams oAuthParams;

    private final HttpRetriever retriever;

    /**
     * Create a new {@link VimeoSearcher}.
     * 
     * @param oAuthParams The parameters for the OAuth-based authentication, not <code>null</code>
     */
    public VimeoSearcher(OAuthParams oAuthParams) {
        Validate.notNull(oAuthParams, "oAuthParams must not be null");
        this.oAuthParams = oAuthParams;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Create a new {@link VimeoSearcher}.
     * </p>
     * 
     * @param consumerKey The OAuth consumer key, not <code>null</code> or empty.
     * @param consumerSecret The OAuth consumer secret, not <code>null</code> or empty.
     * @param accessToken The OAuth access token, not <code>null</code> or empty.
     * @param accessTokenSecret The OAuth access token secret, not <code>null</code> or empty.
     */
    public VimeoSearcher(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        this(new OAuthParams(consumerKey, consumerSecret, accessToken, accessTokenSecret));
    }

    /**
     * <p>
     * Create a new {@link VimeoSearcher}.
     * </p>
     * 
     * @param configuration A {@link Configuration} instance providing the necessary parameters for OAuth authentication
     *            ({@value #CONFIG_CONSUMER_KEY}, {@value #CONFIG_CONSUMER_SECRET}, {@value #CONFIG_ACCESS_TOKEN},
     *            {@value #CONFIG_ACCESS_TOKEN_SECRET}), not <code>null</code>.
     */
    public VimeoSearcher(Configuration configuration) {
        this(new OAuthParams(configuration.getString(CONFIG_CONSUMER_KEY),
                configuration.getString(CONFIG_CONSUMER_SECRET), configuration.getString(CONFIG_ACCESS_TOKEN),
                configuration.getString(CONFIG_ACCESS_TOKEN_SECRET)));
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    private HttpRequest buildRequest(MultifacetQuery query, int page, int resultCount) {
        HttpRequest request = new HttpRequest(HttpMethod.GET, "http://vimeo.com/api/rest/v2");
        request.addParameter("method", "vimeo.videos.search");
        request.addParameter("query", query.getText());
        request.addParameter("full_response", "true");
        request.addParameter("format", "json");
        request.addParameter("page", page);
        request.addParameter("per_page", resultCount);
        return OAuthUtil.createSignedRequest(request, oAuthParams);
    }

    @Override
    public SearchResults<WebVideo> search(MultifacetQuery query) throws SearcherException {

        if (StringUtils.isBlank(query.getText())) {
            throw new SearcherException("The query must specify a search string (other parameters are not supported).");
        }

        List<WebVideo> webResults = CollectionHelper.newArrayList();
        int requestedResults = query.getResultCount();
        Long availableResults = null;
        for (int page = 0; page < Math.ceil((double)requestedResults / 50); page++) {
            int itemsToGet = Math.min(50, requestedResults - page * 50);
            HttpRequest request = buildRequest(query, page, itemsToGet);
            LOGGER.debug("request = " + request);
            HttpResult httpResult;
            try {
                httpResult = retriever.execute(request);
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName()
                        + " (request: " + request + "): " + e.getMessage(), e);
            }
            checkRateLimits(httpResult);
            try {
                JsonObject json = new JsonObject(httpResult.getStringContent());
                availableResults = json.queryLong("/videos/total");
                List<WebVideo> parsedVideos = parseVideoResult(json);
                if (parsedVideos.isEmpty()) {
                    break;
                }
                webResults.addAll(parsedVideos);
            } catch (JsonException e) {
                throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                        + "\" with " + getName() + ": " + e.getMessage(), e);
            }
        }
        return new SearchResults<WebVideo>(webResults, availableResults);
    }

    private static void checkRateLimits(HttpResult httpResult) throws RateLimitedException {
        // http://developer.vimeo.com/guidelines/rate-limiting
        int rateLimit = Integer.parseInt(httpResult.getHeaderString("X-RateLimit-Limit"));
        int rateLimitRemaining = Integer.parseInt(httpResult.getHeaderString("X-RateLimit-Remaining"));
        int rateLimitReset = Integer.parseInt(httpResult.getHeaderString("X-RateLimit-Reset"));
        LOGGER.debug("Rate limit: " + rateLimit + ", remaining: " + rateLimitRemaining + ", reset: " + rateLimitReset);
        if (rateLimitRemaining == 0) {
            int timeUntilReset = rateLimitReset - (int)(System.currentTimeMillis() / 1000);
            throw new RateLimitedException("Rate limit exceeded, rate limit is " + rateLimit, timeUntilReset );
        }
    }

    public static List<WebVideo> parseVideoResult(JsonObject json) throws JsonException {
        List<WebVideo> result = CollectionHelper.newArrayList();
        JsonArray jsonVideos = json.queryJsonArray("videos/video");
        for (int i = 0; i < jsonVideos.size(); i++) {
            JsonObject jsonVideo = jsonVideos.getJsonObject(i);
            String uploadDateString = jsonVideo.getString("upload_date");
            String id = jsonVideo.getString("id");
            BasicWebVideo.Builder builder = new BasicWebVideo.Builder();
            builder.setTitle(jsonVideo.getString("title"));
            builder.setSummary(jsonVideo.getString("description"));
            builder.setPublished(parseDate(uploadDateString));
            builder.setUrl(String.format("https://vimeo.com/%s", id));
            builder.setDuration(jsonVideo.getInt("duration"));
            if (jsonVideo.get("tags") != null) {
                JsonArray tagArray = jsonVideo.queryJsonArray("/tags/tag");
                for (int j = 0; j < tagArray.size(); j++) {
                    String normalizedTag = tagArray.getJsonObject(j).getString("normalized");
                    builder.addTag(normalizedTag);
                }
            }
            builder.setSource(SEARCHER_NAME);
            builder.setIdentifier(jsonVideo.getString("id"));
            result.add(builder.create());
        }
        return result;
    }

    private static Date parseDate(String dateString) {
        DateFormat dateParser = new SimpleDateFormat(DATE_PATTERN);
        try {
            return dateParser.parse(dateString);
        } catch (ParseException e) {
            LOGGER.error("Error parsing date string '" + dateString + "'", e);
            return null;
        }
    }

}
