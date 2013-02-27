package ws.palladian.extraction.location.sources;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.importers.GeonamesUtil;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * A {@link LocationSource} witch uses the <a href="http://www.geonames.org">Geonames</a> API. The services provides
 * 2,000 queries/hour, 30,000 queries/day for free accounts.
 * </p>
 * 
 * @see <a href="http://www.geonames.org/login">Account registration</a>
 * @see <a href="http://www.geonames.org/manageaccount">Account activation</a> (enable access to web services after
 *      registration)
 * @author Philipp Katz
 */
public class GeonamesLocationSource implements LocationSource {

    /** The logger for this class. */
    // private static final Logger LOGGER = LoggerFactory.getLogger(GeonamesLocationSource.class);

    private final String username;

    private final DocumentParser xmlParser = ParserFactory.createXmlParser();

    private final HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();

    /** Count the number of executed request since start. */
    public static int requestCount = 0;

    /**
     * <p>
     * Create a new {@link GeonamesLocationSource}.
     * </p>
     * 
     * @param username The signed up user name, not <code>null</code> or empty.
     */
    public GeonamesLocationSource(String username) {
        Validate.notEmpty(username, "username must not be empty");
        this.username = username;
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        try {
            String getUrl = String.format("http://api.geonames.org/search?name_equals=%s&style=LONG&username=%s",
                    UrlHelper.encodeParameter(locationName), username);
            HttpResult httpResult = httpRetriever.httpGet(getUrl);
            requestCount++;
            Document document = xmlParser.parse(httpResult);
            return parseLocations(document);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Location> retrieveLocations(String locationName, EnumSet<Language> languages) {
        throw new UnsupportedOperationException("Searching by languages is not supported by GeoNames.org");
    }

    static List<Location> parseLocations(Document document) {
        checkError(document);
        List<Location> result = CollectionHelper.newArrayList();
        List<Node> geonames = XPathHelper.getNodes(document, "//geoname");
        for (Node node : geonames) {
            Location location = parseLocation(node);
            result.add(location);
        }
        return result;
    }

    /**
     * Check, whether the service sent an error (usually, when the quota has been exceeded).
     * 
     * @param document
     */
    static void checkError(Document document) {
        Node statusNode = XPathHelper.getNode(document, "//geonames/status");
        if (statusNode != null) {
            StringBuilder diagnosticsMessage = new StringBuilder();
            diagnosticsMessage.append("Error from the web service");
            NamedNodeMap attributes = statusNode.getAttributes();
            if (attributes != null) {
                Node message = attributes.getNamedItem("message");
                if (message != null) {
                    diagnosticsMessage.append(": ");
                    diagnosticsMessage.append(message.getTextContent());
                }
            }
            throw new IllegalStateException(diagnosticsMessage.toString());
        }
    }

    static Location parseLocation(Node node) {
        String primaryName = XPathHelper.getNode(node, "./toponymName").getTextContent();
        double latitude = Double.valueOf(XPathHelper.getNode(node, "./lat").getTextContent());
        double longitude = Double.valueOf(XPathHelper.getNode(node, "./lng").getTextContent());
        int geonameId = Integer.valueOf(XPathHelper.getNode(node, "./geonameId").getTextContent());
        String featureClass = XPathHelper.getNode(node, "./fcl").getTextContent();
        String featureCode = XPathHelper.getNode(node, "./fcode").getTextContent();
        String populationString = XPathHelper.getNode(node, "./population").getTextContent();
        long population = 0;
        if (!populationString.isEmpty()) {
            population = Long.valueOf(populationString);
        }
        LocationType locationType = GeonamesUtil.mapType(featureClass, featureCode);
        return new Location(geonameId, primaryName, null, locationType, latitude, longitude, population);
    }

    @Override
    public List<Location> getHierarchy(int locationId) {
        try {
            String getUrl = String.format("http://api.geonames.org/hierarchy?geonameId=%s&username=%s", locationId,
                    username);
            HttpResult httpResult = httpRetriever.httpGet(getUrl);
            requestCount++;
            Document document = xmlParser.parse(httpResult);
            List<Node> geonames = XPathHelper.getNodes(document, "//geoname/geonameId");
            List<Location> result = CollectionHelper.newArrayList();
            for (Node node : geonames) {
                int geonameId = Integer.valueOf(node.getTextContent());
                if (geonameId == locationId) { // do not add the supplied Location itself.
                    continue;
                }
                Location retrievedLocation = retrieveLocation(geonameId);
                result.add(retrievedLocation);
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Location retrieveLocation(int locationId) {
        try {
            String getUrl = String.format("http://api.geonames.org/get?geonameId=%s&username=%s&style=LONG",
                    locationId, username);
            HttpResult httpResult = httpRetriever.httpGet(getUrl);
            requestCount++;
            Document document = xmlParser.parse(httpResult);
            List<Location> locations = parseLocations(document);
            return CollectionHelper.getFirst(locations);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Collection<LocationRelation> getParents(int locationId) {
        throw new UnsupportedOperationException("Not supported by GeoNames.org");
    }

    public static void main(String[] args) {
        GeonamesLocationSource locationSource = new GeonamesLocationSource("qqilihq");
        List<Location> locations = locationSource.retrieveLocations("stuttgart");
        CollectionHelper.print(locations);

        System.out.println("-------");

        Location firstLocation = CollectionHelper.getFirst(locations);
        List<Location> hierarchy = locationSource.getHierarchy(firstLocation.getId());
        CollectionHelper.print(hierarchy);
    }

}
