package ws.palladian.retrieval.search.web;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringEscapeUtils;
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
 * Searcher for Tweets on Twitter. In contrast to other web searchers, not every search result contains a URL, as we
 * simply extract URLs from the Tweet text, if present. Furthermore, no title is present. The Tweet content can be
 * accessed via {@link WebResult#getSummary()}.
 * </p>
 * 
 * @see <a href="https://dev.twitter.com/docs/api/1/get/search">API Resources: GET search</a>
 * @author Philipp Katz
 */
public final class TwitterSearcher extends WebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TwitterSearcher.class);

    /** The result type for which to search. */
    public static enum ResultType {
        /** Popular + real time results. */
        MIXED,
        /** Only most recent results. */
        RECENT,
        /** Only most popular results. */
        POPULAR
    }

    private static final String DATE_PATTERN = "E, dd MMM yyyy HH:mm:ss Z";

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    public List<WebResult> search(String query, int resultCount, Language language, ResultType resultType)
            throws SearcherException {
        List<WebResult> webResults = new ArrayList<WebResult>();
        int resultsPerPage = Math.min(100, resultCount);
        int numRequests = (int)Math.ceil(resultCount / 100.);

        try {
            for (int page = 1; page <= numRequests; page++) {

                String requestUrl = buildRequestUrl(query, resultsPerPage, language, page, resultType);
                HttpResult httpResult = performHttpRequest(requestUrl);

                String responseString = HttpHelper.getStringContent(httpResult);
                LOGGER.debug("response for " + requestUrl + " : " + responseString);

                JSONObject jsonObject = new JSONObject(responseString);
                JSONArray jsonResults = jsonObject.getJSONArray("results");
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
                    // take the first URL from the tweet, if present.
                    // List<String> urls = UrlHelper.extractUrls(text);
                    // String url = urls.isEmpty() ? null : urls.get(0);
                    String url = createTweetUrl(jsonResult.getString("from_user"), jsonResult.getString("id_str"));
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
                    + getName() + ": " + e.getMessage(), e);
        }

        LOGGER.debug("twitter requests: " + TOTAL_REQUEST_COUNT.get());
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
            LOGGER.error("error parsing date " + dateString, e);
        }
        return date;
    }

    private HttpResult performHttpRequest(String requestUrl) throws HttpException, SearcherException {
        HttpResult httpResult = retriever.httpGet(requestUrl);
        TOTAL_REQUEST_COUNT.incrementAndGet();

        int statusCode = httpResult.getStatusCode();
        if (statusCode == 420) {
            throw new SearcherException("twitter is currently blocked due to rate limit");
        }
        if (statusCode >= 400) {
            throw new SearcherException("HTTP error " + statusCode + " for request URL " + requestUrl);
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
    private String buildRequestUrl(String query, int resultsPerPage, Language language, int page, ResultType resultType) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://search.twitter.com/search.json");
        urlBuilder.append("?q=").append(UrlHelper.urlEncode(query));
        urlBuilder.append("&page=").append(page);
        urlBuilder.append("&rpp=").append(resultsPerPage);
        urlBuilder.append("&lang=").append(getLanguageCode(language));
        urlBuilder.append("&result_type").append(resultType.toString().toLowerCase());
        return urlBuilder.toString();
    }

    /**
     * <p>
     * Get the ISO 639-1 code for the specified language.
     * </p>
     * 
     * @param language
     * @return
     */
    private String getLanguageCode(Language language) {
        switch (language) {
            case ENGLISH:
                return "en";
            case GERMAN:
                return "de";
            default:
                break;
        }
        throw new IllegalArgumentException("No code defined for language " + language);
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
