package ws.palladian.retrieval.search.socialmedia;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.helper.nlp.StringHelper;
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
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.Facet;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.RateLimitedException;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Searcher for Tweets on Twitter. The Tweet content can be accessed via {@link BasicWebContent#getSummary()}.
 * </p>
 * 
 * @see <a href="https://dev.twitter.com/docs/api/1.1/get/search/tweets">API Resources: GET search</a>
 * @author Philipp Katz
 */
public final class TwitterSearcher extends AbstractMultifacetSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterSearcher.class);

    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Twitter";

    /** The result type for which to search. */
    public static enum ResultType implements Facet {
        /** Popular + real time results. */
        MIXED,
        /** Only most recent results. */
        RECENT,
        /** Only most popular results. */
        POPULAR;

        private static final String TWITTER_RESULT_TYPE_ID = "twitter.resultType";

        @Override
        public String getIdentifier() {
            return TWITTER_RESULT_TYPE_ID;
        }

        public String getValue() {
            return toString().toLowerCase();
        }
    }

    /** The identifier for the {@link Configuration} key with the OAuth consumer key. */
    public static final String CONFIG_CONSUMER_KEY = "api.twitter.consumerKey";
    /** The identifier for the {@link Configuration} key with the OAuth consumer secret. */
    public static final String CONFIG_CONSUMER_SECRET = "api.twitter.consumerSecret";
    /** The identifier for the {@link Configuration} key with the OAuth access token. */
    public static final String CONFIG_ACCESS_TOKEN = "api.twitter.accessToken";
    /** The identifier for the {@link Configuration} key with the OAuth access token secret. */
    public static final String CONFIG_ACCESS_TOKEN_SECRET = "api.twitter.accessTokenSecret";

    private static final String DATE_PATTERN = "E MMM dd HH:mm:ss Z yyyy";

    private static final String REQUEST_DATE_PATTERN = "yyyy-MM-dd";

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /**
     * Identifier for the raw JSON representation of a Tweet, which can be retrieved via
     * {@link WebContent#getAdditionalData()}.
     */
    public static final String DATA_ROW_JSON = "twitter.tweet.raw.json";

    private final OAuthParams oAuthParams;

    private final HttpRetriever retriever;

    /**
     * <p>
     * Create a new {@link TwitterSearcher}.
     * </p>
     * 
     * @param oAuthParams The parameters for the OAuth-based authentication, not <code>null</code>
     */
    public TwitterSearcher(OAuthParams oAuthParams) {
        Validate.notNull(oAuthParams, "oAuthParams must not be null");
        this.oAuthParams = oAuthParams;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Create a new {@link TwitterSearcher}.
     * </p>
     * 
     * @param consumerKey The OAuth consumer key, not <code>null</code> or empty.
     * @param consumerSecret The OAuth consumer secret, not <code>null</code> or empty.
     * @param accessToken The OAuth access token, not <code>null</code> or empty.
     * @param accessTokenSecret The OAuth access token secret, not <code>null</code> or empty.
     */
    public TwitterSearcher(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        this(new OAuthParams(consumerKey, consumerSecret, accessToken, accessTokenSecret));
    }

    /**
     * <p>
     * Create a new {@link TwitterSearcher}.
     * </p>
     * 
     * @param configuration A {@link Configuration} instance providing the necessary parameters for OAuth authentication
     *            ({@value #CONFIG_CONSUMER_KEY}, {@value #CONFIG_CONSUMER_SECRET}, {@value #CONFIG_ACCESS_TOKEN},
     *            {@value #CONFIG_ACCESS_TOKEN_SECRET}), not <code>null</code>.
     */
    public TwitterSearcher(Configuration configuration) {
        this(new OAuthParams(configuration.getString(CONFIG_CONSUMER_KEY),
                configuration.getString(CONFIG_CONSUMER_SECRET), configuration.getString(CONFIG_ACCESS_TOKEN),
                configuration.getString(CONFIG_ACCESS_TOKEN_SECRET)));
    }

    /**
     * <p>
     * Build URL linking to the Tweet, which is of the form
     * <code>http://twitter.com/{twitter-user-id}/status/{tweet-status-id}</code>.
     * </p>
     * 
     * @param userId The Twitter user id.
     * @param statusId The Twitter status id.
     * @return The URL linking to the Twitter webpage for showing the Tweet, or <code>null</code> in case any of the
     *         both parameters was <code>null</code>.
     */
    private String createTweetUrl(String userId, String statusId) {
        if (userId == null || statusId == null) {
            return null;
        }
        return String.format("http://twitter.com/%s/status/%s", userId, statusId);
    }

    private static final Date parseDate(String dateString) {
        Date date = null;
        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH);
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            LOGGER.error("Error parsing date {}", dateString, e);
        }
        return date;
    }

    private HttpResult performHttpRequest(HttpRequest request) throws HttpException, SearcherException {
        HttpResult httpResult = retriever.execute(request);
        TOTAL_REQUEST_COUNT.incrementAndGet();

        int statusCode = httpResult.getStatusCode();
        String resetHeader = httpResult.getHeaderString("X-Rate-Limit-Reset");
        String remainingHeader = httpResult.getHeaderString("X-Rate-Limit-Remaining");
        LOGGER.debug("X-Rate-Limit-Reset = {}; X-Rate-Limit-Remaining = {}", resetHeader, remainingHeader);

        if (statusCode == 429) { // changed to v1.1 without verifying; see
                                 // https://dev.twitter.com/docs/rate-limiting/1.1
            Integer timeUntilReset = null;
            if (resetHeader != null) {
                try {
                    int now = (int)(System.currentTimeMillis() / 1000);
                    timeUntilReset = Integer.valueOf(resetHeader) - now;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            throw new RateLimitedException("Twitter is currently blocked due to rate limit", timeUntilReset);
        }
        if (statusCode >= 400) {
            String content = httpResult.getStringContent();
            throw new SearcherException("HTTP error " + statusCode + " for request " + request + ": " + content);
        }
        return httpResult;
    }

    /**
     * <p>
     * Create the request URL for the supplied parameters.
     * </p>
     * 
     * @param query The actual query.
     * @return The authenticated {@link HttpRequest} for accessing the API.
     */
    private HttpRequest buildRequest(MultifacetQuery query, String maxId) {
        HttpRequest request;
        if (query.getId() != null && query.getId().length() > 0) {
            // query for ID
            request = new HttpRequest(HttpMethod.GET, "https://api.twitter.com/1.1/statuses/show.json");
            request.addParameter("id", query.getId());
        } else {
            // query by criteria
            request = new HttpRequest(HttpMethod.GET, "https://api.twitter.com/1.1/search/tweets.json");
            if (query.getText() != null) {
                request.addParameter("q", query.getText());
            }
            request.addParameter("count", Math.min(100, query.getResultCount()));
            if (query.getLanguage() != null) {
                request.addParameter("lang", query.getLanguage().getIso6391());
            }
            Facet facet = query.getFacet(ResultType.TWITTER_RESULT_TYPE_ID);
            if (facet != null) {
                ResultType resultType = (ResultType)facet;
                request.addParameter("result_type", resultType.getValue());
            }
            GeoCoordinate coordinate = query.getCoordinate();
            if (coordinate != null) {
                double radius = query.getRadius() != null ? query.getRadius() : 10;
                String geocode = String.format("%s,%s,%skm", coordinate.getLatitude(), coordinate.getLongitude(),
                        radius);
                request.addParameter("geocode", geocode);

            }
            if (query.getEndDate() != null) {
                String untilString = new SimpleDateFormat(REQUEST_DATE_PATTERN).format(query.getEndDate());
                request.addParameter("until", untilString);
            }
            if (StringUtils.isNotBlank(maxId)) {
                request.addParameter("max_id", maxId);
            }
        }
        HttpRequest signedRequest = OAuthUtil.createSignedRequest(request, oAuthParams);
        LOGGER.debug("Request: {}", request);
        return signedRequest;
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws RateLimitedException, SearcherException {

        List<WebContent> webResults = new ArrayList<WebContent>();
        String responseString = null;
        String maxId = null;

        try {

            for (; webResults.size() < query.getResultCount();) {
                HttpRequest request = buildRequest(query, maxId);
                HttpResult httpResult = performHttpRequest(request);

                responseString = httpResult.getStringContent();
                LOGGER.trace("Response for {}: {}", request, responseString);
                JsonObject jsonObject = new JsonObject(responseString);

                String nextResults = jsonObject.tryQueryString("/search_metadata/next_results");
                if (nextResults != null) {
                    maxId = StringHelper.getSubstringBetween(nextResults, "max_id=", "&");
                }

                if (query.getId() != null && query.getId().length() > 0) {
                    // retrieve single result (ID query)
                    webResults.add(parseSingleEntry(jsonObject));
                } else {
                    JsonArray jsonResults = jsonObject.getJsonArray("statuses");
                    int numResults = jsonResults.size();

                    for (int i = 0; i < numResults; i++) {
                        JsonObject jsonResult = jsonResults.getJsonObject(i);
                        WebContent result = parseSingleEntry(jsonResult);
                        webResults.add(result);
                        if (webResults.size() >= query.getResultCount()) {
                            break;
                        }
                    }
                }
                if (nextResults == null) {
                    break;
                }
            }
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        } catch (JsonException e) {
            throw new SearcherException("Error parsing the JSON response while searching for \"" + query + "\" with "
                    + getName() + ": " + e.getMessage() + " (JSON: '" + responseString + "')", e);
        }

        LOGGER.debug("twitter requests: {}", TOTAL_REQUEST_COUNT.get());
        return new SearchResults<WebContent>(webResults);
    }

    private WebContent parseSingleEntry(JsonObject jsonResult) throws JsonException {
        BasicWebContent.Builder builder = new BasicWebContent.Builder();
        builder.setTitle(StringEscapeUtils.unescapeHtml4(jsonResult.getString("text")));
        builder.setPublished(parseDate(jsonResult.getString("created_at")));
        builder.setIdentifier(jsonResult.getString("id_str"));

        JsonObject jsonUser = jsonResult.getJsonObject("user");
        builder.setUrl(createTweetUrl(jsonUser.getString("screen_name"), jsonResult.getString("id_str")));

        if (jsonResult.get("coordinates") != null) {
            JsonObject jsonCoordinates = jsonResult.getJsonObject("coordinates");
            String type = jsonCoordinates.tryGetString("type");
            if (!"point".equalsIgnoreCase(type)) {
                LOGGER.warn("Unexpected coordinate type: " + type);
            } else {
                JsonArray coordinates = jsonCoordinates.getJsonArray("coordinates");
                double lat = coordinates.getDouble(1);
                double lng = coordinates.getDouble(0);
                builder.setCoordinate(new ImmutableGeoCoordinate(lat, lng));
            }
        }
        JsonArray hashTagsArray = jsonResult.queryJsonArray("/entities/hashtags");
        for (int i = 0; i < hashTagsArray.size(); i++) {
            JsonObject hashTagObject = hashTagsArray.getJsonObject(i);
            builder.addTag(hashTagObject.getString("text"));
        }
        builder.setSource(SEARCHER_NAME);
        builder.setAdditionalData(DATA_ROW_JSON, jsonResult);

        WebContent result = builder.create();
        return result;
    }

    /**
     * Gets the number of HTTP requests sent to Twitter.
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }

}
