package ws.palladian.extraction.location.sources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.Location;
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
 * into a given {@link LocationStore}.
 * </p>
 * 
 * @see <a href="http://download.geonames.org/export/dump/">Geonames dumps</a>
 * @author Philipp Katz
 */
public final class GeonamesImporter {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GeonamesImporter.class);

    /** The store where the imported locations are saved. */
    private final LocationStore locationStore;

    /** Mapping between administrative codes and the corresponding location ID, needed to establish hierarchy. */
    private final Map<String, Integer> adminMappings;

    /**
     * <p>
     * Create a new {@link GeonamesImporter}.
     * </p>
     * 
     * @param locationStore The {@link LocationStore} where to store the data, not <code>null</code>.
     */
    public GeonamesImporter(LocationStore locationStore) {
        Validate.notNull(locationStore, "locationStore must not be null");

        this.locationStore = locationStore;
        this.adminMappings = CollectionHelper.newHashMap();
    }

    /**
     * <p>
     * Import a Geonames dump from a ZIP file.
     * </p>
     * 
     * @param filePath The path to the Geonames dump ZIP file, not <code>null</code>.
     * @throws IOException
     */
    public void importLocationsZip(File filePath) throws IOException {
        Validate.notNull(filePath, "filePath must not be null");
        checkIsFileOfType(filePath, "zip");

        // read directly from the ZIP file, get the entry in the file with the location data
        ZipFile zipFile = null;
        InputStream inputStream1, inputStream2, inputStream3;
        inputStream1 = inputStream2 = inputStream3 = null;
        try {
            zipFile = new ZipFile(filePath);
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            ZipEntry locationZipEntry = null;
            while (zipEntries.hasMoreElements()) {
                locationZipEntry = zipEntries.nextElement();
                String zipEntryName = locationZipEntry.getName().toLowerCase();
                if (zipEntryName.endsWith(".txt") && !zipEntryName.contains("readme")) {
                    break;
                }
            }
            if (locationZipEntry == null) {
                throw new IllegalStateException(
                        "No suitable ZIP entry for import found; make sure the correct file was supplied.");
            }

            LOGGER.info("Checking size of {} in {}", locationZipEntry.getName(), filePath);
            inputStream1 = zipFile.getInputStream(locationZipEntry);
            int totalLines = FileHelper.getNumberOfLines(inputStream1);
            FileHelper.close(inputStream1);
            LOGGER.info("Starting import, {} items in total", totalLines);

            inputStream2 = zipFile.getInputStream(locationZipEntry);
            readAdministrativeItems(inputStream2, totalLines);
            FileHelper.close(inputStream2);

            inputStream3 = zipFile.getInputStream(locationZipEntry);
            importLocations(inputStream3, totalLines);
            FileHelper.close(inputStream3);

            LOGGER.info("Finished importing {} items", totalLines);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                }
            }
            FileHelper.close(inputStream1, inputStream2, inputStream3);
        }
    }

    /**
     * <p>
     * Import a Geonames dump from a TXT file.
     * </p>
     * 
     * @param filePath The path to the Geonames dump TXT file, not <code>null</code>.
     * @throws IOException
     */
    public void importLocations(File filePath) throws IOException {
        Validate.notNull(filePath, "filePath must not be null");
        checkIsFileOfType(filePath, "txt");

        LOGGER.info("Checking size of {}", filePath);
        int totalLines = FileHelper.getNumberOfLines(filePath);
        LOGGER.info("Starting import, {} items in total", totalLines);

        InputStream inputStream1, inputStream2;
        inputStream1 = inputStream2 = null;
        try {
            inputStream1 = new FileInputStream(filePath);
            readAdministrativeItems(inputStream1, totalLines);

            inputStream2 = new FileInputStream(filePath);
            importLocations(inputStream2, totalLines);

            LOGGER.info("Finished importing {} items", totalLines);
        } finally {
            FileHelper.close(inputStream1, inputStream2);
        }
    }

    private void checkIsFileOfType(File filePath, String fileType) {
        if (!filePath.isFile()) {
            throw new IllegalArgumentException(filePath.getAbsolutePath() + " does not exist or is no file");
        }
        if (!filePath.getName().endsWith(fileType)) {
            throw new IllegalArgumentException("Input data must be a " + fileType.toUpperCase() + " file");
        }
    }

    /**
     * Insert non-administrative entries and establish their hierarchical relations.
     * 
     * @param inputStream
     * @param totalLines
     */
    private void importLocations(InputStream inputStream, final int totalLines) {
        LOGGER.info("///////////////////// Inserting locations /////////////////////////////");
        readLocations(inputStream, totalLines, new LocationLineCallback() {
            @Override
            public void readLocation(GeonameLocation geonameLocation) {
                locationStore.save(geonameLocation.buildLocation());
                Integer parentLocationId = getParent(geonameLocation);
                if (parentLocationId != null) {
                    locationStore.addHierarchy(geonameLocation.geonamesId, parentLocationId);
                } else {
                    LOGGER.debug("No parent for {}", geonameLocation.geonamesId);
                }
            }
        });
    }

    private Integer getParent(GeonameLocation location) {
        List<String> hierarchyCode = location.getCodeParts();
        for (int i = hierarchyCode.size(); i > 0; i--) {
            String parentCode = StringUtils.join(hierarchyCode.subList(0, i), '.');
            Integer retrievedParentId = adminMappings.get(parentCode);
            if (retrievedParentId != null && retrievedParentId != location.geonamesId) {
                return retrievedParentId;
            }
        }
        return null;
    }

    /**
     * <p>
     * Read administrative entries and save them, so that they can be refered to later, when we need to look up
     * hierarchical relations.
     * </p>
     * 
     * @param inputStream Stream to the input file.
     * @param totalLines The total number of lines to process, just for informative reasons (progress display).
     */
    private void readAdministrativeItems(InputStream inputStream, final int totalLines) {
        LOGGER.info("/////////////////// Reading administrative items //////////////////////");
        adminMappings.clear();
        readLocations(inputStream, totalLines, new LocationLineCallback() {
            @Override
            public void readLocation(GeonameLocation geonameLocation) {
                Integer existingItem = adminMappings.get(geonameLocation.getCodeCombined());
                if (existingItem == null && geonameLocation.isAdministrativeUnit()) {
                    Integer temp = adminMappings.get(geonameLocation.getCodeCombined());
                    if (temp == null) {
                        adminMappings.put(geonameLocation.getCodeCombined(), geonameLocation.geonamesId);
                    }
                }
            }
        });
        LOGGER.info("Finished reading {} administrative items", adminMappings.size());
    }

    /**
     * <p>
     * Import a Geonames hierarchy file.
     * </p>
     * 
     * @param filePath The path to the hierarchy.txt file, not <code>null</code>.
     */
    public void importHierarchy(File filePath) {
        Validate.notNull(filePath, "filePath must not be null");
        checkIsFileOfType(filePath, "txt");

        final int numLines = FileHelper.getNumberOfLines(filePath);
        final StopWatch stopWatch = new StopWatch();
        LOGGER.info("Reading hierachy, {} lines to read", numLines);
        FileHelper.performActionOnEveryLine(filePath.getAbsolutePath(), new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("\\s");
                if (split.length < 2) {
                    return;
                }
                int parentId = Integer.valueOf(split[0]);
                int childId = Integer.valueOf(split[1]);
                locationStore.addHierarchy(childId, parentId);
                String progress = ProgressHelper.getProgress(lineNumber, numLines, 1, stopWatch);
                if (!progress.isEmpty()) {
                    LOGGER.info(progress);
                }
            }
        });
        LOGGER.info("Finished importing hierarchy in {}", stopWatch.getTotalElapsedTimeString());
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
    protected static GeonameLocation parse(String line) {
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
        GeonameLocation location = new GeonameLocation();
        location.geonamesId = Integer.valueOf(parts[0]);
        location.longitude = Double.valueOf(parts[5]);
        location.latitude = Double.valueOf(parts[4]);
        location.primaryName = stringOrNull(primaryName);
        location.alternativeNames = alternateNames; // FIXME; we can import those in a second run.
        location.population = Long.valueOf(parts[14]);
        location.featureClass = stringOrNull(parts[6]);
        location.featureCode = stringOrNull(parts[7]);
        location.countryCode = stringOrNull(parts[8]);
        location.admin1Code = stringOrNull(parts[10]);
        location.admin2Code = stringOrNull(parts[11]);
        location.admin3Code = stringOrNull(parts[12]);
        location.admin4Code = stringOrNull(parts[13]);
        return location;
    }

    /**
     * Reduce empty string to null, lower memory consumption by creating new strings.
     * 
     * @param string
     * @return
     */
    private static final String stringOrNull(String string) {
        if (string.isEmpty()) {
            return null;
        }
        return new String(string);
    }

    private static interface LocationLineCallback {
        void readLocation(GeonameLocation geonameLocation);
    }

    private static final void readLocations(InputStream inputStream, final int totalLines,
            final LocationLineCallback callback) {
        final StopWatch stopWatch = new StopWatch();
        FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (line.isEmpty()) {
                    return;
                }
                GeonameLocation geonameLocation = parse(line);
                callback.readLocation(geonameLocation);
                String progress = ProgressHelper.getProgress(lineNumber, totalLines, 1, stopWatch);
                if (progress.length() > 0) {
                    LOGGER.info(progress);
                }
            }
        });
        LOGGER.debug("Finished processing, took {}", stopWatch.getTotalElapsedTimeString());
    }

    /**
     * Temporally hold locations after parsing. This class basically just resembles the structure of the GeoNames data.
     */
    static final class GeonameLocation {
        int geonamesId;
        double longitude;
        double latitude;
        String primaryName;
        List<String> alternativeNames;
        long population;
        String featureClass;
        String featureCode;
        String countryCode;
        String admin1Code;
        String admin2Code;
        String admin3Code;
        String admin4Code;

        String getCodeCombined() {
            return StringUtils.join(getCodeParts(), '.');
        }

        List<String> getCodeParts() {
            List<String> ret = CollectionHelper.newArrayList();
            if (countryCode != null) {
                ret.add(countryCode);
            } else {
                ret.add("*");
            }
            if (admin1Code != null) {
                ret.add(admin1Code);
                if (admin2Code != null) {
                    ret.add(admin2Code);
                    if (admin3Code != null) {
                        ret.add(admin3Code);
                        if (admin4Code != null) {
                            ret.add(admin4Code);
                        }
                    }
                }
            }

            // if we end on a part only consisting of zeros, remove that
            String lastPart = CollectionHelper.getLast(ret);
            if (lastPart.matches("[0]+")) {
                ret.remove(ret.size() - 1);
            }

            return ret;
        }

        boolean isAdministrativeUnit() {
            boolean adminFeatureClass = "A".equals(featureClass);
            boolean adminDivision = Arrays.asList("ADM1", "ADM2", "ADM3", "ADM4", "PCLI", "PCLD").contains(featureCode);
            return adminFeatureClass && adminDivision;
        }

        Location buildLocation() {
            Location location = new Location();
            location.setId(geonamesId);
            location.setLongitude(longitude);
            location.setLatitude(latitude);
            location.setPrimaryName(primaryName);
            location.setAlternativeNames(alternativeNames);
            location.setPopulation(population);
            location.setType(GeonamesUtil.mapType(featureClass, featureCode));
            return location;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("GeonameLocation [geonamesId=");
            builder.append(geonamesId);
            builder.append(", longitude=");
            builder.append(longitude);
            builder.append(", latitude=");
            builder.append(latitude);
            builder.append(", primaryName=");
            builder.append(primaryName);
            builder.append(", alternativeNames=");
            builder.append(alternativeNames);
            builder.append(", population=");
            builder.append(population);
            builder.append(", featureClass=");
            builder.append(featureClass);
            builder.append(", featureCode=");
            builder.append(featureCode);
            builder.append(", countryCode=");
            builder.append(countryCode);
            builder.append(", admin1Code=");
            builder.append(admin1Code);
            builder.append(", admin2Code=");
            builder.append(admin2Code);
            builder.append(", admin3Code=");
            builder.append(admin3Code);
            builder.append(", admin4Code=");
            builder.append(admin4Code);
            builder.append("]");
            return builder.toString();
        }

    }

    public static void main(String[] args) throws IOException {
        // LocationSource locationStore = new MockLocationStore();
        // LocationStore locationStore = new CollectionLocationStore();
        LocationDatabase locationStore = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        locationStore.truncate();

        GeonamesImporter importer = new GeonamesImporter(locationStore);
        importer.importHierarchy(new File("/Users/pk/Desktop/LocationLab/geonames.org/hierarchy.txt"));
        importer.importLocationsZip(new File("/Users/pk/Desktop/LocationLab/geonames.org/DE.zip"));
        // importer.importLocationsZip(new File("/Users/pk/Desktop/LocationLab/geonames.org/allCountries.zip"));
    }

}
