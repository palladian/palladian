package ws.palladian.extraction.location.sources.importers;

import java.io.IOException;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

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

    /**
     * <p>
     * Create a new {@link ProtectedPlanetImporter}.
     * </p>
     * 
     * @param locationStore The {@link LocationStore} where to store the data, not <code>null</code>.
     */
    public ProtectedPlanetImporter(LocationStore locationStore) {
        Validate.notNull(locationStore, "locationStore must not be null");
        this.locationStore = locationStore;
    }

    public void importLocations(String locationFilePath) {

        StopWatch stopWatch = new StopWatch();

        // get the currently highest id
        final int maxId = locationStore.getHighestId();
        final int totalLocations = FileHelper.getNumberOfLines(locationFilePath) - 1;

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                if (lineNumber == 0) {
                    return;
                }
                String[] parts = line.split(",");
                String placeName = parts[5];
                Double latitude = null;
                Double longitude = null;
                try {
                    int coordinatesIndex;
                    // find start of geometry
                    for (coordinatesIndex = 25; coordinatesIndex < parts.length; coordinatesIndex++) {
                        String string = parts[coordinatesIndex];
                        if (string.contains("coordinates")) {
                            break;
                        }
                    }

                    String longitudeString = StringHelper.getSubstringBetween(parts[coordinatesIndex], "<coordinates>",
                            null);
                    latitude = Double.valueOf(StringHelper.getSubstringBetween(parts[coordinatesIndex + 1], null, " "));
                    longitude = Double.valueOf(longitudeString);
                } catch (Exception e) {
                    LOGGER.error("no coordinates for " + placeName, e);
                }
                Location location = new Location(maxId + lineNumber, placeName, null, LocationType.LANDMARK, latitude,
                        longitude, null);
                locationStore.save(location);

                ProgressHelper.printProgress(lineNumber, totalLocations, 1);
            }
        };

        FileHelper.performActionOnEveryLine(locationFilePath, la);

        LOGGER.info("imported " + totalLocations + " locations in " + stopWatch.getElapsedTimeString());
    }

    public static void main(String[] args) throws IOException {
        // LocationStore locationStore = new CollectionLocationStore();
        LocationDatabase locationStore = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        locationStore.truncate();

        String locationFilePath = "PATH";
        ProtectedPlanetImporter importer = new ProtectedPlanetImporter(locationStore);
        importer.importLocations(locationFilePath);
    }

}
