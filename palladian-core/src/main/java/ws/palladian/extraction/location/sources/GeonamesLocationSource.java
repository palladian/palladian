package ws.palladian.extraction.location.sources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.sources.importers.GeonamesUtil;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;
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
 * @see <a href="http://www.geonames.org/export/web-services.html">Web Service documentation</a>
 * @author Philipp Katz
 */
public class GeonamesLocationSource extends SingleQueryLocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GeonamesLocationSource.class);

    private final String username;

    private final DocumentParser xmlParser = ParserFactory.createXmlParser();

    private final HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();

    private final boolean retrieveHierarchy;

    /**
     * <p>
     * Create a new {@link GeonamesLocationSource} which caches requests.
     * </p>
     *
     * @param username The signed up user name, not <code>null</code> or empty.
     * @return A new {@link GeonamesLocationSource} with caching.
     */
    public static LocationSource newCachedLocationSource(String username) {
        return newCachedLocationSource(username, true);
    }

    /**
     * <p>
     * Create a new {@link GeonamesLocationSource} which caches requests.
     * </p>
     *
     * @param username The signed up user name, not <code>null</code> or empty.
     * @param retrieveHierarchy <code>true</code> to retrieve hierarchy information (which causes additional REST
     *            requests).
     * @return A new {@link GeonamesLocationSource} with caching.
     */
    public static LocationSource newCachedLocationSource(String username, boolean retrieveHierarchy) {
        return new CachingLocationSource(new ParallelizedRequestLocationSource(new GeonamesLocationSource(username,
                retrieveHierarchy), 10));
    }

    /**
     * <p>
     * Create a new {@link GeonamesLocationSource}.
     * </p>
     *
     * @param username The signed up user name, not <code>null</code> or empty.
     * @deprecated Prefer using the cached variant, which can be obtained via {@link #newCachedLocationSource(String)}.
     */
    @Deprecated
    public GeonamesLocationSource(String username) {
        this(username, true);
    }

    /**
     * <p>
     * Create a new {@link GeonamesLocationSource}.
     * </p>
     *
     * @param username The signed up user name, not <code>null</code> or empty.
     * @param retrieveHierarchy <code>true</code> to retrieve hierarchy information (which causes additional REST
     *            requests).
     * @deprecated Prefer using the cached variant, which can be obtained via {@link #newCachedLocationSource(String)}.
     */
    public GeonamesLocationSource(String username, boolean retrieveHierarchy) {
        Validate.notEmpty(username);
        this.username = username;
        this.retrieveHierarchy = retrieveHierarchy;
    }

    @Override
    public List<Location> getLocations(String locationName, Set<Language> languages) {
        try {
            String cleanName = StringUtils.stripAccents(locationName.toLowerCase());
            String getUrl = String.format("http://api.geonames.org/search?name_equals=%s&style=FULL&username=%s",
                    UrlHelper.encodeParameter(cleanName), username);
            HttpResult httpResult = httpRetriever.httpGet(getUrl);
            Document document = xmlParser.parse(httpResult);
            List<Location> result = new ArrayList<>();
            List<Location> retrievedLocations = parseLocations(document);
            for (Location retrievedLocation : retrievedLocations) {
                // post-filtering; only return those locations which actually match the specified languages;
                // this is done here, because GeoNames does not allow to narrow down queries by (multiple) languages.
                if (retrievedLocation.hasName(locationName, languages)) {
                    List<Integer> hierarchy = getHierarchy(retrievedLocation.getId());
                    Location location = new ImmutableLocation(retrievedLocation,
                            retrievedLocation.getAlternativeNames(), hierarchy);
                    result.add(location);
                } else {
                    LOGGER.debug("Dropping {} because name does not match", retrievedLocation);
                }
            }
            return result;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static List<Location> parseLocations(Document document) {
        checkError(document);
        List<Location> result = new ArrayList<>();
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
        LocationBuilder builder = new LocationBuilder();
        builder.setPrimaryName(XPathHelper.getNode(node, "./toponymName").getTextContent());
        double latitude = Double.parseDouble(XPathHelper.getNode(node, "./lat").getTextContent());
        double longitude = Double.parseDouble(XPathHelper.getNode(node, "./lng").getTextContent());
        builder.setCoordinate(latitude, longitude);
        builder.setId(Integer.parseInt(XPathHelper.getNode(node, "./geonameId").getTextContent()));
        String featureClass = XPathHelper.getNode(node, "./fcl").getTextContent();
        String featureCode = XPathHelper.getNode(node, "./fcode").getTextContent();
        builder.setType(GeonamesUtil.mapType(featureClass, featureCode));
        String populationString = XPathHelper.getNode(node, "./population").getTextContent();
        if (!populationString.isEmpty()) {
            builder.setPopulation(Long.parseLong(populationString));
        }
        List<Node> altNameNodes = XPathHelper.getNodes(node, "./alternateName");
        Set<String> catchedNames = new HashSet<>();
        for (Node altNameNode : altNameNodes) {
            String altName = altNameNode.getTextContent();
            catchedNames.add(altName);
            NamedNodeMap attrs = altNameNode.getAttributes();
            Node langAttr = attrs.getNamedItem("lang");
            String altNameLang = null;
            Language language = null;
            if (langAttr != null) {
                altNameLang = langAttr.getTextContent();
                language = Language.getByIso6391(altNameLang);
            }
            if (altNameLang == null || language != null || "abbr".equalsIgnoreCase(altNameLang)) {
                builder.addAlternativeName(altName, language);
            }
        }
        // In addition to the alternateName tag, alternative names are also provided through on alternateNames (notice
        // plural!) as comma-separated list. Here we only add, what was not already added through the single
        // alternateName tags.
        Node altNamesNode = XPathHelper.getNode(node, "./alternateNames");
        if (altNamesNode != null) {
            String altNamesString = altNamesNode.getTextContent();
            if (!altNamesString.isEmpty()) {
                String[] altNamesSplit = altNamesString.split(",");
                for (String altName : altNamesSplit) {
                    if (!catchedNames.contains(altName)) {
                        builder.addAlternativeName(altName, null);
                    }
                }
            }
        }
        return builder.create();
    }

    private List<Integer> getHierarchy(int locationId) {
        if (!retrieveHierarchy) {
            return Collections.emptyList();
        }
        try {
            String getUrl = String.format("http://api.geonames.org/hierarchy?geonameId=%s&style=SHORT&username=%s",
                    locationId, username);
            HttpResult httpResult = httpRetriever.httpGet(getUrl);
            Document document = xmlParser.parse(httpResult);
            List<Node> geonames = XPathHelper.getNodes(document, "//geoname/geonameId");
            List<Integer> result = new ArrayList<>();
            for (int i = geonames.size() - 1; i >= 0; i--) {
                Node node = geonames.get(i);
                int geonameId = Integer.parseInt(node.getTextContent());
                if (geonameId == locationId) { // do not add the supplied Location itself.
                    continue;
                }
                result.add(geonameId);
            }
            return result;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    @Override
    public Location getLocation(int locationId) {
        try {
            String getUrl = String.format("http://api.geonames.org/get?geonameId=%s&username=%s&style=FULL",
                    locationId, username);
            HttpResult httpResult = httpRetriever.httpGet(getUrl);
            Document document = xmlParser.parse(httpResult);
            List<Location> locations = parseLocations(document);
            Location location = CollectionHelper.getFirst(locations);
            List<Integer> hierarchy = getHierarchy(locationId);
            return new ImmutableLocation(location, location.getAlternativeNames(), hierarchy);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    @Override
    public List<Location> getLocations(GeoCoordinate coordinate, double distance) {
        try {
            String getUrl = String.format(
                    "http://api.geonames.org/findNearby?lat=%s&lng=%s&radius=%s&username=%s&style=FULL&maxRows=100",
                    coordinate.getLatitude(), coordinate.getLongitude(), distance, username);
            LOGGER.debug("Retrieving {}", getUrl);
            HttpResult httpResult = httpRetriever.httpGet(getUrl);
            Document document = xmlParser.parse(httpResult);
            List<Location> locations = parseLocations(document);
            List<Location> result = new ArrayList<>();
            for (Location location : locations) {
                List<Integer> hierarchy = getHierarchy(location.getId());
                result.add(new ImmutableLocation(location, location.getAlternativeNames(), hierarchy));
            }
            return result;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    public static void main(String[] args) {
        GeonamesLocationSource locationSource = new GeonamesLocationSource("qqilihq");
        // Location location = locationSource.getLocation(7268814);
        // System.out.println(location);
        // List<Location> locations = locationSource.getLocations(new ImmutableGeoCoordinate(52.52, 13.41), 10);
        // List<Location> locations = locationSource.getLocations("U.S.", EnumSet.of(Language.ENGLISH));
        // List<Location> locations = locationSource.getLocations("Liége", EnumSet.of(Language.ENGLISH));
        List<Location> locations = locationSource.getLocations("Ceske Budejovice", EnumSet.of(Language.ENGLISH));
        CollectionHelper.print(locations);
    }

}
