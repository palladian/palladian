package ws.palladian.retrieval.search.events;

import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for events on <a href="http://www.eventful.com/">eventful</a>.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="http://api.eventful.com/docs/events/search">eventful event search API</a>
 */
public class EventfulSearcher extends EventSearcher {

    /** The logger for named entity recognizer classes. */
    protected static final Logger LOGGER = LoggerFactory.getLogger(EventfulSearcher.class);

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.eventful.key";

    private final String apiKey;

    /**
     * <p>
     * Creates a new eventful searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing eventful, not <code>null</code> or empty.
     */
    public EventfulSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
    }

    /**
     * <p>
     * Creates a new eventful searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing eventful, which must be
     *            provided
     *            as string via key {@value EventfulSearcher#CONFIG_API_KEY} in the configuration.
     */
    public EventfulSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public List<Event> search(String keywords, String location, Integer radius, Date startDate, Date endDate,
            EventType eventType) throws SearcherException {
        List<Event> events = CollectionHelper.newArrayList();

        String requestUrl = buildRequest(keywords, location, radius, startDate, endDate, eventType);

        Document resultDocument = new DocumentRetriever().getWebDocument(requestUrl);

        List<Node> eventNodes = XPathHelper.getXhtmlNodes(resultDocument, "//event");
        for (Node eventNode : eventNodes) {

            try {
                Event event = new Event();
                event.setTitle(getField(eventNode, "title"));
                event.setDescription(getField(eventNode, "description"));
                event.setStartDate(DateParser.parseDate(getField(eventNode, "start_time")).getNormalizedDate());
                event.setRecurringString(getField(eventNode, "recur_string"));
                event.setUrl(getField(eventNode, "url"));
                event.setVenueName(getField(eventNode, "venue_name"));
                event.setVenueAddress(getField(eventNode, "venue_address"));
                event.setVenueZipCode(getField(eventNode, "postal_code"));
                event.setVenueCity(getField(eventNode, "city_name"));
                event.setVenueRegion(getField(eventNode, "region_name"));
                event.setVenueCountry(getField(eventNode, "country_name"));
                event.setVenueLatitude(Double.valueOf(getField(eventNode, "latitude")));
                event.setVenueLongitude(Double.valueOf(getField(eventNode, "longitude")));

                events.add(event);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }

        return events;
    }

    private String getField(Node node, String name) {
        String field = "";
        try {
            field = XPathHelper.getXhtmlNode(node, ".//" + name).getTextContent();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return field;
    }

    private String buildRequest(String keywords, String location, Integer radius, Date startDate, Date endDate,
            EventType eventType) {

        String url = "http://api.eventful.com/rest/events/search?app_key=" + this.apiKey;
        if (keywords != null && !keywords.isEmpty()) {
            url += "&keywords=" + UrlHelper.encodeParameter(keywords);
        }
        if (location != null) {
            url += "&location=" + UrlHelper.encodeParameter(location);

            if (radius != null) {
                url += "&within=" + radius;
                url += "&units=km";
            }
        }
        if (startDate != null) {
            url += "&date=" + DateHelper.getDatetime("yyyyMMdd00", startDate.getTime());
            if (endDate != null) {
                url += "-" + DateHelper.getDatetime("yyyyMMdd00", endDate.getTime());
            }
        }
        url += "&sort_order=date";
        url += "&page_size=30";
        url += "&sort_direction=ascending";

        System.out.println(url);

        return url;
    }

    @Override
    public String getName() {
        return "Eventful";
    }

    /**
     * @param args
     * @throws SearcherException
     */
    public static void main(String[] args) throws SearcherException {
        // EventfulSearcher searcher = new EventfulSearcher("GET YOUR OWN");
        // List<Event> results = searcher.search("jazz", "Chicago", 10, "November 2013", "music");
        // CollectionHelper.print(results);
    }

}
