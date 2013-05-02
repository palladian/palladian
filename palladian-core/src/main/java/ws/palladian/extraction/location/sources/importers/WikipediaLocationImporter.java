package ws.palladian.extraction.location.sources.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

/**
 * <p>
 * Import locations from <a href="http://www.wikipedia.org">Wikipedia</a> pages.
 * </p>
 * 
 * @see <a href="http://dumps.wikimedia.org/enwiki/latest/">English dumps</a>
 * @see <a href="http://en.wikipedia.org/wiki/Wikipedia:Obtaining_geographic_coordinates">Link 1</a>
 * @see <a href="http://en.wikipedia.org/wiki/Wikipedia:WikiProject_Geographical_coordinates">Link 2</a>
 * @author Philipp Katz
 */
public class WikipediaLocationImporter implements WikipediaPageCallback {

    // TODO get alternative names by redirects

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaLocationImporter.class);

    /**
     * matcher for coordinate template: {{Coord|47|33|27|N|10|45|00|E|display=title}}
     */
    private static final Pattern COORDINATE_TAG_PATTERN = Pattern.compile("\\{\\{Coord" + //
            // match latitude, either DMS or decimal, N/S is optional
            "\\|(-?\\d+(?:\\.\\d+)?)(?:\\|(\\d+(?:\\.\\d+)?)(?:\\|(\\d+(?:\\.\\d+)?))?)?(?:\\|([NS]))?" +
            // ..-(1)--------------.......-(2)------------........-(3)---------------.........-(4)--
            // match longitude, either DMS or decimal, W/E is optional
            "\\|(-?\\d+(?:\\.\\d+)?)(?:\\|(\\d+(?:\\.\\d+)?)(?:\\|(\\d+(?:\\.\\d+)?))?)?(?:\\|([WE]))?" +
            // ..-(5)--------------.......-(6)--------------......-(7)---------------.........-(8)--
            // additional data
            "((?:\\|[^}|]+)*)" + //
            // -(9)-----------
            "\\}\\}", Pattern.CASE_INSENSITIVE);//

//    private static final String LATD = "latd|lat_deg|lat_d|lat_degrees";
//    private static final String LATM = "latm|lat_min|lat_m|lat_minutes";
//    private static final String LATS = "lats|lat_sec|lat_s|lat_seconds";
//    private static final String LATNS = "latNS|lat_NS|lat_direction";
//    private static final String LNGD = "longd|long_deg|long_d|long_degrees|lond|lon_deg|long_d|lon_degrees";
//    private static final String LNGM = "longm|long_min|long_m|long_minutes|lonm|lon_min|lon_m|lon_minutes";
//    private static final String LNGS = "longs|long_sec|long_s|long_seconds|lons|lon_sec|lon_s|lon_seconds";
//    private static final String LNGEW = "longEW|long_EW|long_direction|lonEW|lon_EW|lon_direction";

