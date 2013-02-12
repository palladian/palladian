package ws.palladian.extraction.location.sources;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;

/**
 * A simple, in-memory location source.
 * 
 * @author Philipp Katz
 */
public class CollectionLocationStore implements LocationStore {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionLocationStore.class);

    private final Map<Integer, Location> locationsIds;
    private final MultiMap<String, Location> locationsNames;
    private final MultiMap<Integer, Integer> hierarchy; // XXX not sure, if a normal map wouldn't be sufficient here

    public CollectionLocationStore() {
        locationsIds = CollectionHelper.newHashMap();
        locationsNames = MultiMap.create();
        hierarchy = MultiMap.create();
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        return locationsNames.get(locationName.toLowerCase());
    }

    @Override
    public void save(Location location) {
        locationsNames.add(location.getPrimaryName().toLowerCase(), location);
        if (location.getAlternativeNames() != null) {
            addAlternativeNames(location.getId(), location.getAlternativeNames());
        }
        locationsIds.put(location.getId(), location);
    }

    @Override
    public void addHierarchy(int childId, int parentId) {
        if (childId == parentId) {
            throw new IllegalArgumentException("A child cannot be the parent of itself (id was " + childId + ")");
        }
        List<Integer> existingParents = hierarchy.get(childId);
        if (existingParents == null || !existingParents.contains(parentId)) {
            hierarchy.add(childId, parentId);
        }
    }

    @Override
    public Location retrieveLocation(int locationId) {
        return locationsIds.get(locationId);
    }

    @Override
    public List<Location> getHierarchy(Location location) {
        List<Location> ret = CollectionHelper.newArrayList();
        Location currentLocation = location;
        for (;;) {
            currentLocation = getParentLocation(currentLocation);
            if (currentLocation == null) {
                break;
            }
            ret.add(currentLocation);
        }
        return ret;
    }

    private Location getParentLocation(Location location) {
        List<Integer> parentIds = hierarchy.get(location.getId());
        if (parentIds == null) {
            LOGGER.trace("No parent for {}", location.getId());
            return null;
        }
        if (parentIds.size() > 1) {
            LOGGER.warn("Multiple parents for {}: {}", location.getId(), parentIds);
        }
        return locationsIds.get(parentIds.get(0));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CollectionLocationStore [#locationsIds=");
        builder.append(locationsIds.size());
        builder.append(", #locationsNames=");
        builder.append(locationsNames.size());
        builder.append(", #hierarchy=");
        builder.append(hierarchy.size());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void addAlternativeNames(int locationId, Collection<AlternativeName> alternativeNames) {
        Location location = retrieveLocation(locationId);
        for (AlternativeName alternativeName : alternativeNames) {
            locationsNames.add(alternativeName.getName().toLowerCase(), location);
        }
    }

}
