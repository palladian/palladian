package ws.palladian.extraction.location;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

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

//    /** Mapping from feature codes from the dataset to LocationType. */
//    private static final Map<String, LocationType> FEATURE_MAPPING;
//
//    static {
//        // TODO check, whether those mappings make sense
//        // http://download.geonames.org/export/dump/featureCodes_en.txt
//        // http://www.geonames.org/export/codes.html
//        Map<String, LocationType> temp = CollectionHelper.newHashMap();
//        temp.put("A", LocationType.UNIT);
//        temp.put("A.PCL", LocationType.COUNTRY);
//        temp.put("A.PCLD", LocationType.COUNTRY);
//        temp.put("A.PCLF", LocationType.COUNTRY);
//        temp.put("A.PCLH", LocationType.COUNTRY);
//        temp.put("A.PCLI", LocationType.COUNTRY);
//        temp.put("A.PCLIX", LocationType.COUNTRY);
//        temp.put("A.PCLS", LocationType.COUNTRY);
//        temp.put("H", LocationType.LANDMARK);
//        temp.put("L", LocationType.POI);
//        temp.put("L.AREA", LocationType.REGION);
//        temp.put("L.COLF", LocationType.REGION);
//        temp.put("L.CONT", LocationType.CONTINENT);
//        temp.put("L.RGN", LocationType.REGION);
//        temp.put("L.RGNE", LocationType.REGION);
//        temp.put("L.RGNH", LocationType.REGION);
//        temp.put("L.RGNL", LocationType.REGION);
//        temp.put("P", LocationType.CITY);
//        temp.put("R", LocationType.POI);
//        temp.put("S", LocationType.POI);
//        temp.put("T", LocationType.LANDMARK);
//        temp.put("U", LocationType.LANDMARK);
//        temp.put("U.BDLU", LocationType.REGION);
//        temp.put("U.PLNU", LocationType.REGION);
//        temp.put("U.PRVU", LocationType.REGION);
//        temp.put("V", LocationType.POI);
//        FEATURE_MAPPING = Collections.unmodifiableMap(temp);
//    }

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
        InputStream inputStream3 = null;
        try {
            zipFile = new ZipFile(filePath);
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry currentEntry = zipEntries.nextElement();
                String zipEntryName = currentEntry.getName().toLowerCase();
                if (zipEntryName.endsWith(".txt") && !zipEntryName.contains("readme")) {


                    LOGGER.info("Checking size of {} in {}", currentEntry.getName(), filePath);
                    inputStream1 = zipFile.getInputStream(currentEntry);
                    final int totalLines = FileHelper.getNumberOfLines(inputStream1);
                    FileHelper.close(inputStream1);
                    LOGGER.info("Starting import, {} items in total", totalLines);

                    LOGGER.info("/////////////////// Reading administrative items //////////////////////");
                    final Map<String, GeonameLocation> adminLocations = CollectionHelper.newHashMap();
                    inputStream2 = zipFile.getInputStream(currentEntry);
                    readLocations(inputStream2, totalLines, new LocationLineCallback() {
                        @Override
                        public void readLocation(GeonameLocation geonameLocation) {
                            if (geonameLocation.isAdministrative()) {
                                adminLocations.put(geonameLocation.getCombinedCode(), geonameLocation);
                            }
                        }
                    });
                    FileHelper.close(inputStream2);
                    LOGGER.info("Finished reading {} administrative items", adminLocations.size());

                    LOGGER.info("///////////////////// Inserting hierarchy /////////////////////////////");
                    for (int i = 0; i <= 5; i++) {
                        for (GeonameLocation currentLocation : adminLocations.values()) {
                            if (currentLocation.getLevel() == i) {
                                GeonameLocation parent = adminLocations.get(currentLocation.getParentCode());
                                if (parent == null) {
                                    LOGGER.error("No parent found for {} ({}) with {}",
                                            new Object[] {currentLocation.primaryName, currentLocation.geonamesId,
                                                    currentLocation.getParentCode()});
                                    continue;
                                }
                                locationSource.addHierarchy(currentLocation.geonamesId, parent.geonamesId, null);
                            }
                        }
                    }
                    LOGGER.info("Finished inserting hierarchy");

                    LOGGER.info("///////////////////// Inserting locations /////////////////////////////");
                    inputStream3 = zipFile.getInputStream(currentEntry);
                    readLocations(inputStream3, totalLines, new LocationLineCallback() {
                        @Override
                        public void readLocation(GeonameLocation geonameLocation) {
                            locationSource.save(geonameLocation.buildLocation());
                            // for non administrative, we have to add the parent here...
                            if (!geonameLocation.isAdministrative()) {
                                GeonameLocation parentLocation = adminLocations.get(geonameLocation.getParentCode());
                                if (parentLocation != null) {
                                    locationSource.addHierarchy(geonameLocation.geonamesId, parentLocation.geonamesId,
                                            null);
                                } else {
                                    System.out.println("No parent for " + geonameLocation.geonamesId);
                                }
                            }
                        }
                    });
                    FileHelper.close(inputStream3);
                    LOGGER.info("Finished importing {} items", totalLines);
                }
            }
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
     * Import a Geonames hierarchy file.
     * </p>
     * 
     * @param hierarchyFilePath The path to the hierarchy.txt file, not <code>null</code>.
     * @param locationSource The {@link LocationSource} where to store the data, not <code>null</code>.
     */
    public static void importHierarchy(File hierarchyFilePath, final LocationSource locationSource) {
        Validate.notNull(hierarchyFilePath, "hierarchyFilePath must not be null");
        Validate.notNull(locationSource, "locationSource must not be null");

        if (!hierarchyFilePath.isFile()) {
            throw new IllegalArgumentException(hierarchyFilePath.getAbsolutePath() + " does not exist or is no file");
        }
        if (!hierarchyFilePath.getName().endsWith(".txt")) {
            throw new IllegalArgumentException("Input data must be a TXT file");
        }

        final int numLines = FileHelper.getNumberOfLines(hierarchyFilePath);
        final StopWatch stopWatch = new StopWatch();
        LOGGER.info("Reading hierachy, {} lines to read", numLines);
        FileHelper.performActionOnEveryLine(hierarchyFilePath.getAbsolutePath(), new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("\\s");
                if (split.length < 2) {
                    return;
                }
                int from = Integer.valueOf(split[0]);
                int to = Integer.valueOf(split[1]);
                String type = null;
                if (split.length == 3) {
                    type = split[2];
                }
                locationSource.addHierarchy(from, to, type);
                String progress = ProgressHelper.getProgress(lineNumber, numLines, 1, stopWatch);
                if (!progress.isEmpty()) {
                    LOGGER.info(progress);
                }
            }
        });
        LOGGER.info("Finished import");
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

        String getCombinedCode() {
            return StringUtils.join(getHierarchyCode(), '.');
        }

        String getParentCode() {
            List<String> hierarchyCode = getHierarchyCode();

            // special case: countries have no parent in the schema
            // (this is modeled in the dedicated hierarchy schema).
            if (isCountry()) {
                return StringUtils.EMPTY;
            }

            // special case; if we only have one parent code (i.e. country code like DE),
            // we return this
            if (hierarchyCode.size() == 1) {
                return hierarchyCode.get(0);
            }

            // remove the last item
            hierarchyCode.remove(hierarchyCode.size() - 1);

            // if we have entries with zeros, remove them and the following;
            // this is necessary, if we have a hierarchy spanning more than one level
            // (e.g. ADM1 < ADM3)
            Iterator<String> iterator = hierarchyCode.iterator();
            boolean remove = false;
            while (iterator.hasNext()) {
                String current = iterator.next();
                if (remove || current.matches("[0]+")) {
                    iterator.remove();
                    remove = true;
                }
            }
            return StringUtils.join(hierarchyCode, '.');
        }

        private List<String> getHierarchyCode() {
            List<String> ret = CollectionHelper.newArrayList();
            if (countryCode != null) {
                ret.add(countryCode);
            } else {
                ret.add("*");
            }
            if (!isCountry() && admin1Code != null) {
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
            return ret;
        }

        boolean isAdministrative() {
            boolean continent = "L".equals(featureClass) && "CONT".equals(featureCode);
            boolean adminFeatureClass = "A".equals(featureClass);
            boolean adminDivision = Arrays.asList("ADM1", "ADM2", "ADM3", "ADM4", "PCLI").contains(featureCode);
            return continent || (adminFeatureClass && adminDivision);
        }

        boolean isCountry() {
            return "A".equals(featureClass) && "PCLI".equals(featureCode);
        }

        /** Retrieve the hierarchy level. */
        int getLevel() {
            if ("L".equals(featureClass) && "CONT".equals(featureCode)) {
                return 0;
            }
            if ("A".equals(featureClass)) {
                if ("PCLI".equals(featureCode)) {
                    return 1;
                }
                if ("ADM1".equals(featureCode)) {
                    return 2;
                }
                if ("ADM2".equals(featureCode)) {
                    return 3;
                }
                if ("ADM3".equals(featureCode)) {
                    return 4;
                }
                if ("ADM4".equals(featureCode)) {
                    return 5;
                }
            }
            return -1; // not administrative, therefore no hierarchy level.
        }

        Location buildLocation() {
            Location location = new Location();
            location.setId(geonamesId);
            location.setLongitude(longitude);
            location.setLatitude(latitude);
            location.setPrimaryName(primaryName);
            location.setAlternativeNames(alternativeNames);
            location.setPopulation(population);
            location.setType(GeonamesLocationSource.mapType(featureClass, featureCode));
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

    private GeonamesImporter() {
        // helper class.
    }

    public static void main(String[] args) throws IOException {
        // LocationSource locationSource = new MockLocationSource();
        LocationSource locationSource = new CollectionLocationSource();
        // LocationDatabase locationSource = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        // locationSource.truncate();
        importFromGeonames(new File("/Users/pk/Desktop/LocationLab/geonames.org/DE.zip"), locationSource);
        // importFromGeonames(new File("/Users/pk/Desktop/LocationLab/geonames.org/allCountries.zip"), locationSource);
        // importHierarchy(new File("/Users/pk/Desktop/LocationLab/geonames.org/hierarchy.txt"), locationSource);

        System.out.println(locationSource);

        // List<Location> locations = locationSource.retrieveLocations("stuttgart");
        List<Location> locations = locationSource.retrieveLocations("Wiendorf");
        CollectionHelper.print(locations);

        System.out.println("-------");

        Location firstLocation = CollectionHelper.getLast(locations);
        List<Location> hierarchy = locationSource.getHierarchy(firstLocation);
        CollectionHelper.print(hierarchy);

    }

}
