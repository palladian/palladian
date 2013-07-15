package ws.palladian.extraction.location;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * @author Philipp Katz
 */
public final class LocationExtractorUtils {

    public static String normalizeName(String value) {
        if (value.matches("([A-Z]\\.)+")) {
            value = value.replace(".", "");
        }
        value = value.replaceAll("[©®™]", "");
        value = value.replaceAll("\\s+", " ");
        if (value.equals("US")) {
            value = "U.S.";
        }
        return value;
    }

    public static boolean isDescendantOf(Location child, Location parent) {
        return child.getAncestorIds().contains(parent.getId());
    }

    public static boolean isChildOf(Location child, Location parent) {
        Integer firstId = CollectionHelper.getFirst(child.getAncestorIds());
        if (firstId == null) {
            return false;
        }
        return firstId == parent.getId();
    }

    public static Location getBiggest(Collection<Location> locations) {
        Location biggest = null;
        for (Location location : locations) {
            Long population = location.getPopulation();
            if (population == null) {
                continue;
            }
            if (biggest == null || population > biggest.getPopulation()) {
                biggest = location;
            }
        }
        return biggest;
    }

    /**
     * <p>
     * For each pair in the given Collection of {@link Location}s determine the distance, and return the highest
     * distance.
     * </p>
     * 
     * @param locations {@link Collection} of {@link Location}s, not <code>null</code>.
     * @return The maximum distance between any pair in the given {@link Collection}, or zero in case the collection wsa
     *         empty.
     */
    public static double getLargestDistance(Collection<Location> locations) {
        double largestDistance = 0;
        List<Location> temp = new ArrayList<Location>(locations);
        for (int i = 0; i < temp.size(); i++) {
            Location l1 = temp.get(i);
            for (int j = i + 1; j < temp.size(); j++) {
                Location l2 = temp.get(j);
                largestDistance = Math.max(largestDistance, GeoUtils.getDistance(l1, l2));
            }
        }
        return largestDistance;
    }

    public static <T> Set<T> filterConditionally(Collection<T> set, Filter<T> filter) {
        Set<T> temp = new HashSet<T>(set);
        CollectionHelper.remove(temp, filter);
        return temp.size() > 0 ? temp : new HashSet<T>(set);
    }

    /**
     * <p>
     * Check, whether two {@link Location}s share a common name. Names are normalized according to the rules given in
     * {@link #normalizeName(String)}.
     * </p>
     * 
     * @param l1 First location, not <code>null</code>.
     * @param l2 Second location, not <code>null</code>.
     * @return <code>true</code>, if a common name exists, <code>false</code> otherwise.
     */
    public static boolean commonName(Location l1, Location l2) {
        Set<String> names1 = collectNames(l1);
        Set<String> names2 = collectNames(l2);
        names1.retainAll(names2);
        return names1.size() > 0;
    }

    public static Set<String> collectNames(Location location) {
        Set<String> names = CollectionHelper.newHashSet();
        names.add(normalizeName(location.getPrimaryName()));
        for (AlternativeName alternativeName : location.getAlternativeNames()) {
            names.add(normalizeName(alternativeName.getName()));
        }
        return names;
    }

