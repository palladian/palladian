package ws.palladian.retrieval.search.events;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateParser;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for events on <a href="http://www.evently.com/">evently</a>.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="http://event.ly/api">evently API</a>
 */
public class EventlySearcher extends EventSearcher {

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.evently.key";

    private final String apiKey;

    private Map<EventType, Integer> eventTypeMapping;

    /**
     * <p>
     * Creates a new evently searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing evently, not <code>null</code> or empty.
     */
    public EventlySearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        setup();
    }

    /**
     * <p>
     * FIXME Creates a new evently searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing evently, which must be
     *            provided
     *            as string via key {@value EventlySearcher#CONFIG_API_KEY} in the configuration.
     */
    public EventlySearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
        setup();
    }

    private void setup() {
        eventTypeMapping = CollectionHelper.newHashMap();
        eventTypeMapping.put(EventType.CONCERT, 1);
        eventTypeMapping.put(EventType.COMEDY, 2);
        eventTypeMapping.put(EventType.THEATRE, 4);
        eventTypeMapping.put(EventType.EXHIBITION, 5);
        eventTypeMapping.put(EventType.FESTIVAL, 7);
    }

    @Override
    public List<Event> search(String keywords, String location, Integer radius, Date startDate, Date endDate,
            EventType eventType, int maxResults) throws SearcherException {
        List<Event> events = CollectionHelper.newArrayList();

        String requestUrl = buildRequest(keywords, location, eventType);

        JsonArray eventEntries;
        try {
            eventEntries = new JsonArray(new DocumentRetriever().getText(requestUrl));

            for (int i = 0; i < eventEntries.size(); i++) {

                JsonObject eventEntry = eventEntries.getJsonObject(i);

                Event event = new Event();
                event.setTitle(eventEntry.getString("name"));
                event.setDescription(eventEntry.getString("long_description"));
                event.setStartDate(DateParser.parseDate(eventEntry.getString("time")).getNormalizedDate());
                event.setEndDate(DateParser.parseDate(eventEntry.getString("end_time")).getNormalizedDate());
                event.setUrl(eventEntry.getString("event_page_url"));
                if (event.getUrl() == null) {
                    event.setUrl(eventEntry.getString("web_url"));
                }
                JsonObject venueEntry = eventEntry.getJsonObject("venue");
                event.setVenueName(venueEntry.getString("name"));
                event.setVenueAddress(venueEntry.getString("street"));
                event.setVenueZipCode(venueEntry.getString("postcode"));
                event.setVenueCity(venueEntry.getString("city"));
                event.setVenueRegion(venueEntry.getString("area"));
                event.setVenueCountry(venueEntry.getString("country"));
                event.setVenueLatitude(Double.valueOf(venueEntry.getString("lat")));
                event.setVenueLongitude(Double.valueOf(venueEntry.getString("lng")));

                // XXX the API does not consider the city when searching events
                if (event.getVenueCity().equalsIgnoreCase(location)) {
                    events.add(event);
                }

                if (events.size() >= maxResults) {
                    break;
                }
            }

        } catch (JsonException e) {
            throw new SearcherException(e.getMessage());
        }

        return events;
    }

    private String buildRequest(String keywords, String location, EventType eventType) {

        String url = "http://api.event.ly/v3/";

        Integer genreId = eventTypeMapping.get(eventType);
        if (genreId != null) {
            url += "genres/" + genreId + "/";
        }

        url += "events.json?api_key=" + this.apiKey;
        if (keywords != null && !keywords.isEmpty()) {
            url += "&name=" + UrlHelper.encodeParameter(keywords);
        }
        if (location != null) {
            url += "&city=" + UrlHelper.encodeParameter(location);
        }

        return url;
    }

    @Override
    public String getName() {
        return "evently";
    }

    /**
     * @param args
     * @throws SearcherException
     */
    public static void main(String[] args) throws SearcherException {
        EventlySearcher searcher = new EventlySearcher("GET YOUR OWN");
        List<Event> results = searcher.search("funny", "London", null, null, null, EventType.EVENT);
        CollectionHelper.print(results);
    }

}