//    /**
//     * matcher for info box content:
//     * | latd = 37
//     * | latm = 47
//     * | latNS = N
//     * | longd = 122
//     * | longm = 25
//     * | longEW = W
//     */
//    private static final Pattern INFOBOX_PATTERN = Pattern.compile("" + //
//            "\\|\\s*(?:" + LATD + ")\\s*=\\s*(-?\\d+(?:\\.\\d+)?)" + //
//            "(?:\\s*\\|\\s*(?:" + LATM + ")\\s*=\\s*(\\d+(?:\\.\\d+)?))?" + //
//            "(?:\\s*\\|\\s*(?:" + LATS + ")\\s*=\\s*(\\d+(?:\\.\\d+)?))?" + //
//            "(?:\\s*\\|\\s*(?:" + LATNS + ")(?:\\s*=\\s*([NS])))?" + //
//            "\\s*\\|\\s*(?:" + LNGD + ")\\s*=\\s*(-?\\d+(?:\\.\\d+)?)" + //
//            "(?:\\s*\\|\\s*(?:" + LNGM + ")\\s*=\\s*(\\d+(?:\\.\\d+)?))?" + //
//            "(?:\\s*\\|\\s*(?:" + LNGS + ")\\s*=\\s*(\\d+(?:\\.\\d+)?))?" + //
//            "(?:\\s*\\|\\s*(?:" + LNGEW + ")(?:\\s*=\\s*([EW])))?");

    /** Pages with those titles will be ignored. */
    private static final Pattern IGNORED_PAGES = Pattern.compile("(?:Geography|Battle) of .*");

    /** The mapping between Wikipedia types and Palladian {@link LocationType}. Values mapped to null will be dropped. */
    private static final Map<String, LocationType> TYPE_MAPPING = getTypeMapping();

    private static Map<String, LocationType> getTypeMapping() {
        Map<String, LocationType> map = CollectionHelper.newHashMap();
        map.put("adm1st", LocationType.UNIT);
        map.put("adm2nd", LocationType.UNIT);
        map.put("adm3rd", LocationType.UNIT);
        map.put("airport", LocationType.POI);
        map.put("city", LocationType.CITY);
        map.put("country", LocationType.COUNTRY);
        map.put("edu", LocationType.POI);
        map.put("event", null);
        map.put("forest", LocationType.LANDMARK);
        map.put("glacier", LocationType.LANDMARK);
        map.put("isle", LocationType.LANDMARK);
        map.put("landmark", LocationType.POI); // XXX not sure, whether this is accurate
        map.put("mountain", LocationType.LANDMARK);
        map.put("pass", LocationType.LANDMARK);
        map.put("railwaystation", LocationType.POI);
        map.put("river", LocationType.LANDMARK);
        map.put("satellite", null);
        map.put("waterbody", LocationType.LANDMARK);
        map.put("camera", null);
        return Collections.unmodifiableMap(map);
    }

    private final LocationStore locationStore;

    // private int id;

    private int pageCounter;
    private StopWatch stopWatch;

    public WikipediaLocationImporter(LocationStore locationStore) {
        Validate.notNull(locationStore, "locationStore must not be null");
        this.locationStore = locationStore;
    }

    /**
     * 
     * @param dumpXml
     */
    public void importDump(File dumpXml) {
        InputStream in = null;
        try {
            in = new FileInputStream(dumpXml);
            importDump(in);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } finally {
            FileHelper.close(in);
        }
    }

    public void importDump(InputStream inputStream) {
        // id = locationStore.getHighestId() + 1;
        pageCounter = 0;
        stopWatch = new StopWatch();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(inputStream, new WikipediaPageContentHandler(this));
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        } catch (SAXException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void callback(WikipediaPage page) {
        if (++pageCounter % 1000 == 0) {
            float throughput = (float)pageCounter / TimeUnit.MILLISECONDS.toSeconds(stopWatch.getElapsedTime());
            LOGGER.info("Processed {} pages, throughput {} pages/second.", pageCounter, Math.round(throughput));
        }
        if (page.getNamespaceId() != 0) {
            return;
        }
        if (IGNORED_PAGES.matcher(page.getTitle()).matches()) {
            LOGGER.info("Ignoring '{}' by blacklist", page.getTitle());
            return;
        }

        String text = page.getText();
        List<MarkupLocation> locations = extractCoordinateTag(text);

        for (MarkupLocation location : locations) {
            if (location.display != null && location.display.contains("title")) {
                String name = cleanName(page.getTitle());

                LocationType type = LocationType.UNDETERMINED;
                if (location.type != null) {
                    type = TYPE_MAPPING.get(location.type);
                    if (type == null) { // no mapping
                        LOGGER.warn("Unmapped type '{}' for '{}'; ignore", location.type, page.getTitle());
                        continue;
                    }
                }

                locationStore.save(new ImmutableLocation(page.getPageId(), name, type, location.lat, location.lng,
                        location.population));
            }
        }
    }

    static String cleanName(String name) {
        String clean = name.replaceAll("\\s\\([^)]*\\)", "");
        clean = clean.replaceAll(",.*", "");
        return clean;
    }

//    static boolean extractInfobox(String text) {
//        Matcher m = INFOBOX_PATTERN.matcher(text);
//        while (m.find()) {
//            double decLat = parseComponents(m.group(1), m.group(2), m.group(3), m.group(4));
//            double decLng = parseComponents(m.group(5), m.group(6), m.group(7), m.group(8));
//            System.out.println(m.group());
//            System.out.println(decLat + ", " + decLng);
//            return true;
//        }
//        return false;
//    }

    static List<MarkupLocation> extractCoordinateTag(String text) {
        List<MarkupLocation> result = CollectionHelper.newArrayList();
        Matcher m = COORDINATE_TAG_PATTERN.matcher(text);
        while (m.find()) {
            MarkupLocation coordMarkup = new MarkupLocation();
            coordMarkup.lat = parseComponents(m.group(1), m.group(2), m.group(3), m.group(4));
            coordMarkup.lng = parseComponents(m.group(5), m.group(6), m.group(7), m.group(8));

            // get coordinate parameters
            String type = getCoordinateParam(m.group(9), "type");
            if (type != null) {
                coordMarkup.population = getNumberInBrackets(type);
                type = type.replaceAll("\\(.*\\)", ""); // remove population
            }
            coordMarkup.type = type;
            coordMarkup.region = getCoordinateParam(m.group(9), "region");
            // get other parameters
            coordMarkup.display = getOtherParam(m.group(9), "display");
            coordMarkup.name = getOtherParam(m.group(9), "name");

            result.add(coordMarkup);
        }
        return result;
    }

    private static Long getNumberInBrackets(String string) {
        Matcher matcher = Pattern.compile("\\(([\\d,]+)\\)").matcher(string);
        if (matcher.find()) {
            String temp = matcher.group(1).replace(",", "");
            try {
                return Long.valueOf(temp);
            } catch (NumberFormatException e) {
                LOGGER.error("Error parsing {}", temp);
            }
        }
        return null;
    }

    private static String getOtherParam(String group, String name) {
        String[] parts = group.split("\\|");
        for (String temp1 : parts) {
            String[] keyValue = temp1.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(name)) {
                return keyValue[1].trim();
            }
        }
        return null;
    }

    private static String getCoordinateParam(String group, String name) {
        String[] parts = group.split("\\|");
        for (String temp1 : parts) {
            for (String temp2 : temp1.split("_")) {
                String[] keyValue = temp2.split(":");
                if (keyValue.length == 2 && keyValue[0].equals(name)) {
                    return keyValue[1].trim();
                }
            }
        }
        return null;
    }

    /**
     * Parse DMS components. The only part which must not be <code>null</code> is deg.
     * 
     * @param deg Degree part, not <code>null</code>.
     * @param min Minute part, may be <code>null</code>.
     * @param sec Second part, may be <code>null</code>.
     * @param nsew NSEW modifier, should be in [NSEW], may be <code>null</code>.
     * @return Parsed double value.
     */
    private static double parseComponents(String deg, String min, String sec, String nsew) {
        Validate.notNull(deg, "deg must not be null");
        double parsedDeg = Double.valueOf(deg);
        double parsedMin = min != null ? Double.valueOf(min) : 0;
        double parsedSec = sec != null ? Double.valueOf(sec) : 0;
        int sgn = ("S".equals(nsew) || "W".equals(nsew)) ? -1 : 1;
        return sgn * (parsedDeg + parsedMin / 60. + parsedSec / 3600.);
    }

    /**
     * Utility class representing a location extracted from Wikipedia coordinate markup.
     */
    static final class MarkupLocation {
        double lat;
        double lng;
        Long population;
        String display;
        String name;
        String type;
        String region;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("MarkupLocation [lat=");
            builder.append(lat);
            builder.append(", lng=");
            builder.append(lng);
            builder.append(", population=");
            builder.append(population);
            builder.append(", display=");
            builder.append(display);
            builder.append(", name=");
            builder.append(name);
            builder.append(", type=");
            builder.append(type);
            builder.append(", region=");
            builder.append(region);
            builder.append("]");
            return builder.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        LocationDatabase locationStore = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        locationStore.truncate();

        WikipediaLocationImporter importer = new WikipediaLocationImporter(locationStore);
        // String fileName = "/Users/pk/Downloads/enwiki-20130403-pages-meta-current1.xml-p000000010p000010000.bz2";
        String fileName = "/Users/pk/Downloads/enwiki-latest-pages-articles.xml.bz2";
        InputStream in = new FileInputStream(new File(fileName));
        // BZip2CompressorInputStream bzipIn = new BZip2CompressorInputStream(in);
        InputStream bzipIn = new MultiStreamBZip2InputStream(in);
        importer.importDump(bzipIn);
    }

}
