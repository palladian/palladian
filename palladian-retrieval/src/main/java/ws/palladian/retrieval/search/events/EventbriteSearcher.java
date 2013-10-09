package ws.palladian.retrieval.search.events;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for events on <a href="http://www.eventbrite.com/">Eventbrite</a>.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="http://developer.eventbrite.com/doc/events/event_search/">Eventbrite event search API</a>
 */
public class EventbriteSearcher extends EventSearcher {

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.eventbrite.key";

    private final String apiKey;

    private Map<EventType, Set<String>> eventTypeMapping;

    /**
     * <p>
     * Creates a new Eventbrite searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing Eventbrite, not <code>null</code> or empty.
     */
    public EventbriteSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        setup();
    }

    /**
     * <p>
     * Creates a new Eventbrite searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing Eventbrite, which must be
     *            provided
     *            as string via key {@value EventbriteSearcher#CONFIG_API_KEY} in the configuration.
     */
    public EventbriteSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    private void setup() {
        eventTypeMapping = CollectionHelper.newHashMap();
        eventTypeMapping.put(EventType.CONCERT, new HashSet<String>(Arrays.asList("music")));
        eventTypeMapping.put(EventType.SPORT, new HashSet<String>(Arrays.asList("sports")));
        eventTypeMapping.put(EventType.THEATRE, new HashSet<String>(Arrays.asList("performances")));
        eventTypeMapping.put(EventType.EXHIBITION, new HashSet<String>(Arrays.asList("conventions", "tradeshows")));
        eventTypeMapping.put(EventType.FESTIVAL, new HashSet<String>(Arrays.asList("food")));
        eventTypeMapping.put(EventType.CONFERENCE,
                new HashSet<String>(Arrays.asList("conferences", "seminars", "fairs")));
    }

    @Override
    public List<Event> search(String keywords, String location, Integer radius, Date startDate, Date endDate,
            EventType eventType) throws SearcherException {
        List<Event> events = CollectionHelper.newArrayList();

        String requestUrl = buildRequest(keywords, location, radius, startDate, endDate, eventType);

        JsonObject json = new JsonObject();
        JsonArray eventEntries = new JsonArray();

        try {
            json = new DocumentRetriever().getJsonObject(requestUrl);

            eventEntries = json.getJsonArray("events");
            if (eventEntries == null) {
                return events;
            }
        } catch (Exception e) {
            throw new SearcherException(e.getMessage());
        }
        for (int i = 1; i < eventEntries.size(); i++) {

            JsonObject eventEntry;
            try {
                eventEntry = eventEntries.getJsonObject(i).getJsonObject("event");

                Event event = new Event();
                event.setTitle(eventEntry.getString("title"));
                event.setDescription(HtmlHelper.stripHtmlTags(eventEntry.getString("description")));
                event.setStartDate(DateParser.parseDate(eventEntry.getString("start_date")).getNormalizedDate());
                try {
                    event.setEndDate(DateParser.parseDate(eventEntry.getString("end_date")).getNormalizedDate());
                } catch (Exception e) {
                }
                event.setRecurringString(eventEntry.getString("repeats"));
                event.setUrl(eventEntry.getString("url"));

                JsonObject venueEntry = eventEntry.getJsonObject("venue");

                if (venueEntry == null) {
                    continue;
                }

                event.setVenueName(venueEntry.getString("name"));
                event.setVenueAddress(venueEntry.getString("address"));
                event.setVenueZipCode(venueEntry.getString("postal_code"));
                event.setVenueCity(venueEntry.getString("city"));
                event.setVenueRegion(venueEntry.getString("region"));
                event.setVenueCountry(venueEntry.getString("country"));
                event.setVenueLatitude(venueEntry.getDouble("latitude"));
                event.setVenueLongitude(venueEntry.getDouble("longitude"));

                boolean addEvent = isWithinTimeFrame(startDate, endDate, event);

                if (addEvent) {
                    events.add(event);
                }

            } catch (JsonException e) {
                throw new SearcherException(e.getMessage());
            }
        }

        return events;
    }

    private String buildRequest(String keywords, String location, Integer radius, Date startDate, Date endDate,
            EventType eventType) {

        String url = "https://www.eventbrite.com/json/event_search?app_key=" + this.apiKey;
        if (keywords != null && !keywords.isEmpty()) {
            url += "&keywords=" + UrlHelper.encodeParameter(keywords);
        }

        if (eventType != null) {
            Set<String> categoryIds = eventTypeMapping.get(eventType);
            if (categoryIds != null) {
                url += "&category=" + StringUtils.join(categoryIds, ",");
            }
        }

        if (location != null) {
            url += "&city=" + UrlHelper.encodeParameter(location);

            if (radius != null) {
                url += "&within=" + radius;
                url += "&within_unit=K";
            }
        }
        if (startDate != null) {
            url += "&date=" + DateHelper.getDatetime("yyyy-MM-dd", startDate.getTime());
            if (endDate != null) {
                url += "%20" + DateHelper.getDatetime("yyyy-MM-dd", endDate.getTime());
            }
        }
        url += "&sort_by=date";

        return url;
    }

    @Override
    public String getName() {
        return "Eventbrite";
    }

    /**
     * @param args
     * @throws SearcherException
     */
    public static void main(String[] args) throws SearcherException {
        EventbriteSearcher searcher = new EventbriteSearcher("GET YOUR OWN");
        Date startDate = new Date();
        Date endDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));
        List<Event> results = searcher.search(null, "Chicago", 10, startDate, endDate, EventType.EVENT);
        CollectionHelper.print(results);
    }

}
