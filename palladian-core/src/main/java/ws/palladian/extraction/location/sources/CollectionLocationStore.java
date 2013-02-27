package ws.palladian.extraction.location.sources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Function;
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

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionLocationStore.class);

    private final Map<Integer, Location> locationsIds;
    private final MultiMap<String, Location> locationsNames;
    private final Map<Integer, Set<LocationRelation>> hierarchyIds;

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
    public void addHierarchy(LocationRelation hierarchy) {
        if (hierarchy.getChildId() == hierarchy.getParentId()) {
            throw new IllegalArgumentException("A child cannot be the parent of itself (id was "
                    + hierarchy.getChildId() + ")");
        }
        Set<LocationRelation> parentSet = hierarchyIds.get(hierarchy.getChildId());
        if (parentSet == null) {
            parentSet = CollectionHelper.newHashSet();
            hierarchyIds.put(hierarchy.getChildId(), parentSet);
        }
        parentSet.add(hierarchy);
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
            Location currentLocation = getParentLocation(currentLocationId);
            if (currentLocation == null) {
                break;
            }
            ret.add(currentLocation);
            currentLocationId = currentLocation.getId();
        }
        return ret;
    }

    @Override
    public Collection<LocationRelation> getParents(int locationId) {
        Set<LocationRelation> parents = hierarchyIds.get(locationId);
        if (parents == null) {
            return Collections.emptySet();
        }
        return parents;
    }

    private Location getParentLocation(int locationId) {
        Collection<LocationRelation> parentRelations = getParents(locationId);
        if (parentRelations.isEmpty()) {
            LOGGER.trace("No parent for {}", locationId);
            return null;
        }
        if (parentRelations.size() > 1) {
            LOGGER.debug("Ambiguities for {}: {}", locationId, parentRelations);
        }
        MultiMap<Integer, LocationRelation> groupBy = CollectionHelper.groupBy(parentRelations,
                new Function<LocationRelation, Integer>() {
                    @Override
                    public Integer compute(LocationRelation input) {
                        return input.getPriority();
                    }
                });
        for (int index : new TreeSet<Integer>(groupBy.keySet())) {
            List<LocationRelation> values = groupBy.get(index);
            if (values.size() == 1) {
                Location location = locationsIds.get(values.get(0).getParentId());
                if (location == null) {
                    LOGGER.error("Location {} is null", values.get(0).getParentId());
                }
                return location;
            }
        }
        return null;
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
    public Integer getHighestId() {
        // FIXME, untested
        List<Integer> locationIdList = new ArrayList<Integer>(locationsIds.keySet());
        Collections.sort(locationIdList);
        return CollectionHelper.getLast(locationIdList);
    }

}
