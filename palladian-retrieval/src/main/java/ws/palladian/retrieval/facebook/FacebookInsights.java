package ws.palladian.retrieval.facebook;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpMethod;
import ws.palladian.retrieval.HttpRequest2;
import ws.palladian.retrieval.HttpRequest2Builder;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * Retrieve <a href="https://www.facebook.com">Facebook</a> Insights.
 * 
 * @see <a href="https://developers.facebook.com/tools/explorer">Graph API Explorer to get a "Page Access Token".</a>
 * @see <a href="https://developers.facebook.com/docs/graph-api/reference/v2.5/insights">Graph API documentation for
 *      Insights, listing all available metrics.</a>
 * @see <a href="https://developers.facebook.com/docs/platforminsights/page">General information about Facebook Page
 *      Insights API</a>
 * @author Philipp Katz
 */
public class FacebookInsights {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookInsights.class);

    /** The period for an insights result. */
    public enum Period {
        DAY, WEEK, DAYS_28, MONTH, LIFETIME
    }

    /** Format for parsing dates sent to and returned by API. */
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    /** Version of the API which we use. */
    private static final String API_VERSION = "v2.5";

    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    private final String accessToken;

    /**
     * <p>
     * Create a new {@link FacebookInsights} instance.
     * 
     * @param accessToken The access token, not empty or <code>null</code>. Note that this is a "Page Access Token", not
     *            a "User Access Token"!
     */
    public FacebookInsights(String accessToken) {
        Validate.notEmpty(accessToken, "accessToken must not be empty");
        this.accessToken = accessToken;
    }

    /**
     * <p>
     * Retrieves Facebook insights.
     * 
     * @param pageOrPostId The ID of the page or the post for which to retrieve insights.
     * @param metric Name of the metric.
     * @param since Lower time interval, or <code>null</code>.
     * @param until Upper time interval, or <code>null</code>.
     * @param period The period resolution.
     * @return The insights.
     * @throws FacebookInsightsException In case anything goes wrong.
     * @see <a href="https://developers.facebook.com/docs/graph-api/reference/v2.5/insights">Graph API documentation for
     *      Insights, listing all available metrics.</a>
     */
    public final Insights getInsights(String pageOrPostId, String metric, Date since, Date until, Period period)
            throws FacebookInsightsException {
        Validate.notEmpty(pageOrPostId, "pageOrPostId must not be empty");
        Validate.notEmpty(metric, "metric must not be empty");
        Validate.notNull(period, "period must not be null");
        validateTimeInterval(since, until);
        String url = "https://graph.facebook.com/" + API_VERSION + "/" + pageOrPostId + "/insights";
        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.GET, url);
        requestBuilder.addUrlParam("metric", metric);
        requestBuilder.addUrlParam("period", period.toString().toLowerCase());
        requestBuilder.addUrlParam("access_token", accessToken);
        if (since != null) {
            // need to go back one day, else the specified "since" day is missing!
            long sinceTime = since.getTime() - TimeUnit.DAYS.toMillis(1);
            requestBuilder.addUrlParam("since", String.valueOf(sinceTime / 1000));
        }
        if (until != null) {
            requestBuilder.addUrlParam("until", String.valueOf(until.getTime() / 1000));
        }
        HttpResult result = performRequest(requestBuilder.create());
        try {
            JsonObject jsonResult = new JsonObject(result.getStringContent());
            JsonArray jsonData = jsonResult.getJsonArray("data");
            if (jsonData.size() != 1) {
                throw new IllegalStateException("Size of array should be one, but was " + jsonData.size());
            }
            JsonObject firstData = jsonData.getJsonObject(0);
            String name = firstData.getString("name");
            String title = firstData.getString("title");
            String description = firstData.getString("description");
            String id = firstData.getString("id");
            List<Value> values = new ArrayList<>();
            JsonArray jsonValues = firstData.getJsonArray("values");
            for (int i = 0; i < jsonValues.size(); i++) {
                JsonObject currentJsonValue = jsonValues.getJsonObject(i);
                String endTime = currentJsonValue.tryGetString("end_time");
                Object value = currentJsonValue.get("value");
                values.add(new Value(value, parseTime(endTime)));
            }
            return new Insights(name, period, values, title, description, id);
        } catch (JsonException e) {
            throw new FacebookInsightsException("Could not parse JSON result (" + result.getStringContent() + ")", e);
        }
    }

    /**
     * <p>
     * Retrieves a Facebook page's feed.
     * 
     * @param pageId The ID of the page.
     * @param since Lower time interval, or <code>null</code>.
     * @param until Upper time interval, or <code>null</code>.
     * @return The feed.
     * @throws FacebookInsightsException In case anything goes wrong.
     */
    public final List<FeedItem> getFeed(String pageId, Date since, Date until) throws FacebookInsightsException {
        Validate.notEmpty(pageId, "pageId must not be empty");
        validateTimeInterval(since, until);
        String url = "https://graph.facebook.com/" + API_VERSION + "/" + pageId + "/feed";
        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.GET, url);
        if (since != null) {
            requestBuilder.addUrlParam("since", String.valueOf(since.getTime() / 1000));
        }
        if (until != null) {
            requestBuilder.addUrlParam("until", String.valueOf(until.getTime() / 1000));
        }
        requestBuilder.addUrlParam("access_token", accessToken);
        HttpRequest2 request = requestBuilder.create();
        List<FeedItem> items = new ArrayList<>();
        for (;;) { // paging
            HttpResult result = performRequest(request);
            try {
                JsonObject jsonResult = new JsonObject(result.getStringContent());
                JsonArray jsonData = jsonResult.getJsonArray("data");
                if (jsonData.size() == 0) {
                    break;
                }
                for (int i = 0; i < jsonData.size(); i++) {
                    JsonObject currentJson = jsonData.getJsonObject(i);
                    String id = currentJson.getString("id");
                    Date createdTime = parseTime(currentJson.getString("created_time"));
                    String message = currentJson.tryGetString("message");
                    String story = currentJson.tryGetString("story");
                    items.add(new FeedItem(id, createdTime, message, story));
                }
                // build next request
                String nextUrl = jsonResult.getJsonObject("paging").getString("next");
                LOGGER.debug("Paging to URL {}", nextUrl);
                request = new HttpRequest2Builder(HttpMethod.GET, nextUrl).create();
            } catch (JsonException e) {
                throw new FacebookInsightsException("Could not parse JSON result (" + result.getStringContent() + ")",
                        e);
            }
        }
        return items;
    }

    private HttpResult performRequest(HttpRequest2 request) throws FacebookInsightsException {
        HttpResult result;
        try {
            result = retriever.execute(request);
        } catch (HttpException e) {
            throw new FacebookInsightsException("Error during HTTP request", e);
        }
        checkError(result);
        LOGGER.debug("JSON result = {}", result.getStringContent());
        return result;
    }

    /**
     * Validate a since-until time interval. In case both a given, since must be before until.
     * 
     * @param since Since or <code>null</code>.
     * @param until Until or <code>null</code>.
     */
    private static void validateTimeInterval(Date since, Date until) {
        if (since == null || until == null) {
            return;
        }
        if (until.before(since)) {
            throw new IllegalArgumentException("until must not be before since");
        }
    }

    private static void checkError(HttpResult result) throws FacebookInsightsException {
        if (result.errorStatus()) {
            String message;
            try {
                JsonObject jsonErrorObject = new JsonObject(result.getStringContent());
                message = jsonErrorObject.getJsonObject("error").getString("message");
                throw new FacebookInsightsException(message);
            } catch (JsonException e) {
                throw new FacebookInsightsException("Encountered error result from API, but could not parse as JSON ("
                        + result.getStringContent() + ")");
            }
        }
    }

    /**
     * Parse a time string.
     * 
     * @param timeString The time string, or <code>null</code>.
     * @return The parse time string, or <code>null</code> in case argument was null.
     * @throws FacebookInsightsException In case the time string could not be parsed.
     */
    private static Date parseTime(String timeString) throws FacebookInsightsException {
        if (timeString == null) {
            return null;
        }
        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(timeString);
        } catch (ParseException e) {
            throw new FacebookInsightsException("Could not parse tim string " + timeString + " using " + DATE_FORMAT);
        }
    }

}
