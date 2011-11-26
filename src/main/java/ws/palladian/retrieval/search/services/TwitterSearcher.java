package ws.palladian.retrieval.search.services;

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
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.search.WebResult;

/**
 * <p>
 * Searcher for Tweets on Twitter. In contrast to other web searchers, not every search result contains a URL, as we
 * simply extract URLs from the Tweet text, if present. Furthermore, no title is present. The Tweet content can be
 * accessed via {@link WebResult#getSummary()}.
 * </p>
 * 
 * @see https://dev.twitter.com/docs/api/1/get/search
 * @author Philipp Katz
 */
public final class TwitterSearcher extends BaseWebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TwitterSearcher.class);

    private static final String DATE_PATTERN = "E, dd MMM yyyy HH:mm:ss Z";

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    @Override
    public List<WebResult> search(String query) {

        List<WebResult> webResults = new ArrayList<WebResult>();
        int resultsPerPage = Math.min(100, getResultCount());
        int numRequests = (int) Math.ceil(getResultCount() / 100.);

        try {
            for (int page = 1; page <= numRequests; page++) {

                String requestUrl = buildRequestUrl(query, resultsPerPage, page);

                HttpResult httpResult = retriever.httpGet(requestUrl);
                TOTAL_REQUEST_COUNT.incrementAndGet();

                int statusCode = httpResult.getStatusCode();
                if (statusCode == 420) {
                    LOGGER.error("twitter is currently blocked due to rate limit");
                    break;
                }
                if (statusCode >= 400) {
                    LOGGER.error("http error " + statusCode);
                    break;
                }

                String responseString = new String(httpResult.getContent());
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
                    if (webResults.size() >= getResultCount()) {
                        break;
                    }
                }
            }
        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (JSONException e) {
            LOGGER.error(e);
        }
        LOGGER.info("twitter requests: " + TOTAL_REQUEST_COUNT.get());
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

    /**
     * <p>
     * Create the request URL for the supplied parameters.
     * </p>
     * 
     * @param query
     * @param resultsPerPage
     * @param page
     * @return
     */
    private String buildRequestUrl(String query, int resultsPerPage, int page) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://search.twitter.com/search.json");
        urlBuilder.append("?q=").append(UrlHelper.urlEncode(query));
        urlBuilder.append("&page=").append(page);
        urlBuilder.append("&rpp=").append(resultsPerPage);
        return urlBuilder.toString();
    }

    @Override
    public String getName() {
        return "Twitter";
    }

}
