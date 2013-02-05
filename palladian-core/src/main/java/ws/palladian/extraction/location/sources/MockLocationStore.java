package ws.palladian.extraction.location.sources;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.Location;

public class MockLocationStore implements LocationStore {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MockLocationStore.class);

    @Override
    public List<Location> retrieveLocations(String locationName) {
        return Collections.emptyList();
    }

    @Override
    public void save(Location location) {
        LOGGER.trace("Saving {}", location);
    }

    @Override
    public void addHierarchy(int fromId, int toId) {
        LOGGER.trace("Hierarchy from {} to {}", fromId, toId);
    }

    @Override
    public Location retrieveLocation(int locationId) {
        return null;
    }

    @Override
    public List<Location> getHierarchy(Location location) {
        return Collections.emptyList();
    }

}
