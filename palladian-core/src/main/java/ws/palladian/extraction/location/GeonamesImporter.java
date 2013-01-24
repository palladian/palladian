package ws.palladian.extraction.location;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.persistence.DatabaseManagerFactory;

public class GeonamesImporter {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GeonamesImporter.class);

    public static void importFromGeonames(File filePath, final LocationSource locationSource) {
        LOGGER.info("Starting import from {}, storing to {}", filePath, locationSource);
        
        final int totalLines = FileHelper.getNumberOfLines(filePath);
        final StopWatch stopWatch = new StopWatch();
        FileHelper.performActionOnEveryLine(filePath.getAbsolutePath(), new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                Location location = parse(line);
                locationSource.save(location);
                ProgressHelper.printProgress(lineNumber, totalLines, 1, stopWatch);
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
     * @param line
     * @return
     */
    protected static Location parse(String line) {
        String[] parts = line.split("\\t");
        if (parts.length != 19) {
            throw new IllegalStateException("Exception while parsing, expected 19 elements, but was " + parts.length
                    + "('" + line + "')");
        }
        
        LOGGER.trace("Value array {}", Arrays.toString(parts));

        // long id = Long.valueOf(parts[0]);
        String name = parts[1];
        String[] alternateNames = parts[3].split(",");
        double latitude = Double.valueOf(parts[4]);
        double longitude = Double.valueOf(parts[5]);
        String featureClass = parts[6];
        // String featureCode = parts[7];
        // String countryCode = parts[8];
        long population = Long.valueOf(parts[14]);
        
        List<String> names = CollectionHelper.newArrayList();
        names.add(name);
        names.addAll(Arrays.asList(alternateNames));

        Location location = new Location();
        location.setLongitude(longitude);
        location.setLatitude(latitude);
        location.setNames(names);
        location.setPopulation((int)population);
        // location.setValue(name);
        location.setType(featureClass);
        return location;
    }

    public static void main(String[] args) {
        LocationDatabase locationSource = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        locationSource.truncate();
        importFromGeonames(new File("/Users/pk/Desktop/DE/DE.txt"), locationSource);
    }

}
