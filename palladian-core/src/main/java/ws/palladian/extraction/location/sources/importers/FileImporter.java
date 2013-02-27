package ws.palladian.extraction.location.sources.importers;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

/**
 * <p>
 * This class reads simple CSV files with locations and imports them into a given {@link LocationStore}.
 * </p>
 * 
 * <p>
 * A CSV must obey to the following format:
 * </p>
 * 
 * <pre>
 * Location Name;Location Type;Latitude;Longitude;Population
 * </pre>
 * 
 * @author David Urbansky
 */
public final class FileImporter {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FileImporter.class);

    /** The store where the imported locations are saved. */
    private final LocationStore locationStore;

    /**
     * <p>
     * Create a new {@link FileImporter}.
     * </p>
     * 
     * @param locationStore The {@link LocationStore} where to store the data, not <code>null</code>.
     */
    public FileImporter(LocationStore locationStore) {
        Validate.notNull(locationStore, "locationStore must not be null");
        this.locationStore = locationStore;
    }

    public void importLocations(String locationFilePath) {

        StopWatch stopWatch = new StopWatch();

        // get the currently highest id
        int maxId = locationStore.getHighestId();
        
        List<String> lines = FileHelper.readFileToArray(locationFilePath);
        int idOffset = 1;
        for (String line : lines) {
            String[] parts = line.split(";");
            String locationName = parts[0];
            LocationType locationType = LocationType.valueOf(parts[1]);
            Double latitude = Double.valueOf(parts[2]);
            Double longitude = Double.valueOf(parts[3]);
            Location location = new Location(maxId+idOffset, locationName, null, locationType, latitude, longitude, null);
            locationStore.save(location);
            idOffset++;
        }

        LOGGER.info("imported " + (idOffset - 1) + " locations in " + stopWatch.getElapsedTimeString());
    }

    public static void main(String[] args) throws IOException {
        // LocationStore locationStore = new CollectionLocationStore();
        LocationDatabase locationStore = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        locationStore.truncate();

        String locationFilePath = "";
        FileImporter importer = new FileImporter(locationStore);
        importer.importLocations(locationFilePath);
    }

}
