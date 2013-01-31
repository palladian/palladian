package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserFactory;

public class GeonamesLocationSource implements LocationSource {

    /** The logger for this class. */
    // private static final Logger LOGGER = LoggerFactory.getLogger(GeonamesLocationSource.class);

    /** Mapping from feature codes from the dataset to LocationType. */
    private static final Map<String, LocationType> FEATURE_MAPPING;

    static {
        // TODO check, whether those mappings make sense
        // http://download.geonames.org/export/dump/featureCodes_en.txt
        // http://www.geonames.org/export/codes.html
        Map<String, LocationType> temp = CollectionHelper.newHashMap();
        temp.put("A", LocationType.UNIT);
        temp.put("A.PCL", LocationType.COUNTRY);
        temp.put("A.PCLD", LocationType.COUNTRY);
        temp.put("A.PCLF", LocationType.COUNTRY);
        temp.put("A.PCLH", LocationType.COUNTRY);
        temp.put("A.PCLI", LocationType.COUNTRY);
        temp.put("A.PCLIX", LocationType.COUNTRY);
        temp.put("A.PCLS", LocationType.COUNTRY);
        temp.put("H", LocationType.LANDMARK);
        temp.put("L", LocationType.POI);
        temp.put("L.AREA", LocationType.REGION);
        temp.put("L.COLF", LocationType.REGION);
        temp.put("L.CONT", LocationType.CONTINENT);
        temp.put("L.RGN", LocationType.REGION);
        temp.put("L.RGNE", LocationType.REGION);
        temp.put("L.RGNH", LocationType.REGION);
        temp.put("L.RGNL", LocationType.REGION);
        temp.put("P", LocationType.CITY);
        temp.put("R", LocationType.POI);
        temp.put("S", LocationType.POI);
        temp.put("T", LocationType.LANDMARK);
        temp.put("U", LocationType.LANDMARK);
        temp.put("U.BDLU", LocationType.REGION);
        temp.put("U.PLNU", LocationType.REGION);
        temp.put("U.PRVU", LocationType.REGION);
        temp.put("V", LocationType.POI);
        FEATURE_MAPPING = Collections.unmodifiableMap(temp);
    }

    private final String username;

    private final DocumentParser xmlParser = ParserFactory.createXmlParser();

    private final HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();

    /** Count the number of executed request since start. */
    public static int requestCount = 0;

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
        String geonameId = XPathHelper.getNode(node, "./geonameId").getTextContent();
        String featureClass = XPathHelper.getNode(node, "./fcl").getTextContent();
        String featureCode = XPathHelper.getNode(node, "./fcode").getTextContent();
        String populationString = XPathHelper.getNode(node, "./population").getTextContent();
        long population = 0;
        if (!populationString.isEmpty()) {
            population = Long.valueOf(populationString);
        }
        Location location = new Location();
        location.setId(Integer.valueOf(geonameId));
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setPopulation(population);
        location.setPrimaryName(primaryName);
        location.setType(mapType(featureClass, featureCode));
        return location;
    }

    static LocationType mapType(String featureClass, String featureCode) {
        // first, try lookup by full feature code (e.g. 'L.CONT')
        LocationType locationType = FEATURE_MAPPING.get(String.format("%s.%s", featureClass, featureCode));
        if (locationType != null) {
            return locationType;
        }
        // second, try lookup only be feature class (e.g. 'A')
        locationType = FEATURE_MAPPING.get(featureClass);
        if (locationType != null) {
            return locationType;
        }
        return LocationType.UNDETERMINED;
    }

    @Override
    public List<Location> getHierarchy(Location location) {
        try {
            String getUrl = String.format("http://api.geonames.org/hierarchy?geonameId=%s&username=%s",
                    location.getId(), username);
            HttpResult httpResult = httpRetriever.httpGet(getUrl);
            requestCount++;
            Document document = xmlParser.parse(httpResult);
            List<Node> geonames = XPathHelper.getNodes(document, "//geoname/geonameId");
            List<Location> result = CollectionHelper.newArrayList();
            for (Node node : geonames) {
                int geonameId = Integer.valueOf(node.getTextContent());
                if (geonameId == location.getId()) { // do not add the supplied Location itself.
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
    public void save(Location location) {
        throw new UnsupportedOperationException("Modifications are not supported.");
    }

    @Override
    public void addHierarchy(int fromId, int toId, String type) {
        throw new UnsupportedOperationException("Modifications are not supported.");
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

    public static void main(String[] args) {
        GeonamesLocationSource locationSource = new GeonamesLocationSource("qqilihq");
        List<Location> locations = locationSource.retrieveLocations("stuttgart");
        CollectionHelper.print(locations);

        System.out.println("-------");

        Location firstLocation = CollectionHelper.getFirst(locations);
        List<Location> hierarchy = locationSource.getHierarchy(firstLocation);
        CollectionHelper.print(hierarchy);
    }

}
