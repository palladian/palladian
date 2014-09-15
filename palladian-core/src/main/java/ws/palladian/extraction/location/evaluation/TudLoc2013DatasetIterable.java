package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * {@link Iterable} TUD-Loc-2013 dataset (and datasets which have been converted to this format).
 * 
 * @author pk
 */
public final class TudLoc2013DatasetIterable implements Iterable<LocationDocument> {

    /** The pattern for recognizing the role="main" annotation. */
    private static final String MAIN_ROLE_ANNOTATION_PATTERN = "\\<([A-Z]+)(\\s+role=\"main\")?\\>(.{1,1000}?)\\</\\1\\>";

    private final List<File> files;
    private final Map<String, Map<Integer, GeoCoordinate>> coordinates;
    private final int numFiles;
    private final File datasetDirectory;

    public TudLoc2013DatasetIterable(File datasetDirectory) {
        Validate.notNull(datasetDirectory, "datasetDirectory must not be null");
        files = Arrays.asList(FileHelper.getFiles(datasetDirectory.getPath(), "text"));
        final File coordinateFile = new File(datasetDirectory, "coordinates.csv");
        coordinates = readCoordinates(coordinateFile);
        numFiles = files.size();
        this.datasetDirectory = datasetDirectory;
    }

    @Override
    public Iterator<LocationDocument> iterator() {
        return new Iterator<LocationDocument>() {
            Iterator<File> fileIterator = files.iterator();
            ProgressMonitor monitor = new ProgressMonitor(numFiles, 0);

            @Override
            public boolean hasNext() {
                return fileIterator.hasNext();
            }

            @Override
            public LocationDocument next() {
                monitor.incrementAndPrintProgress();
                File currentFile = fileIterator.next();
                String fileContent = FileHelper.tryReadFileToString(currentFile);
                String rawText = fileContent.replace(" role=\"main\"", "");
                String cleanText = HtmlHelper.stripHtmlTags(rawText);
                Map<Integer, GeoCoordinate> currentCoordinates = coordinates.get(currentFile.getName());
                List<LocationAnnotation> annotations = getAnnotations(rawText, currentCoordinates);
                int mainLocationIdx = getMainLocationIdx(fileContent);
                Location mainLocation = null;
                if (mainLocationIdx != -1) {
                    mainLocation = annotations.get(mainLocationIdx).getLocation();
                }
                return new LocationDocument(currentFile.getName(), cleanText, annotations, mainLocation);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Get the index of the annotation marked with <code>role="main"</code>.
     * 
     * @param text The text.
     * @return The main index, or -1 if no annotation was marked as such.
     */
    private static int getMainLocationIdx(String text) {
        Pattern pattern = Pattern.compile(MAIN_ROLE_ANNOTATION_PATTERN, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        int idx = 0;
        while (matcher.find()) {
            if (matcher.group(2) != null && matcher.group(2).length() > 0) {
                return idx;
            }
            idx++;
        }
        return -1;
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
    static Map<String, Map<Integer, GeoCoordinate>> readCoordinates(File coordinateFile) {
        Validate.notNull(coordinateFile, "coordinateFile must not be null");
        final Map<String, Map<Integer, GeoCoordinate>> coordinateMap = LazyMap
                .create(new Factory<Map<Integer, GeoCoordinate>>() {
                    @Override
                    public Map<Integer, GeoCoordinate> create() {
                        return CollectionHelper.newTreeMap();
                    }
                });
        int lines = FileHelper.performActionOnEveryLine(coordinateFile, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (lineNumber == 0) {
                    return;
                }
                String[] split = StringUtils.splitPreserveAllTokens(line, ";");
                String documentName = split[0];
                int offset = Integer.parseInt(split[2]);
                GeoCoordinate coordinate = null;
                if (!split[3].isEmpty() && !split[4].isEmpty()) {
                    double lat = Double.parseDouble(split[3]);
                    double lng = Double.parseDouble(split[4]);
                    coordinate = new ImmutableGeoCoordinate(lat, lng);
                }
                coordinateMap.get(documentName).put(offset, coordinate);
            }
        });
        if (lines == -1) {
            throw new IllegalStateException("Could not read " + coordinateFile);
        }
        return coordinateMap;
    }

    private static List<LocationAnnotation> getAnnotations(String rawText, Map<Integer, GeoCoordinate> coordinates) {
        List<LocationAnnotation> annotations = CollectionHelper.newArrayList();
        Annotations<Annotation> xmlAnnotations = FileFormatParser.getAnnotationsFromXmlText(rawText);
        for (Annotation xmlAnnotation : xmlAnnotations) {
            int dummyId = xmlAnnotation.getValue().hashCode();
            String name = xmlAnnotation.getValue();
            GeoCoordinate coordinate = coordinates.get(xmlAnnotation.getStartPosition());
            LocationType type = LocationType.map(xmlAnnotation.getTag());
            Location location = new ImmutableLocation(dummyId, name, type, coordinate, 0l);
            annotations.add(new LocationAnnotation(xmlAnnotation, location));
        }
        return annotations;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TudLoc2013DatasetIterable [datasetDirectory=");
        builder.append(datasetDirectory);
        builder.append("]");
        return builder.toString();
    }

}
