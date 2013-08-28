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
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.OAuthParams;
import ws.palladian.retrieval.OAuthUtil;
import ws.palladian.retrieval.search.SearcherException;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;

/**
 * <p>
 * Searcher for Tweets on Twitter. The Tweet content can be accessed via {@link WebResult#getSummary()}.
 * </p>
 * 
 * @see <a href="https://dev.twitter.com/docs/api/1/get/search">API Resources: GET search</a>
 * @author Philipp Katz
 */
public final class TwitterSearcher extends WebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterSearcher.class);

    /** The result type for which to search. */
    public static enum ResultType {
        /** Popular + real time results. */
        MIXED,
        /** Only most recent results. */
        RECENT,
        /** Only most popular results. */
        POPULAR
    }

    /** The identifier for the {@link Configuration} key with the OAuth consumer key. */
    public static final String CONFIG_CONSUMER_KEY = "api.twitter.consumerKey";
    /** The identifier for the {@link Configuration} key with the OAuth consumer secret. */
    public static final String CONFIG_CONSUMER_SECRET = "api.twitter.consumerSecret";
    /** The identifier for the {@link Configuration} key with the OAuth access token. */
    public static final String CONFIG_ACCESS_TOKEN = "api.twitter.accessToken";
    /** The identifier for the {@link Configuration} key with the OAuth access token secret. */
    public static final String CONFIG_ACCESS_TOKEN_SECRET = "api.twitter.accessTokenSecret";

    private static final String DATE_PATTERN = "E, dd MMM yyyy HH:mm:ss Z";

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    private final OAuthParams oAuthParams;

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
        Validate.notNull(configuration, "configuration must not be null");
        this.oAuthParams = new OAuthParams(configuration.getString(CONFIG_CONSUMER_KEY),
                configuration.getString(CONFIG_CONSUMER_SECRET), configuration.getString(CONFIG_ACCESS_TOKEN),
                configuration.getString(CONFIG_ACCESS_TOKEN_SECRET));
    }

    public List<WebResult> search(String query, int resultCount, Language language, ResultType resultType)
            throws SearcherException {
        List<WebResult> webResults = new ArrayList<WebResult>();
        int resultsPerPage = Math.min(100, resultCount);
        int numRequests = (int)Math.ceil(resultCount / 100.);

        // XXX v1.1 currently does not support paging, so 100 results is maximum;
        // leave code here for now, maybe this will be improved in the future
        // https://dev.twitter.com/discussions/11016
        if (resultCount > 100) {
            LOGGER.warn("Currently, at most 100 results per query are supported by the Twitter API.");
        }

        String responseString = null;

        try {

            for (int page = 1; page <= numRequests; page++) {

                HttpRequest request = buildRequest(query, resultsPerPage, language, page, resultType);
                HttpResult httpResult = performHttpRequest(request);

                responseString = httpResult.getStringContent();
                LOGGER.debug("Response for {}: {}", request, responseString);

                JSONObject jsonObject = new JSONObject(responseString);
                JSONArray jsonResults = jsonObject.getJSONArray("statuses");
                int numResults = jsonResults.length();

                // stop, if we got no results
                if (numResults == 0) {
                    break;
                }

                for (int i = 0; i < numResults; i++) {
                    JSONObject jsonResult = jsonResults.getJSONObject(i);
                    String text = StringEscapeUtils.unescapeHtml4(jsonResult.getString("text"));
                    String dateString = jsonResult.getString("created_at");
                    Date date = parseDate(dateString);
                    JSONObject jsonUser = jsonResult.getJSONObject("user");
                    String url = createTweetUrl(jsonUser.getString("screen_name"), jsonResult.getString("id_str"));
                    webResults.add(new WebResult(url, text, null, date));
                    if (webResults.size() >= resultCount) {
                        break;
                    }
                }
            }
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        } catch (JSONException e) {
            throw new SearcherException("Error parsing the JSON response while searching for \"" + query + "\" with "
                    + getName() + ": " + e.getMessage() + " (JSON: '" + responseString + "')", e);
        }

        LOGGER.debug("twitter requests: {}", TOTAL_REQUEST_COUNT.get());
        return webResults;
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
        if (userId == null | statusId == null) {
            return null;
        }
        return String.format("http://twitter.com/%s/status/%s", userId, statusId);
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {
        return search(query, resultCount, language, ResultType.MIXED);
    }

    private Date parseDate(String dateString) {
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
        if (statusCode == 420) {
            throw new SearcherException("Twitter is currently blocked due to rate limit");
        }
        if (statusCode >= 400) {
            throw new SearcherException("HTTP error " + statusCode + " for request " + request + ": "
                    + httpResult.getStringContent());
        }
        return httpResult;
    }

    /**
     * <p>
     * Create the request URL for the supplied parameters.
     * </p>
     * 
     * @param query The actual query.
     * @param resultsPerPage The number of results to return.
     * @param language The language.
     * @param page The page index.
     * @return
     */
    private HttpRequest buildRequest(String query, int resultsPerPage, Language language, int page,
            ResultType resultType) {
        HttpRequest request = new HttpRequest(HttpMethod.GET, "https://api.twitter.com/1.1/search/tweets.json");
        request.addParameter("q", query);
        request.addParameter("count", resultsPerPage);
        request.addParameter("lang", language.getIso6391());
        request.addParameter("result_type", resultType.toString().toLowerCase());
        HttpRequest signedRequest = OAuthUtil.createSignedRequest(request, oAuthParams);
        return signedRequest;
    }

    @Override
    public String getName() {
        return "Twitter";
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
