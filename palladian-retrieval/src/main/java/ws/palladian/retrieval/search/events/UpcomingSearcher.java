package ws.palladian.retrieval.search.events;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.JPathHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.helper.JsonObjectWrapper;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for events on <a href="http://beta.upcoming.yahoo.com/">Upcoming</a>.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="http://beta.upcoming.yahoo.com/services/api/explore/?method=event.search">Upcoming event search API</a>
 */
public class UpcomingSearcher extends EventSearcher {

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.upcoming.key";

    private final String apiKey;

    private Map<EventType, Integer> eventTypeMapping;

    /**
     * <p>
     * Creates a new Upcoming searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing Upcoming, not <code>null</code> or empty.
     */
    public UpcomingSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        setup();
    }

    /**
     * <p>
     * Creates a new Upcoming searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing Upcoming, which must be
     *            provided
     *            as string via key {@value UpcomingSearcher#CONFIG_API_KEY} in the configuration.
     */
    public UpcomingSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
        setup();
    }

    private void setup() {
        eventTypeMapping = CollectionHelper.newHashMap();
        eventTypeMapping.put(EventType.CONCERT, 1);
        eventTypeMapping.put(EventType.COMEDY, 11);
        eventTypeMapping.put(EventType.THEATRE, 2);
        eventTypeMapping.put(EventType.EXHIBITION, 6);
        eventTypeMapping.put(EventType.FESTIVAL, 7);
    }

    @Override
    public List<Event> search(String keywords, String location, Integer radius, Date startDate, Date endDate,
            EventType eventType) throws SearcherException {
        List<Event> events = CollectionHelper.newArrayList();

        String requestUrl = buildRequest(keywords, location, radius, startDate, endDate, eventType);

        String jsonText = new DocumentRetriever().getText(requestUrl);
        JsonObjectWrapper json = new JsonObjectWrapper(jsonText);

        JSONArray eventEntries = JPathHelper.get(json.getJsonObject(), "rsp/event", JSONArray.class);
        if (eventEntries == null) {
            return events;
        }

        for (int i = 0; i < eventEntries.length(); i++) {

            JsonObjectWrapper eventEntry;
            try {
                eventEntry = new JsonObjectWrapper(eventEntries.getJSONObject(i));

                Event event = new Event();
                event.setTitle(eventEntry.getString("name"));
                event.setDescription(HtmlHelper.htmlToReadableText(eventEntry.getString("description")));
                String startDateString = eventEntry.getString("start_date");
                String startTime = eventEntry.getString("start_time");
                String startDateTime = startDateString;
                if (!startTime.isEmpty()) {
                    startDateTime += " " + startTime;
                }
                event.setStartDate(DateParser.parseDate(startDateTime).getNormalizedDate());
                String endDateString = eventEntry.getString("end_date");
                if (endDateString.isEmpty()) {
                    endDateString = startDateString;
                }
                String endTime = eventEntry.getString("end_time");
                if (endTime.length() > 4) {
                    event.setEndDate(DateParser.parseDate(endDateString + " " + endTime).getNormalizedDate());
                }
                event.setUrl(eventEntry.getString("url"));
                event.setVenueName(eventEntry.getString("venue_name"));
                event.setVenueAddress(eventEntry.getString("venue_address"));
                event.setVenueZipCode(eventEntry.getString("venue_zip"));
                event.setVenueCity(eventEntry.getString("venue_city"));
                event.setVenueRegion(eventEntry.getString("venue_state_name"));
                event.setVenueCountry(eventEntry.getString("venue_country_name"));
                event.setVenueLatitude(eventEntry.getDouble("latitude"));
                event.setVenueLongitude(eventEntry.getDouble("longitude"));

                events.add(event);

            } catch (JSONException e) {
                throw new SearcherException(e.getMessage());
            }
        }

        return events;
    }

    private String buildRequest(String keywords, String location, Integer radius, Date startDate, Date endDate,
            EventType eventType) {

        String url = "http://beta.upcoming.yahoo.com/services/rest/?method=event.search&api_key=" + this.apiKey;
        if (keywords != null && !keywords.isEmpty()) {
            url += "&search_text=" + UrlHelper.encodeParameter(keywords);
        }
        if (eventType != null) {
            Integer categoryId = eventTypeMapping.get(eventType);
            if (categoryId != null) {
                url += "&category_id=" + categoryId;
            }
        }

        if (location != null) {
            url += "&location=" + UrlHelper.encodeParameter(location);

            if (radius != null) {
                url += "&radius=" + radius / 1.60934;
            }
        }
        if (startDate != null) {
            url += "&min_date=" + DateHelper.getDatetime("yyyy-MM-dd", startDate.getTime());
        }
        if (endDate != null) {
            url += "&max_date" + DateHelper.getDatetime("yyyy-MM-dd", endDate.getTime());
        }
        url += "&sort=start-date-asc";
        url += "&format=json";

        System.out.println(url);

        return url;
    }

    @Override
    public String getName() {
        return "Upcoming";
    }

    /**
     * @param args
     * @throws SearcherException
     */
    public static void main(String[] args) throws SearcherException {
        UpcomingSearcher searcher = new UpcomingSearcher("GET YOUR OWN");
        Date startDate = new Date();
        Date endDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));
        List<Event> results = searcher.search(null, "Chicago", 10, startDate, endDate, EventType.COMEDY);
        CollectionHelper.print(results);
    }

}
