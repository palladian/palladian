package ws.palladian.extraction.location;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
 * This class reads data dumps from Geonames (usually you want to take the file "allCountries.zip") and imports them
 * into a given {@link LocationSource}.
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
        // http://download.geonames.org/export/dump/featureCodes_en.txt
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

    /**
     * <p>
     * Import a Geonames dump into the given {@link LocationSource}.
     * </p>
     * 
     * @param filePath The path to the Geonames dump ZIP file, not <code>null</code>.
     * @param locationSource The {@link LocationSource} where to store the data, not <code>null</code>.
     * @throws IOException 
     */
    public static void importFromGeonames(File filePath, final LocationSource locationSource) throws IOException {
        Validate.notNull(filePath, "filePath must not be null");
        Validate.notNull(locationSource, "locationSource must not be null");

        if (!filePath.isFile()) {
            throw new IllegalArgumentException(filePath.getAbsolutePath() + " does not exist or is no file");
        }
        if (!filePath.getName().endsWith(".zip")) {
            throw new IllegalArgumentException("Input data must be a ZIP file");
        }
        
        // read directly from the ZIP file
        ZipFile zipFile = null;
        InputStream inputStream1 = null;
        InputStream inputStream2 = null;
        try {
            zipFile = new ZipFile(filePath);
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry currentEntry = zipEntries.nextElement();
                if (currentEntry.getName().endsWith(".txt")) {
                    LOGGER.info("Checking size of {} in {}", currentEntry.getName(), filePath);
                    inputStream1 = zipFile.getInputStream(currentEntry);
                    final int totalLines = FileHelper.getNumberOfLines(inputStream1);
                    LOGGER.info("Starting import, {} items to read", totalLines);
                    final StopWatch stopWatch = new StopWatch();
                    inputStream2 = zipFile.getInputStream(currentEntry);
                    FileHelper.performActionOnEveryLine(inputStream2, new LineAction() {
                        @Override
                        public void performAction(String line, int lineNumber) {
                            Location location = parse(line);
                            locationSource.save(location);
                            String progress = ProgressHelper.getProgress(lineNumber, totalLines, 1, stopWatch);
                            if (progress.length() > 0) {
                                LOGGER.info(progress);
                            }
                        }
                    });
                    LOGGER.info("Finished importing in {}", stopWatch);
                }
            }
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                }
            }
            FileHelper.close(inputStream1, inputStream2);
        }
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
        String primaryName = parts[1];
        List<String> alternateNames = CollectionHelper.newArrayList();
        for (String item : parts[3].split(",")) {
            // do not add empty entries and names, which are already set as primary name
            if (item.length() > 0 && !item.equals(primaryName)) {
                alternateNames.add(item);
            }
        }
        Location location = new Location();
        location.setLongitude(Double.valueOf(parts[5]));
        location.setLatitude(Double.valueOf(parts[4]));
        location.setPrimaryName(primaryName);
        location.setAlternativeNames(alternateNames);
        location.setPopulation(Long.valueOf(parts[14]));
        location.setType(mapType(parts[6], parts[7]));
        return location;
    }

    private static LocationType mapType(String featureClass, String featureCode) {
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

    private GeonamesImporter() {
        // helper class.
    }

    public static void main(String[] args) throws IOException {
        LocationDatabase locationSource = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        locationSource.truncate();
        importFromGeonames(new File("/Users/pk/Desktop/LocationLab/geonames.org/allCountries.zip"), locationSource);
    }

}