    /**
     * <p>
     * Get an {@link Iterator} for the TUD-Loc dataset.
     * </p>
     * 
     * @param datasetDirectory Path to the dataset directory containing the annotated text files and a
     *            <code>coordinates.csv</code> file, not <code>null</code>.
     * @return An iterator for the dataset.
     */
    public static Iterator<LocationDocument> iterateDataset(File datasetDirectory) {
        List<File> files = Arrays.asList(FileHelper.getFiles(datasetDirectory.getPath(), "text"));
        File coordinateFile = new File(datasetDirectory, "coordinates.csv");

        final Iterator<File> fileIterator = files.iterator();
        final Map<String, Map<Integer, GeoCoordinate>> coordinates = readCoordinates(coordinateFile);
        final int numFiles = files.size();

        return new Iterator<LocationDocument>() {
            int counter = 0;
            @Override
            public boolean hasNext() {
                return fileIterator.hasNext();
            }

            @Override
            public LocationDocument next() {
                ProgressHelper.printProgress(counter++, numFiles, 0);
                File currentFile = fileIterator.next();
                String rawText = FileHelper.readFileToString(currentFile).replace(" role=\"main\"", "");
                String cleanText = HtmlHelper.stripHtmlTags(rawText);
                Map<Integer, GeoCoordinate> currentCoordinates = coordinates.get(currentFile.getName());
                List<LocationAnnotation> annotations = getAnnotations(rawText, currentCoordinates);
                return new LocationDocument(currentFile.getName(), cleanText, annotations);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

    }

    /**
     * <p>
     * Read a coordinates CSV file from TUD-Loc dataset. The coordinates file contains the following columns:
     * <code>docId;idx;offset;latitude;longitude;sourceId</code>. <code>docId</code> specifies the filename,
     * <code>idx</code> is a running index for the annotations, starting with zero, <code>offset</code> is the character
     * offset from the beginning of the text, starting with zero, <code>latitude</code> and <code>longitude</code>
     * specify the coordinates, but may be empty, <code>sourceId</code> is a unique, source specific identifier for the
     * location.
     * </p>
     * 
     * @param coordinateFile The path to the coordinate file, not <code>null</code>.
     * @return A nested map; first key is the docId, second key is the character offset, value are {@link GeoCoordinate}
     *         s. In case, the coordinates did not specify longitude/latitude values, the values in the GeoCoordinate
     *         are also <code>null</code>.
     */
    public static Map<String, Map<Integer, GeoCoordinate>> readCoordinates(File coordinateFile) {
        Validate.notNull(coordinateFile, "coordinateFile must not be null");
        final Map<String, Map<Integer, GeoCoordinate>> coordinateMap = LazyMap
                .create(new Factory<Map<Integer, GeoCoordinate>>() {
                    @Override
                    public Map<Integer, GeoCoordinate> create() {
                        return CollectionHelper.newTreeMap();
                    }
                });
        FileHelper.performActionOnEveryLine(coordinateFile, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (lineNumber == 0) {
                    return;
                }
                String[] split = StringUtils.splitPreserveAllTokens(line, ";");
                String documentName = split[0];
                int offset = Integer.valueOf(split[2]);
                GeoCoordinate coordinate = null;
                if (!split[3].isEmpty() && !split[4].isEmpty()) {
                    double lat = Double.valueOf(split[3]);
                    double lng = Double.valueOf(split[4]);
                    coordinate = new ImmutableGeoCoordinate(lat, lng);
                }
                coordinateMap.get(documentName).put(offset, coordinate);
            }
        });
        return coordinateMap;
    }

    private static List<LocationAnnotation> getAnnotations(String rawText, Map<Integer, GeoCoordinate> coordinates) {
        List<LocationAnnotation> annotations = CollectionHelper.newArrayList();
        Annotations<ContextAnnotation> xmlAnnotations = FileFormatParser.getAnnotationsFromXmlText(rawText);
        for (ContextAnnotation xmlAnnotation : xmlAnnotations) {
            int dummyId = xmlAnnotation.getValue().hashCode();
            String name = xmlAnnotation.getValue();
            GeoCoordinate coordinate = coordinates.get(xmlAnnotation.getStartPosition());
            Double lat = coordinate != null ? coordinate.getLongitude() : null;
            Double lng = coordinate != null ? coordinate.getLatitude() : null;
            LocationType type = LocationType.map(xmlAnnotation.getTag());
            Location location = new ImmutableLocation(dummyId, name, type, lng, lat, 0l);
            annotations.add(new LocationAnnotation(xmlAnnotation, location));
        }
        return annotations;
    }

    public static class LocationTypeFilter implements Filter<Location> {

        private final LocationType type;

        public LocationTypeFilter(LocationType type) {
            this.type = type;
        }

        @Override
        public boolean accept(Location item) {
            return item.getType() == type;
        }

    }

    public static class CoordinateFilter implements Filter<Location> {
        @Override
        public boolean accept(Location item) {
            return item.getLatitude() != null && item.getLongitude() != null;
        }

    }

    public static class LocationDocument {

        private final String fileName;
        private final String text;
        private final List<LocationAnnotation> annotations;

        public LocationDocument(String fileName, String text, List<LocationAnnotation> annotations) {
            this.fileName = fileName;
            this.text = text;
            this.annotations = annotations;
        }

        public String getFileName() {
            return fileName;
        }

        public String getText() {
            return text;
        }

        public List<LocationAnnotation> getAnnotations() {
            return annotations;
        }

    }

    private LocationExtractorUtils() {
        // thou shalt not instantiate
    }

}
