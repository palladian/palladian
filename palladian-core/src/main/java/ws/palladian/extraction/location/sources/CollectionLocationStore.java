package ws.palladian.extraction.location.sources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;

/**
 * <p>
 * A simple, in-memory location source.
 * </p>
 * 
 * @author Philipp Katz
 */
public class CollectionLocationStore implements LocationStore {

    private final Map<Integer, Location> locationsIds;
    private final MultiMap<String, Location> locationsNames;
    private final Map<Integer, Integer> hierarchyIds;

    public CollectionLocationStore() {
        locationsIds = CollectionHelper.newHashMap();
        locationsNames = MultiMap.create();
        hierarchyIds = CollectionHelper.newHashMap();
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        return locationsNames.get(locationName.toLowerCase());
    }

    @Override
    public List<Location> retrieveLocations(String locationName, EnumSet<Language> languages) {
        // TODO Auto-generated method stub
        return null;
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
        hierarchyIds.put(childId, parentId);
    }

    @Override
    public Location retrieveLocation(int locationId) {
        return locationsIds.get(locationId);
    }

    @Override
    public List<Location> getHierarchy(int locationId) {
        List<Location> ret = CollectionHelper.newArrayList();
        int currentLocationId = locationId;
        for (;;) {
            Integer parentLocationId = hierarchyIds.get(currentLocationId);
            if (parentLocationId == null) {
                break;
            }
            Location parentLocation = locationsIds.get(parentLocationId);
            ret.add(parentLocation);
            currentLocationId = parentLocation.getId();
        }
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CollectionLocationStore [#locationsIds=");
        builder.append(locationsIds.size());
        builder.append(", #locationsNames=");
        builder.append(locationsNames.size());
        builder.append(", #hierarchy=");
        builder.append(hierarchyIds.size());
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

    @Override
    public int getHighestId() {
        if (locationsIds.isEmpty()) {
            return 0;
        }
        List<Integer> locationIdList = new ArrayList<Integer>(locationsIds.keySet());
        return Collections.max(locationIdList);
    }

}
