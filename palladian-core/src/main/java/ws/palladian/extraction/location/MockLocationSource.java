package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockLocationSource implements LocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MockLocationSource.class);

    @Override
    public List<Location> retrieveLocations(String locationName) {
        return Collections.emptyList();
    }

    @Override
    public void save(Location location) {
        LOGGER.info("Saving {}", location);
    }

    @Override
    public void addHierarchy(int fromId, int toId, String type) {
        LOGGER.info("Hierarchy from {} to {} with type {}", new Object[] {fromId, toId, type});
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
