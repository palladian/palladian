package ws.palladian.retrieval.search.events;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
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

    private Map<EventType, Set<String>> eventTypeMapping;

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
        setup();
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

    private void setup() {
        eventTypeMapping = CollectionHelper.newHashMap();
        eventTypeMapping.put(EventType.CONCERT, new HashSet<String>(Arrays.asList("music")));
        eventTypeMapping.put(EventType.COMEDY, new HashSet<String>(Arrays.asList("movies_film", "performing_arts")));
        eventTypeMapping.put(EventType.SPORT, new HashSet<String>(Arrays.asList("sports")));
        eventTypeMapping.put(EventType.THEATRE, new HashSet<String>(Arrays.asList("performing_arts")));
        eventTypeMapping.put(EventType.MOVIE, new HashSet<String>(Arrays.asList("movies_film")));
        eventTypeMapping.put(EventType.EXHIBITION, new HashSet<String>(Arrays.asList("art")));
        eventTypeMapping.put(EventType.FESTIVAL, new HashSet<String>(Arrays.asList("festivals_parades", "food")));
        eventTypeMapping.put(EventType.CONFERENCE, new HashSet<String>(Arrays.asList("conference")));
    }

    @Override
    public List<Event> search(String keywords, String location, Integer radius, Date startDate, Date endDate,
            EventType eventType, int maxResults) throws SearcherException {

        List<Event> events = CollectionHelper.newArrayList();

        String requestUrl = buildRequest(keywords, location, radius, startDate, endDate, eventType);
        requestUrl += "&page_number=PAGE_NUMBER";

        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        httpRetriever.setMaxFileSize(SizeUnit.KILOBYTES.toBytes(250));
        DocumentRetriever ret = new DocumentRetriever(httpRetriever);

        int currentPageNumber = 1;
        boolean nextPageAvailable = true;
        while (nextPageAvailable && events.size() < maxResults) {

            nextPageAvailable = false;

            Document resultDocument = ret.getWebDocument(requestUrl.replace("PAGE_NUMBER",
                    String.valueOf(currentPageNumber)));

            List<Node> eventNodes = XPathHelper.getXhtmlNodes(resultDocument, "//event");
            for (Node eventNode : eventNodes) {

                try {
                    Event event = new Event();
                    event.setTitle(getField(eventNode, "title"));
                    event.setDescription(getField(eventNode, "description"));
                    event.setStartDate(DateParser.parseDate(getField(eventNode, "start_time")).getNormalizedDate());
                    try {
                        event.setEndDate(DateParser.parseDate(getField(eventNode, "stop_time")).getNormalizedDate());
                    } catch (Exception e) {
                    }
                    event.setRecurringString(getField(eventNode, "recur_string"));
                    event.setUrl(getField(eventNode, "url"));

                    String venueName = getField(eventNode, "venue_name");
                    if (venueName.isEmpty()) {
                        continue;
                    }
                    event.setVenueName(venueName);
                    event.setVenueAddress(getField(eventNode, "venue_address"));
                    event.setVenueZipCode(getField(eventNode, "postal_code"));
                    event.setVenueCity(getField(eventNode, "city_name"));
                    event.setVenueRegion(getField(eventNode, "region_name"));
                    event.setVenueCountry(getField(eventNode, "country_name"));
                    event.setVenueLatitude(Double.parseDouble(getField(eventNode, "latitude")));
                    event.setVenueLongitude(Double.parseDouble(getField(eventNode, "longitude")));

                    boolean addEvent = isWithinTimeFrame(startDate, endDate, event);

                    if (addEvent) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    LOGGER.warn(e.getMessage());
                }
            }

            // see if there are more pages
            Node pageCountNode = XPathHelper.getXhtmlNode(resultDocument, "//page_count");
            if (pageCountNode != null) {
                int totalPages = Integer.parseInt(pageCountNode.getTextContent());
                if (currentPageNumber < totalPages) {
                    currentPageNumber++;
                    nextPageAvailable = true;
                }
            }

        }

        return events;
    }

    private String getField(Node node, String name) {
        String field = "";
        try {
            field = XPathHelper.getXhtmlNode(node, ".//" + name).getTextContent();

            // XXX for some reason UTF-8 answer is interpreted as ISO-8859-1, making some characters go insane, see here
            // for more: http://ask-leo.com/why_do_i_get_odd_characters_instead_of_quotes_in_my_documents.html
            field = field.replace("â€™", "'");
            field = field.replace("Â ", "");
            field = field.replace("Ã©", "é");
            field = field.replace("â€¦", "...");
            field = field.replace("â€“", "-");
            field = field.replace("â€œ", "“");

            // get rid of the rest non-ascii
            field = StringHelper.removeNonAsciiCharacters(field);

        } catch (Exception e) {
            // HtmlHelper.printDom(node);
            // System.out.println(HtmlHelper.getInnerXml(node));
            LOGGER.warn(e.getMessage());
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
        if (eventType != null) {
            Set<String> categoryIds = eventTypeMapping.get(eventType);
            if (categoryIds != null) {
                url += "&category=" + StringUtils.join(categoryIds, ",");
            }
        }
        if (startDate != null) {
            url += "&date=" + DateHelper.getDatetime("yyyyMMdd00", startDate.getTime());
            if (endDate != null) {
                url += "-" + DateHelper.getDatetime("yyyyMMdd00", endDate.getTime());
            }
        }
        url += "&sort_order=date";
        url += "&page_size=50";
        url += "&sort_direction=ascending";

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
