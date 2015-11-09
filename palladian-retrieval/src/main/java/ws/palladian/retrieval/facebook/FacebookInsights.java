package ws.palladian.retrieval.facebook;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * Retrieve Facebook Insights.
 * @author pk
 */
public class FacebookInsights {
    
    @SuppressWarnings("serial")
    public class FacebookInsightsException extends Exception {

        public FacebookInsightsException(String message) {
            super(message);
        }

    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookInsights.class);
    
    enum Period {
        day, week, days_28, month, lifetime
    }
    
    public static final class Insights {
        final String name;
        final Period period;
        final List<Value> values;
        final String title;
        final String description;
        final String id;
        Insights(String name, Period period, List<Value> values, String title, String description, String id) {
            this.name = name;
            this.period = period;
            this.values = values;
            this.title = title;
            this.description = description;
            this.id = id;
        }
    }
    
    public static class Value {
        final Object value;
        final Date endTime;
        public Value(Object value, Date endTime) {
            this.value = value;
            this.endTime = endTime;
        }
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Value [value=");
            builder.append(value);
            builder.append(", endTime=");
            builder.append(endTime);
            builder.append("]");
            return builder.toString();
        }
    }
    
    // 2015-11-07T08:00:00+0000
    static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    
    final String accessToken;
    
    public FacebookInsights(String accessToken) {
        this.accessToken=accessToken;
    }
    
    /**
     * Retrieves Facebook insights.
     * @param id The ID of the page.
     * @param metric Name of the metric.
     * @param since Lower time interval, or <code>null</code>.
     * @param until Upper time interval, or <code>null</code>.
     * @param period The period resolution.
     * @return The insights.
     * @throws HttpException
     * @throws JsonException
     * @throws ParseException
     * @throws FacebookInsightsException 
     */
    public final Insights getInsights(String id, String metric, Date since, Date until, Period period) throws HttpException, JsonException, ParseException, FacebookInsightsException {
        // https://graph.facebook.com/v2.5/384144361790908/insights?metric=page_consumptions&period=day&since=1443657600&access_token=CAACEdEose0cBAOIPlRvZCYQtHfLqur68DZCuREVXplhTK4CPDtZCPVlIJlYZCFCyWOmBMVPvXSOSqfy8DwIVoCATYnrtQNnXuOzP4AjWD5HX34uJSSXlZAlHV9Kz6YCcaGgbzSaNlr8G2q59aZApBBvAll4p3F31gAOHB4LOYqEmnmrycCrZBOC16lsa4kCE1cZD
        
        String url = String.format("https://graph.facebook.com/v2.5/%s/insights?metric=%s&period=%s&access_token=%s",
                id, metric, period.toString(), accessToken);
        if (since != null) {
            // FIXME need to go back one day, else the specified "since" day is missing!
            url += "&since=" + since.getTime() / 1000;
        }
        if (until != null) {
            url += "&until=" + until.getTime() / 1000;
        }
//        LOGGER.debug("Retrieving URL {}", url);
        System.out.println(url);
        
        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        HttpResult result = retriever.httpGet(url);
        if (result.errorStatus()) {
            JsonObject jsonErrorObject = new JsonObject(result.getStringContent());
            String message = jsonErrorObject.getJsonObject("error").getString("message");
            throw new FacebookInsightsException(message);
        }
        
        System.out.println(result.getStringContent());
        
        JsonObject jsonResult = new JsonObject(result.getStringContent());
        JsonArray jsonData = jsonResult.getJsonArray("data");
        if (jsonData.size() != 1) throw new IllegalStateException("Size of array should be one, but was " + jsonData.size());
        
        JsonObject firstData = jsonData.getJsonObject(0);
        List<Value> values = new ArrayList<>();
        
        String name = firstData.getString("name");
        String title = firstData.getString("title");
        String description = firstData.getString("description");
        String id2 = firstData.getString("id");
        JsonArray jsonValues = firstData.getJsonArray("values");
        for (int i = 0; i < jsonValues.size(); i++) {
            JsonObject currentJsonValue = jsonValues.getJsonObject(i);
            String endTimeString = currentJsonValue.getString("end_time");
            Object valueObject = currentJsonValue.get("value");
            values.add(new Value(valueObject, parseTime(endTimeString)));
        }
        return new Insights(name, null, values, title, description, id2);
    }

    private static Date parseTime(String endTimeString) throws ParseException {
        return new SimpleDateFormat(DATE_FORMAT).parse(endTimeString);
    }
    
    public static void main(String[] args) throws Exception, Exception, Exception {
        FacebookInsights insights = new FacebookInsights("xxx");
        Date since = parseTime("2014-01-01T00:00:00+0000");
        // Insights result = insights.getInsights("384144361790908", "page_consumptions", since, null, Period.day);
        // Insights result = insights.getInsights("384144361790908", "page_consumptions_by_consumption_type", since, null, Period.day);
        // Insights result = insights.getInsights("384144361790908", "page_impressions_by_country_unique", since, null, Period.day);
        // Insights result = insights.getInsights("384144361790908", "page_story_adds_by_age_gender_unique", since, null, Period.week);
        Insights result = insights.getInsights("384144361790908", "page_storytellers_by_country", since, null, Period.days_28);
        // CollectionHelper.print(result.values);
    }

}
