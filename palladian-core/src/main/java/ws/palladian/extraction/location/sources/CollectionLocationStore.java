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

    private final Map<Integer, Location> idsLocations;
    private final MultiMap<String, Integer> namesIds;
    private final Map<Integer, Integer> hierarchyIds;
    private final MultiMap<Integer, AlternativeName> idsAlternativeNames;

    public CollectionLocationStore() {
        idsLocations = CollectionHelper.newHashMap();
        namesIds = MultiMap.create();
        hierarchyIds = CollectionHelper.newHashMap();
        idsAlternativeNames = MultiMap.create();
    }

    @Override
    public Collection<Location> retrieveLocations(String locationName) {
        Collection<Location> result = CollectionHelper.newHashSet();
        List<Integer> ids = namesIds.get(locationName.toLowerCase());
        for (Integer id : ids) {
            Location location = retrieveLocation(id);
            result.add(location);
        }
        return result;
    }

    @Override
    public Collection<Location> retrieveLocations(String locationName, EnumSet<Language> languages) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void save(Location location) {
        namesIds.add(location.getPrimaryName().toLowerCase(), location.getId());
        if (location.getAlternativeNames() != null) {
            addAlternativeNames(location.getId(), location.getAlternativeNames());
        }
        idsLocations.put(location.getId(), location);
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
        Location temp = idsLocations.get(locationId);
        List<AlternativeName> alternativeNames = idsAlternativeNames.get(locationId);
        return new Location(temp.getId(), temp.getPrimaryName(), alternativeNames, temp.getType(), temp.getLatitude(),
                temp.getLongitude(), temp.getPopulation());
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
            Location parentLocation = idsLocations.get(parentLocationId);
            ret.add(parentLocation);
            currentLocationId = parentLocation.getId();
        }
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CollectionLocationStore [#locationsIds=");
        builder.append(idsLocations.size());
        builder.append(", #namesIds=");
        builder.append(namesIds.size());
        builder.append(", #hierarchy=");
        builder.append(hierarchyIds.size());
        builder.append(", #idsAlternativeNames=");
        builder.append(idsAlternativeNames.allValues().size());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void addAlternativeNames(int locationId, Collection<AlternativeName> alternativeNames) {
        for (AlternativeName alternativeName : alternativeNames) {
            namesIds.add(alternativeName.getName().toLowerCase(), locationId);
            idsAlternativeNames.add(locationId, alternativeName);
        }
    }

    @Override
    public int getHighestId() {
        if (idsLocations.isEmpty()) {
            return 0;
        }
        List<Integer> locationIdList = new ArrayList<Integer>(idsLocations.keySet());
        return Collections.max(locationIdList);
    }

}
