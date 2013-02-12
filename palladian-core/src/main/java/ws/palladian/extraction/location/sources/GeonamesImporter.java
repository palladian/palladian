package ws.palladian.extraction.location.sources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.persistence.DatabaseManagerFactory;

/**
 * <p>
 * This class reads data dumps from Geonames (usually you want to take the files "hierarchy.txt" and "allCountries.zip")
 * and imports them into a given {@link LocationStore}.
 * </p>
 * 
 * @see <a href="http://download.geonames.org/export/dump/">Geonames dumps</a>
 * @author Philipp Katz
 */
public final class GeonamesImporter {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GeonamesImporter.class);

    /** Mapping between the administrative/country codes and our internal numeric level. */
    private static final Map<String, Integer> ADMIN_LEVELS_MAPPING;

    static {
        Map<String, Integer> temp = CollectionHelper.newHashMap();
        temp.put("PCLI", 0);
        temp.put("PCLD", 0);
        temp.put("ADM1", 1);
        temp.put("ADM2", 2);
        temp.put("ADM3", 3);
        temp.put("ADM4", 4);
        ADMIN_LEVELS_MAPPING = Collections.unmodifiableMap(temp);
    }

    /** The store where the imported locations are saved. */
    private final LocationStore locationStore;

    /** Mapping between administrative codes and the corresponding location ID, needed to establish hierarchy. */
    private final Map<String, Integer> countryMapping;
    private final Map<String, Integer> admin1Mapping;
    private final Map<String, Integer> admin2Mapping;
    private final Map<String, Integer> admin3Mapping;
    private final Map<String, Integer> admin4Mapping;

    /** Explicitly given hierarchy relations, they have precedence over the administrative relations. */
    private final Map<Integer, Integer> hierarchyMappings;

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
        this.countryMapping = CollectionHelper.newHashMap();
        this.admin1Mapping = CollectionHelper.newHashMap();
        this.admin2Mapping = CollectionHelper.newHashMap();
        this.admin3Mapping = CollectionHelper.newHashMap();
        this.admin4Mapping = CollectionHelper.newHashMap();
        this.hierarchyMappings = CollectionHelper.newHashMap();
    }

    /**
     * <p>
     * Import a Geonames dump from a ZIP file.
     * </p>
     * 
     * @param locationFile The path to the Geonames dump ZIP file, not <code>null</code>.
     * @param hierarchyFile The path to the hierarchy file, not <code>null</code>.
     * @param alternateNamesFile The path to the alternate names file, not <code>null</code>.
     * @throws IOException
     */
    public void importLocationsZip(File locationFile, File hierarchyFile, File alternateNamesFile) throws IOException {
        Validate.notNull(locationFile, "locationFile must not be null");
        checkIsFileOfType(locationFile, "zip");

        // read the hierarchy first
        importHierarchy(hierarchyFile);

        // read the alternate names file
        importAlternativeNames(alternateNamesFile);

        // read directly from the ZIP file, get the entry in the file with the location data
        ZipFile zipFile = null;
        InputStream inputStream1, inputStream2, inputStream3;
        inputStream1 = inputStream2 = inputStream3 = null;
        try {
            zipFile = new ZipFile(locationFile);
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

            LOGGER.info("Checking size of {} in {}", locationZipEntry.getName(), locationFile);
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

            saveHierarchy();

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
     * @param locationFile The path to the Geonames dump TXT file, not <code>null</code>.
     * @param hierarchyFile The path to the hierarchy file, not <code>null</code>.
     * @param alternateNamesFile The path to the alternate names file, not <code>null</code>.
     * @throws IOException
     */
    public void importLocations(File locationFile, File hierarchyFile, File alternateNamesFile) throws IOException {
        Validate.notNull(locationFile, "locationFile must not be null");
        checkIsFileOfType(locationFile, "txt");

        importHierarchy(hierarchyFile);
        importAlternativeNames(alternateNamesFile);

        LOGGER.info("Checking size of {}", locationFile);
        int totalLines = FileHelper.getNumberOfLines(locationFile);
        LOGGER.info("Starting import, {} items in total", totalLines);

        InputStream inputStream1, inputStream2;
        inputStream1 = inputStream2 = null;
        try {
            inputStream1 = new FileInputStream(locationFile);
            readAdministrativeItems(inputStream1, totalLines);

            inputStream2 = new FileInputStream(locationFile);
            importLocations(inputStream2, totalLines);

            saveHierarchy();

            LOGGER.info("Finished importing {} items", totalLines);
        } finally {
            FileHelper.close(inputStream1, inputStream2);
        }
    }

    private void saveHierarchy() {
        for (Integer childId : hierarchyMappings.keySet()) {
            Integer parentId = hierarchyMappings.get(childId);
            locationStore.addHierarchy(childId, parentId);
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

        // explicitly given hierarchy relations (as defined in the hierarchy.txt file) have precedence over the derived
        // parental relations
        Integer explicitMapping = hierarchyMappings.get(location.geonamesId);
        if (explicitMapping != null) {
            return explicitMapping;
        }

        List<String> hierarchyCode = location.getCodeParts();

        if (hierarchyCode.size() > 0) {
            // if it is an administrative location, we need to remove the last part in the code,
            // in order to walk up in the hierarchy
            if (location.isAdministrativeUnit()) {
                hierarchyCode.remove(hierarchyCode.get(hierarchyCode.size() - 1));
            }

            for (int i = hierarchyCode.size(); i > 0; i--) {
                String parentCode = StringUtils.join(hierarchyCode.subList(0, i), '.');
                Map<String, Integer> mapping = getMappingForLevel(i - 1);
                Integer retrievedParentId = mapping.get(parentCode);
                if (retrievedParentId != null && retrievedParentId != location.geonamesId) {
                    return retrievedParentId;
                }
            }
        }
        return null;
    }
    
    private Map<String, Integer> getMappingForLevel(int level) {
        switch (level) {
            case 0:
                return countryMapping;
            case 1:
                return admin1Mapping;
            case 2:
                return admin2Mapping;
            case 3:
                return admin3Mapping;
            case 4:
                return admin4Mapping;
        }
        throw new IllegalArgumentException("Level " + level + " is not allowed.");
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
        readLocations(inputStream, totalLines, new LocationLineCallback() {
            @Override
            public void readLocation(GeonameLocation geonameLocation) {
                String codeCombined = geonameLocation.getCodeCombined();

                // remove historic locations from the hierarchy mapping again, as we do not want the DDR in the
                // hierarchy list ... sorry, Erich.
                if (geonameLocation.isHistoric()) {
                    int removeCount = removeByValue(hierarchyMappings, geonameLocation.geonamesId);
                    LOGGER.debug("Removed {} occurences of historic location {} with type {} from hierarchy mappings",
                            new Object[] {removeCount, geonameLocation.geonamesId, codeCombined});
                    return;
                }

                if (!geonameLocation.isAdministrativeUnit() || codeCombined.isEmpty() || codeCombined.endsWith("*")) {
                    return;
                }

                Map<String, Integer> mapping = getMappingForLevel(geonameLocation.getLevel());
                Integer existingItem = mapping.get(codeCombined);
                if (existingItem == null) {
                    mapping.put(codeCombined, geonameLocation.geonamesId);
                } else {
                    LOGGER.error(
                            "There is already an item with code {} in the mappings, this will almost certainly lead to inconsistencies and should be fixed! (item with Geonames ID {})",
                            codeCombined, geonameLocation.geonamesId);
                }
            }

        });
        LOGGER.info("Finished reading administrative items");
        LOGGER.info("# country: {}", countryMapping.size());
        LOGGER.info("# level 1: {}", admin1Mapping.size());
        LOGGER.info("# level 2: {}", admin2Mapping.size());
        LOGGER.info("# level 3: {}", admin3Mapping.size());
        LOGGER.info("# level 4: {}", admin4Mapping.size());
    }

    /**
     * Remove entries from a map with a given value.
     * 
     * @param map The map.
     * @param value The value.
     * @return The number of removed entries.
     */
    private <K, V> int removeByValue(Map<K, V> map, V value) {
        int removeCount = 0;
        Iterator<Entry<K, V>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<K, V> entry = iterator.next();
            if (entry.getValue().equals(value)) {
                iterator.remove();
                removeCount++;
            }
        }
        return removeCount;
    }

    /**
     * <p>
     * Import a Geonames hierarchy file.
     * </p>
     * 
     * @param filePath The path to the hierarchy.txt file, not <code>null</code>.
     */
    private void importHierarchy(File filePath) {
        Validate.notNull(filePath, "filePath must not be null");
        checkIsFileOfType(filePath, "txt");

        final int numLines = FileHelper.getNumberOfLines(filePath);
        final StopWatch stopWatch = new StopWatch();
        LOGGER.info("Reading hierarchy from {}, {} lines to read", filePath.getAbsolutePath(), numLines);
        FileHelper.performActionOnEveryLine(filePath.getAbsolutePath(), new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("\\s");
                if (split.length < 2) {
                    return;
                }
                int parentId = Integer.valueOf(split[0]);
                int childId = Integer.valueOf(split[1]);
                hierarchyMappings.put(childId, parentId);
                String progress = ProgressHelper.getProgress(lineNumber, numLines, 1, stopWatch);
                if (!progress.isEmpty()) {
                    LOGGER.info(progress);
                }
            }
        });
        LOGGER.info("Finished importing hierarchy in {}", stopWatch.getTotalElapsedTimeString());
    }

    /**
     * <p>
     * Import a Geonames alternate names file.
     * </p>
     * 
     * @param filePath The path to the alternative names file, not <code>null</code>.
     */
    private void importAlternativeNames(File filePath) {
        Validate.notNull(filePath, "filePath must not be null");
        checkIsFileOfType(filePath, "txt");

        final int numLines = FileHelper.getNumberOfLines(filePath);
        final StopWatch stopWatch = new StopWatch();
        LOGGER.info("Reading alternative names from {}, {} lines to read", filePath.getAbsolutePath(), numLines);
        FileHelper.performActionOnEveryLine(filePath.getAbsolutePath(), new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String progress = ProgressHelper.getProgress(lineNumber, numLines, 1, stopWatch);
                if (!progress.isEmpty()) {
                    LOGGER.info(progress);
                }
                String[] split = line.split("\\t");
                if (split.length < 4) {
                    return;
                }
                int geonameid = Integer.valueOf(split[1]);
                String isoLanguage = split[2];
                String alternateName = split[3];
                Language language = null;
                if (!isoLanguage.isEmpty()) {
                    language = Language.getByIso6391(isoLanguage);
                    if (language == null) {
                        // a language was specified, but not mapped in our enum. Thank you, we're not interested.
                        return;
                    }
                }
                AlternativeName name = new AlternativeName(alternateName, language);
                locationStore.addAlternativeNames(geonameid, Collections.singletonList(name));
            }
        });
        LOGGER.info("Finished importing alternative names in {}", stopWatch.getTotalElapsedTimeString());
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
//        List<String> alternateNames = CollectionHelper.newArrayList();
//        for (String item : parts[3].split(",")) {
//            // do not add empty entries and names, which are already set as primary name
//            if (item.length() > 0 && !item.equals(primaryName)) {
//                alternateNames.add(item);
//            }
//        }
        GeonameLocation location = new GeonameLocation();
        location.geonamesId = Integer.valueOf(parts[0]);
        location.longitude = Double.valueOf(parts[5]);
        location.latitude = Double.valueOf(parts[4]);
        location.primaryName = stringOrNull(primaryName);
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

            int level = getLevel();
            if (level >= 0) {
                ret.add(countryCode != null ? countryCode : "*");
                if (level >= 1) {
                    ret.add(admin1Code != null ? admin1Code : "*");
                    if (level >= 2) {
                        ret.add(admin2Code != null ? admin2Code : "*");
                        if (level >= 3) {
                            ret.add(admin3Code != null ? admin3Code : "*");
                            if (level >= 4) {
                                ret.add(admin4Code != null ? admin4Code : "*");
                            }
                        }
                    }
                }
            }

            // did we get any meaningful content at all? If we only have a result like "*.*.*.*",
            // just return an empty list instead.
            boolean noContent = true;
            for (String part : ret) {
                if (!part.equals("*")) {
                    noContent = false;
                }
            }

            if (noContent) {
                return Collections.emptyList();
            }

            return ret;
        }

        boolean isAdministrativeUnit() {
            boolean adminDivision = ADMIN_LEVELS_MAPPING.containsKey(featureCode);
            return isAdministrativeClass() && adminDivision;
        }

        int getLevel() {
            if (isAdministrativeClass()) {
                Integer result = ADMIN_LEVELS_MAPPING.get(featureCode);
                if (result != null) {
                    return result;
                }
            }
            return Integer.MAX_VALUE;
        }

        boolean isHistoric() {
            if (isAdministrativeClass()) {
                if (featureCode != null && featureCode.endsWith("H")) {
                    return true;
                }
            }
            return false;
        }

        boolean isAdministrativeClass() {
            return "A".equals(featureClass);
        }

        Location buildLocation() {
            Location location = new Location();
            location.setId(geonamesId);
            location.setLongitude(longitude);
            location.setLatitude(latitude);
            location.setPrimaryName(primaryName);
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

    @Override
    public String toString() {
        StringBuilder bob = new StringBuilder();
        bob.append("Mappings:").append('\n');
        bob.append("CountryMapping=").append(countryMapping).append('\n');
        bob.append("Admin1Mapping=").append(admin1Mapping).append('\n');
        bob.append("Admin2Mapping=").append(admin2Mapping).append('\n');
        bob.append("Admin3Mapping=").append(admin3Mapping).append('\n');
        bob.append("Admin4Mapping=").append(admin4Mapping).append('\n');
        bob.append("HierarchyMapping=").append(hierarchyMappings);
        return bob.toString();
    }

    public static void main(String[] args) throws IOException {
        // LocationStore locationStore = new CollectionLocationStore();
        LocationDatabase locationStore = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        // locationStore.truncate();

        GeonamesImporter importer = new GeonamesImporter(locationStore);
        File locationFile = new File("/Users/pk/Desktop/LocationLab/geonames.org/allCountries.zip");
        File hierarchyFile = new File("/Users/pk/Desktop/LocationLab/geonames.org/hierarchy.txt");
        File alternateNamesFile = new File(
                "/Users/pk/Desktop/LocationLab/geonames.org/alternateNames/alternateNames.txt");
        // importer.importLocationsZip(locationFile, hierarchyFile, alternateNamesFile);
        importer.importAlternativeNames(alternateNamesFile);
    }

}
