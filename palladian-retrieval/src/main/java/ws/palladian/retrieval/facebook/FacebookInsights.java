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
    enum Period {
        DAY, WEEK, DAYS_28, MONTH, LIFETIME
    }

    /** Format for parsing dates sent to and returned by API. */
    static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

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
        String url = "https://graph.facebook.com/v2.5/" + pageOrPostId + "/insights";
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
        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        HttpResult result;
        try {
            result = retriever.execute(requestBuilder.create());
        } catch (HttpException e) {
            throw new FacebookInsightsException("Error during HTTP request", e);
        }
        checkError(result);
        LOGGER.debug("JSON result = {}", result.getStringContent());
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
                String endTime = currentJsonValue.getString("end_time");
                Object value = currentJsonValue.get("value");
                values.add(new Value(value, parseTime(endTime)));
            }
            return new Insights(name, period, values, title, description, id);
        } catch (JsonException e) {
            throw new FacebookInsightsException("Could not parse JSON result (" + result.getStringContent() + ")", e);
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

    private static Date parseTime(String timeString) throws FacebookInsightsException {
        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(timeString);
        } catch (ParseException e) {
            throw new FacebookInsightsException("Could not parse tim string " + timeString + " using " + DATE_FORMAT);
        }
    }

}
