package ws.palladian.extraction.location.sources.importers;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.CollectionLocationStore;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.helper.io.DelimitedStringHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * <p>
 * This class reads the Protected Planet CSV locations and imports them into a given {@link LocationStore}.
 * </p>
 * 
 * @see <a href="http://protectedplanet.net/">Protected Planet</a>
 * @author David Urbansky
 */
public final class ProtectedPlanetImporter {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtectedPlanetImporter.class);

    /** The store where the imported locations are saved. */
    private final LocationStore locationStore;

    private final ProgressReporter progressReporter;

    /**
     * <p>
     * Create a new {@link ProtectedPlanetImporter}.
     * </p>
     * 
     * @param locationStore The {@link LocationStore} where to store the data, not <code>null</code>.
     * @param progressReporter For reporting the import progress, or <code>null</code> to report not progress.
     */
    public ProtectedPlanetImporter(LocationStore locationStore, ProgressReporter progressReporter) {
        Validate.notNull(locationStore, "locationStore must not be null");
        this.locationStore = locationStore;
        this.progressReporter = progressReporter != null ? progressReporter : NoProgress.INSTANCE;
    }

    public void importLocations(String locationFilePath) {

        // get the currently highest id
//        final int maxId = locationStore.getHighestId();
        final int totalLocations = FileHelper.getNumberOfLines(locationFilePath) - 1;
        progressReporter.startTask(null, totalLocations);
        locationStore.startImport();

        LineAction action = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                List<String> parts = DelimitedStringHelper.splitLine(line, ',', '"');
                if (lineNumber == 0 || parts.size() != 26) {
                    LOGGER.debug("Skipping line {}: {}", lineNumber, line);
                    return;
                }
                LocationBuilder builder = new LocationBuilder();
                builder.setId(Integer.parseInt(parts.get(0)));
                String name = parts.get(5);
                String origName = parts.get(6);

                builder.setPrimaryName(new String(name)); // new string, save memory.
                if (!name.equals(origName)) {
                    builder.addAlternativeName(new String(origName), null);
                }
                
                builder.setCoordinate(extractSingleCoordinate(parts.get(25)));

//                try {
//                    int coordinatesIndex;
//                    // find start of geometry
//                    for (coordinatesIndex = 25; coordinatesIndex < parts.length; coordinatesIndex++) {
//                        String string = parts[coordinatesIndex];
//                        if (string.contains("coordinates")) {
//                            break;
//                        }
//                    }
//                    String longitudeString = StringHelper.getSubstringBetween(parts[coordinatesIndex], "<coordinates>",
//                            null);
//                    double latitude = Double.parseDouble(StringHelper.getSubstringBetween(parts[coordinatesIndex + 1], null, " "));
//                    double longitude = Double.parseDouble(longitudeString);
//                    builder.setCoordinate(latitude, longitude);
//                } catch (Exception e) {
//                    LOGGER.error("No coordinates in {}", line);
//                }
//                int id = maxId + lineNumber;
//                builder.setId(id);
                builder.setType(LocationType.LANDMARK);
                Location location = builder.create();
                locationStore.save(location);

                progressReporter.increment();
            }
        };

        FileHelper.performActionOnEveryLine(locationFilePath, action);
        locationStore.finishImport();

        LOGGER.info("imported {} locations.", totalLocations);
    }

    /**
     * Parse the KML data, and determine midpoint coordinate from all given coordinates.
     * 
     * @param kmlString The KML string.
     * @return The midpoint coordinate, or <code>null</code> in case the data could not be parsed.
     */
    protected static GeoCoordinate extractSingleCoordinate(String kmlString) {
        try {
            String removedKml = kmlString.replaceAll("<[^>]*>", " ");
            String[] splitPairs = removedKml.split(" ");
            Set<GeoCoordinate> coordinates = new HashSet<>();
            for (String pair : splitPairs) {
                if (pair.length() > 0) {
                    String[] latitudeLongitudeString = pair.split(",");
                    double lat = Double.parseDouble(latitudeLongitudeString[1]);
                    double lng = Double.parseDouble(latitudeLongitudeString[0]);
                    coordinates.add(new ImmutableGeoCoordinate(lat, lng));
                }
            }
            return GeoUtils.getMidpoint(coordinates);
        } catch (Exception e) {
            LOGGER.error("Could not parse KML ({}): {}", e.getMessage(), kmlString);
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        LocationStore locationStore = new CollectionLocationStore();
        // LocationDatabase locationStore = DatabaseManagerFactory.create(LocationDatabase.class, "locations");

        // String locationFilePath = "/Users/pk/Desktop/LocationLab/protectedPlanet.csv";
        String locationFilePath = "/Users/pk/Downloads/qqilihq-search-1417635095276/qqilihq-search-1417635095276.csv";
        ProtectedPlanetImporter importer = new ProtectedPlanetImporter(locationStore, new ProgressMonitor());
        importer.importLocations(locationFilePath);
    }

}
