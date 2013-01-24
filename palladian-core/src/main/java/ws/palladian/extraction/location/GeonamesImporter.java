package ws.palladian.extraction.location;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.persistence.DatabaseManagerFactory;

/**
 * <p>
 * This class reads data dumps from Geonames and imports them into a given {@link LocationSource}.
 * </p>
 * 
 * @see <a href="http://download.geonames.org/export/dump/">Geonames dumps</a>
 * @author Philipp Katz
 */
public final class GeonamesImporter {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GeonamesImporter.class);

    /** Mapping from feature codes from the dataset to LocationType. */
    private static final Map<String, LocationType> FEATURE_MAPPING;

    static {
        // TODO check, whether those mappings make sense
        Map<String, LocationType> temp = CollectionHelper.newHashMap();
        temp.put("A", LocationType.UNIT);
        temp.put("H", LocationType.LANDMARK);
        temp.put("L", LocationType.POI);
        temp.put("P", LocationType.CITY);
        temp.put("R", LocationType.POI);
        temp.put("S", LocationType.POI);
        temp.put("T", LocationType.LANDMARK);
        temp.put("U", LocationType.LANDMARK);
        temp.put("V", LocationType.POI);
        FEATURE_MAPPING = Collections.unmodifiableMap(temp);
    }

    /**
     * <p>
     * Import a Geonames dump into the given {@link LocationSource}.
     * </p>
     * 
     * @param filePath The path to the Geonames dump file, not <code>null</code>.
     * @param locationSource The {@link LocationSource} where to store the data, not <code>null</code>.
     */
    public static void importFromGeonames(File filePath, final LocationSource locationSource) {
        Validate.notNull(filePath, "filePath must not be null");
        Validate.notNull(locationSource, "locationSource must not be null");

        if (!filePath.isFile()) {
            throw new IllegalArgumentException(filePath.getAbsolutePath() + " does not exist or is no file");
        }

        final int totalLines = FileHelper.getNumberOfLines(filePath);
        LOGGER.info("Starting import from {}, items to read {}", filePath, totalLines);
        final StopWatch stopWatch = new StopWatch();
        FileHelper.performActionOnEveryLine(filePath.getAbsolutePath(), new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                Location location = parse(line);
                locationSource.save(location);
                LOGGER.info(ProgressHelper.getProgress(lineNumber, totalLines, 1, stopWatch));
            }
        });
    }

    /**
     * <pre>
     *   The main 'geoname' table has the following fields :
     *   ---------------------------------------------------
     *   geonameid         : integer id of record in geonames database
     *   name              : name of geographical point (utf8) varchar(200)
     *   asciiname         : name of geographical point in plain ascii characters, varchar(200)
     *   alternatenames    : alternatenames, comma separated varchar(5000)
     *   latitude          : latitude in decimal degrees (wgs84)
     *   longitude         : longitude in decimal degrees (wgs84)
     *   feature class     : see http://www.geonames.org/export/codes.html, char(1)
     *   feature code      : see http://www.geonames.org/export/codes.html, varchar(10)
     *   country code      : ISO-3166 2-letter country code, 2 characters
     *   cc2               : alternate country codes, comma separated, ISO-3166 2-letter country code, 60 characters
     *   admin1 code       : fipscode (subject to change to iso code), see exceptions below, see file admin1Codes.txt for display names of this code; varchar(20)
     *   admin2 code       : code for the second administrative division, a county in the US, see file admin2Codes.txt; varchar(80) 
     *   admin3 code       : code for third level administrative division, varchar(20)
     *   admin4 code       : code for fourth level administrative division, varchar(20)
     *   population        : bigint (8 byte int) 
     *   elevation         : in meters, integer
     *   dem               : digital elevation model, srtm3 or gtopo30, average elevation of 3''x3'' (ca 90mx90m) or 30''x30'' (ca 900mx900m) area in meters, integer. srtm processed by cgiar/ciat.
     *   timezone          : the timezone id (see file timeZone.txt) varchar(40)
     *   modification date : date of last modification in yyyy-MM-dd format
     * </pre>
     * 
     * @param line The line to parse, not <code>null</code>.
     * @return The parser {@link Location}.
     */
    protected static Location parse(String line) {
        String[] parts = line.split("\\t");
        if (parts.length != 19) {
            throw new IllegalStateException("Exception while parsing, expected 19 elements, but was " + parts.length
                    + "('" + line + "')");
        }
        List<String> alternateNames = CollectionHelper.newArrayList();
        for (String item : parts[3].split(",")) {
            if (item.length() > 0) {
                alternateNames.add(item);
            }
        }
        Location location = new Location();
        location.setLongitude(Double.valueOf(parts[5]));
        location.setLatitude(Double.valueOf(parts[4]));
        location.setPrimaryName(parts[1]);
        location.setAlternativeNames(alternateNames);
        location.setPopulation(Integer.valueOf(parts[14]));
        location.setType(mapType(parts[6]));
        return location;
    }

    private static LocationType mapType(String featureClass) {
        LocationType locationType = FEATURE_MAPPING.get(featureClass);
        if (locationType == null) {
            throw new IllegalArgumentException("Unknown featureClass " + featureClass);
        }
        return locationType;
    }

    private GeonamesImporter() {
        // helper class.
    }

    public static void main(String[] args) {
        LocationDatabase locationSource = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        locationSource.truncate();
        importFromGeonames(new File("/Users/pk/Desktop/LocationLab/geonames.org/DE/DE.txt"), locationSource);
    }

}
