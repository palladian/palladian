package ws.palladian.extraction.location.sources.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AbstractLocation;
import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.functional.Consumer;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
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

    /** Name of the file in the ZIP archive containing all countries. */
    private static final String COUNTRIES_FILE_NAME = "allCountries.txt";
    /** Name of the file in the ZIP archive containing alternate namings. */
    private static final String ALTERNATE_FILE_NAME = "alternateNames.txt";
    /** Name of the file in the ZIP archive containing the hierarchy. */
    private static final String HIERARCHY_FILE_NAME = "hierarchy.txt";

    /** Mapping between the administrative/country codes and our internal numeric level. */
    private static final Map<String, Integer> ADMIN_LEVELS_MAPPING;

    static {
        Map<String, Integer> temp = new HashMap<>();
        temp.put("PCLI", 0);
        temp.put("PCLD", 0);
        temp.put("TERR", 0);
        temp.put("PCLIX", 0);
        temp.put("ADM1", 1);
        temp.put("ADM2", 2);
        temp.put("ADM3", 3);
        temp.put("ADM4", 4);
        ADMIN_LEVELS_MAPPING = Collections.unmodifiableMap(temp);
    }

    /** The store where the imported locations are saved. */
    private final LocationStore locationStore;

    /** Mapping between administrative codes and the corresponding location ID, needed to establish hierarchy. */
    private final Map<String, Integer> administrativeMappings = new HashMap<>();

    /** Explicitly given hierarchy relations, they have precedence over the administrative relations. */
    private final Map<Integer, Integer> hierarchyMappings = new HashMap<>();

    /** For reporting import progress. */
    private final ProgressReporter progressReporter;

    /**
     * <p>
     * Create a new {@link GeonamesImporter}.
     * </p>
     * 
     * @param locationStore The {@link LocationStore} where to store the data, not <code>null</code>.
     * @param progressReporter For reporting the import progress, or <code>null</code> to report not progress.
     */
    public GeonamesImporter(LocationStore locationStore, ProgressReporter progressReporter) {
        Validate.notNull(locationStore, "locationStore must not be null");
        this.locationStore = locationStore;
        this.progressReporter = progressReporter != null ? progressReporter : NoProgress.INSTANCE;
    }

    /**
     * <p>
     * Import a Geonames dump from given ZIP files.
     * </p>
     * 
     * @param locationFile The path to the Geonames dump ZIP file, not <code>null</code>.
     * @param hierarchyFile The path to the hierarchy ZIP file, not <code>null</code>.
     * @param alternateNamesFile The path to the alternate names ZIP file, not <code>null</code>.
     * @throws IOException
     */
    public void importLocationsZip(File locationFile, File hierarchyFile, File alternateNamesFile) throws IOException {
        checkIsFileOfType(locationFile, "zip");
        checkIsFileOfType(hierarchyFile, "zip");
        checkIsFileOfType(alternateNamesFile, "zip");
        InputStreamProvider locationProvider = new ZipEntryInputStreamProvider(locationFile, COUNTRIES_FILE_NAME);
        InputStreamProvider hierarchyProvider = new ZipEntryInputStreamProvider(hierarchyFile, HIERARCHY_FILE_NAME);
        InputStreamProvider alternateNamesProvider = new ZipEntryInputStreamProvider(alternateNamesFile, ALTERNATE_FILE_NAME);
        importLocations(locationProvider, hierarchyProvider, alternateNamesProvider);
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
        checkIsFileOfType(locationFile, "txt");
        checkIsFileOfType(hierarchyFile, "txt");
        checkIsFileOfType(alternateNamesFile, "txt");
        InputStreamProvider locationProvider = new FileInputStreamProvider(locationFile);
        InputStreamProvider hierarchyProvider = new FileInputStreamProvider(hierarchyFile);
        InputStreamProvider alternateNamesProvider = new FileInputStreamProvider(alternateNamesFile);
        importLocations(locationProvider, hierarchyProvider, alternateNamesProvider);
    }

    private void importLocations(InputStreamProvider locationProvider, InputStreamProvider hierarchyProvider,
            InputStreamProvider alternateNamesProvider) throws IOException {
        progressReporter.startTask(null, -1);
        importHierarchy(hierarchyProvider, progressReporter.createSubProgress(0.25));
        int totalLines;
        try (InputStream inputStream = locationProvider.getInputStream()) {
            totalLines = FileHelper.getNumberOfLines(inputStream);
        }
        try (InputStream inputStream = locationProvider.getInputStream()) {
            readAdministrativeItems(inputStream, progressReporter.createSubProgress(0.25), totalLines);
        }
        try (InputStream inputStream = locationProvider.getInputStream()) {
            importLocations(inputStream, progressReporter.createSubProgress(0.25), totalLines);
        }
        LOGGER.info("Finished importing {} locations", totalLines);
        importAlternativeNames(alternateNamesProvider, progressReporter.createSubProgress(0.25));
    }

    private static void checkIsFileOfType(File filePath, String fileType) {
        Validate.notNull(filePath, "filePath must not be null");
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
     * @param inputStream Stream to the input file.
     * @param progress Progress monitor.
     * @param numLines Total number of lines in the locations file.
     */
    private void importLocations(InputStream inputStream, final ProgressReporter progress, int numLines) {
        LOGGER.info("Inserting locations");
        progress.startTask("Reading locations", numLines);
        readLocations(inputStream, progress, new Consumer<GeonameLocation>() {
            @Override
            public void process(GeonameLocation geonameLocation) {
                locationStore.save(geonameLocation);
                Integer parentId = getParent(geonameLocation);
                if (parentId != null) {
                    locationStore.addHierarchy(geonameLocation.geonamesId, parentId);
                } else {
                    LOGGER.debug("No parent for {}", geonameLocation.geonamesId);
                }
            }
        });
    }

    /**
     * <p>
     * Get ID of parent location, if it exists. First try to get parent from the relations in the
     * <code>hierarchy.txt</code> file. If there is no given relation, determine parent through administrative
     * relations.
     * </p>
     * 
     * @param location The {@link GeonameLocation} for which to get the parent.
     * @return The ID of the parent relation, if a parent exists, or <code>null</code> if no parent could be found.
     */
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
                hierarchyCode.remove(hierarchyCode.size() - 1);
            }

            if (location.isAdministrativeUnitUnleveled()) {
                for (int i = hierarchyCode.size() - 1; i >= 0; i--) {
                    if (hierarchyCode.get(hierarchyCode.size() - 1).equals("*")) {
                        hierarchyCode.remove(hierarchyCode.size() - 1);
                    } else {
                        break;
                    }
                }
                hierarchyCode.remove(hierarchyCode.size() - 1);
            }

            for (int i = hierarchyCode.size(); i > 0; i--) {
                String parentCode = StringUtils.join(hierarchyCode.subList(0, i), '.');
                Integer retrievedParentId = administrativeMappings.get(parentCode);
                if (retrievedParentId != null && retrievedParentId != location.geonamesId) {
                    return retrievedParentId;
                }
            }
        }
        return null;
    }

    /**
     * <p>
     * Read administrative entries and save them, so that they can be referred to later, when we need to look up
     * hierarchical relations.
     * </p>
     * 
     * @param inputStream Stream to the input file.
     * @param progress Progress monitor.
     * @param numLines Total number of lines in the locations file.
     */
    private void readAdministrativeItems(InputStream inputStream, final ProgressReporter progress, int numLines) {
        LOGGER.info("Reading administrative items");
        progress.startTask("Reading administrative items", numLines);
        readLocations(inputStream, progress, new Consumer<GeonameLocation>() {
            @Override
            public void process(GeonameLocation geonameLocation) {
                String codeCombined = geonameLocation.getCodeCombined();

                // remove historic locations from the hierarchy mapping again, as we do not want the DDR in the
                // hierarchy list ... sorry, Erich.
                if (geonameLocation.isHistoric()) {
                    // XXX keep, but lower priority here?
                    int removeCount = removeChildFromHierarchy(geonameLocation.geonamesId);
                    LOGGER.debug("Removed {} occurences of historic location {} with type {} from hierarchy mappings",
                            new Object[] {removeCount, geonameLocation.geonamesId, codeCombined});
                    return;
                }

                if (geonameLocation.isLowerOrderAdminDivision()) {
                    // XXX keep, but lower priority here?
                    removeChildFromHierarchy(geonameLocation.geonamesId);
                    LOGGER.debug("Remove second order relation {}", geonameLocation.geonamesId);
                }

                // FIXME priority needs to be determined based on the destination of relation

                if (!geonameLocation.isAdministrativeUnit() || codeCombined.isEmpty() || codeCombined.endsWith("*")) {
                    return;
                }

                Integer existingItem = administrativeMappings.get(codeCombined);
                if (existingItem == null) {
                    administrativeMappings.put(codeCombined, geonameLocation.geonamesId);
                } else {
                    LOGGER.error(
                            "There is already an item with code {} in the mappings, this will almost certainly lead to inconsistencies and should be fixed! (item with Geonames ID {})",
                            codeCombined, geonameLocation.geonamesId);
                }
            }

        });
        LOGGER.info("Finished reading {} administrative items for mapping", administrativeMappings.size());
    }

    /**
     * Remove entries from a map with a given value.
     * 
     * @param geonamesId The geonames id which to remove.
     * @return The number of removed entries.
     */
    private int removeChildFromHierarchy(int geonamesid) {
        int oldSize = hierarchyMappings.size();
        hierarchyMappings.values().removeAll(Collections.singleton(geonamesid));
        return oldSize - hierarchyMappings.size();
    }

    /**
     * <p>
     * Import a Geonames hierarchy file.
     * </p>
     * 
     * @param hierarchyProvider The {@link InputStream} providing the data.
     * @param progress Progress monitor.
     * @throws IOException
     */
    private void importHierarchy(InputStreamProvider hierarchyProvider, final ProgressReporter progress)
            throws IOException {
        LOGGER.info("Importing hierarchy");
        int numLines;
        try (InputStream inputStream = hierarchyProvider.getInputStream()) {
            numLines = FileHelper.getNumberOfLines(inputStream);
        }
        progress.startTask("Reading hierarchy", numLines);
        try (InputStream inputStream = hierarchyProvider.getInputStream()) {
            final MultiMap<Integer, Integer> childParents = DefaultMultiMap.createWithSet();
            FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
                @Override
                public void performAction(String line, int lineNumber) {
                    String[] split = line.split("\\s");
                    if (split.length < 2) {
                        return;
                    }
                    int parentId = Integer.parseInt(split[0]);
                    int childId = Integer.parseInt(split[1]);
                    String type = split.length > 2 ? split[2] : null;
                    if (type == null || type.equals("ADM")) {
                        childParents.add(childId, parentId);
                    }
                    progress.increment();
                }
            });
            // only add relation, if unambiguous
            for (Integer childId : childParents.keySet()) {
                Collection<Integer> parentIds = childParents.get(childId);
                if (parentIds.size() == 1) {
                    hierarchyMappings.put(childId, CollectionHelper.getFirst(parentIds));
                }
            }
            LOGGER.info("Finished importing hierarchy.");
        }
    }

    /**
     * <p>
     * Import a Geonames alternate names file.
     * </p>
     * 
     * @param alternateNamesProvider The {@link InputStream} providing the data.
     * @param progress Progress monitor.
     * @throws IOException
     */
    private void importAlternativeNames(InputStreamProvider alternateNamesProvider, final ProgressReporter progress)
            throws IOException {
        LOGGER.info("Importing alternative names");
        int numLines;
        try (InputStream inputStream = alternateNamesProvider.getInputStream()) {
            numLines = FileHelper.getNumberOfLines(inputStream);
        }
        progress.startTask("Reading alternate names", numLines);
        try (InputStream inputStream = alternateNamesProvider.getInputStream()) {
            FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
                @Override
                public void performAction(String line, int lineNumber) {
                    progress.increment();
                    String[] split = line.split("\\t");
                    if (split.length < 4) {
                        return;
                    }
                    int geonameid = Integer.parseInt(split[1]);
                    String isoLanguage = split[2];
                    String alternateName = split[3];
                    Language language = null;
                    if (!isoLanguage.isEmpty() && !isoLanguage.equals("abbr")) {
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
        }
        LOGGER.info("Finished importing {} alternative names.", numLines);
    }

    /**
     * Reduce empty string to null, lower memory consumption by creating new strings.
     * 
     * @param string The string.
     * @return A new string, as compact as possible, or <code>null</code> in case the given string was empty.
     */
    private static final String stringOrNull(String string) {
        return string.isEmpty() ? null : new String(string);
    }

    private static final void readLocations(InputStream inputStream, final ProgressReporter progress,
            final Consumer<GeonameLocation> callback) {
        FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (line.isEmpty()) {
                    return;
                }
                GeonameLocation geonameLocation = new GeonameLocation(line);
                callback.process(geonameLocation);
                progress.increment();
            }
        });
        LOGGER.debug("Finished processing.");
    }

    /**
     * Temporally hold locations after parsing. This class basically just resembles the structure of the GeoNames data.
     */
    private static final class GeonameLocation extends AbstractLocation {
        final int geonamesId;
        final GeoCoordinate coordinate;
        final String primaryName;
        final long population;
        final String featureClass;
        final String featureCode;
        final String countryCode;
        final String admin1Code;
        final String admin2Code;
        final String admin3Code;
        final String admin4Code;

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
         */
        GeonameLocation(String line) {
            String[] parts = line.split("\\t");
            if (parts.length != 19) {
                throw new IllegalStateException("Exception while parsing, expected 19 elements, but was "
                        + parts.length + "('" + line + "')");
            }
            this.geonamesId = Integer.parseInt(parts[0]);
            double longitude = Double.parseDouble(parts[5]);
            double latitude = Double.parseDouble(parts[4]);
            this.coordinate = new ImmutableGeoCoordinate(latitude, longitude);
            this.primaryName = stringOrNull(parts[1]);
            this.population = Long.parseLong(parts[14]);
            this.featureClass = stringOrNull(parts[6]);
            this.featureCode = stringOrNull(parts[7]);
            this.countryCode = stringOrNull(parts[8]);
            this.admin1Code = stringOrNull(parts[10]);
            this.admin2Code = stringOrNull(parts[11]);
            this.admin3Code = stringOrNull(parts[12]);
            this.admin4Code = stringOrNull(parts[13]);
        }

        String getCodeCombined() {
            return StringUtils.join(getCodeParts(), '.');
        }

        List<String> getCodeParts() {
            List<String> ret = new ArrayList<>();

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
            for (String part : ret) {
                if (!part.equals("*")) {
                    return ret;
                }
            }

            return Collections.emptyList();
        }

        boolean isAdministrativeUnit() {
            return isAdministrativeClass() && ADMIN_LEVELS_MAPPING.containsKey(featureCode);
        }

        boolean isAdministrativeUnitUnleveled() {
            return isAdministrativeClass() && featureCode != null && featureCode.equals("ADMD");
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
            return isAdministrativeClass() && featureCode != null && featureCode.endsWith("H");
        }

        boolean isLowerOrderAdminDivision() {
            return "P".equals(featureClass) && Arrays.asList("PPLA2", "PPLA3", "PPLA4").contains(featureCode);
        }

        boolean isAdministrativeClass() {
            return "A".equals(featureClass);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("GeonameLocation [geonamesId=");
            builder.append(geonamesId);
            builder.append(", coordinate=");
            builder.append(coordinate);
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

        @Override
        public int getId() {
            return geonamesId;
        }

        @Override
        public String getPrimaryName() {
            return primaryName;
        }

        @Override
        public Collection<AlternativeName> getAlternativeNames() {
            return Collections.emptyList();
        }

        @Override
        public LocationType getType() {
            return GeonamesUtil.mapType(featureClass, featureCode);
        }

        @Override
        public GeoCoordinate getCoordinate() {
            return coordinate;
        }

        @Override
        public Long getPopulation() {
            return population;
        }

        @Override
        public List<Integer> getAncestorIds() {
            return Collections.emptyList();
        }

    }
    
    private static interface InputStreamProvider {
        InputStream getInputStream() throws IOException;
    }

    private static final class FileInputStreamProvider implements InputStreamProvider {
        private final File file;

        public FileInputStreamProvider(File file) {
            this.file = file;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }
    }

    private static final class ZipEntryInputStreamProvider implements InputStreamProvider {
        private final File zipFile;
        private final String fileName;

        public ZipEntryInputStreamProvider(File zipFile, String fileName) {
            this.zipFile = zipFile;
            this.fileName = fileName;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (fileName.equals(zipEntry.getName())) {
                    return zipInputStream;
                }
            }
            throw new IOException("No matching entry found for " + fileName + ".");
        }
    }

    public static void main(String[] args) throws IOException {
        // LocationStore locationStore = new CollectionLocationStore();
        LocationDatabase locationStore = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        locationStore.truncate();

        GeonamesImporter importer = new GeonamesImporter(locationStore, null);
        File locationFile = new File("/Users/pk/Desktop/LocationLab/geonames.org/allCountries.zip");
        File hierarchyFile = new File("/Users/pk/Desktop/LocationLab/geonames.org/hierarchy.zip");
        File alternateNames = new File("/Users/pk/Desktop/LocationLab/geonames.org/alternateNames.zip");
        importer.importLocationsZip(locationFile, hierarchyFile, alternateNames);
    }

}
