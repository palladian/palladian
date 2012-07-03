package ws.palladian.retrieval.search.web;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final String DATE_PATTERN = "E, dd MMM yyyy HH:mm:ss Z";

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebResult> webResults = new ArrayList<WebResult>();
        int resultsPerPage = Math.min(100, resultCount);
        int numRequests = (int) Math.ceil(resultCount / 100.);

        try {
            for (int page = 1; page <= numRequests; page++) {

                String requestUrl = buildRequestUrl(query, resultsPerPage, language, page);
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
                    String text = jsonResult.getString("text");
                    String dateString = jsonResult.getString("created_at");
                    Date date = parseDate(dateString);
                    List<String> urls = UrlHelper.extractUrls(text);
                    // take the first URL from the tweet, if present.
                    String url = urls.isEmpty() ? null : urls.get(0);
                    webResults.add(new WebResult(url, null, text, date));
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
    private String buildRequestUrl(String query, int resultsPerPage, Language language, int page) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://search.twitter.com/search.json");
        urlBuilder.append("?q=").append(UrlHelper.urlEncode(query));
        urlBuilder.append("&page=").append(page);
        urlBuilder.append("&rpp=").append(resultsPerPage);
        urlBuilder.append("&lang=").append(getLanguageCode(language));
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
