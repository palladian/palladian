package ws.palladian.extraction.location.sources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private Location getParentLocation(int locationId) {
        //
        // XXX I have a feeling that this method is more complicated than absolutely necessary. We sort our candidates
        // by priority and check if we have a clear winner (i.e. exact on top location which has a higher priority than
        // all other locations). If there are more top locations with equal priority, we can no determine the parent
        // correctly.
        //
        Set<LocationRelation> parentRelations = hierarchyIds.get(locationId);
        if (parentRelations == null || parentRelations.isEmpty()) {
            LOGGER.trace("No parent for {}", locationId);
            return null;
        }
        List<LocationRelation> parentList = new ArrayList<LocationRelation>(parentRelations);
        Collections.sort(parentList);
        LocationRelation firstRelation = parentList.get(0);
        if (parentRelations.size() == 1) {
            return locationsIds.get(firstRelation.getParentId());
        }
        LocationRelation secondRelation = parentList.get(1);
        if (firstRelation.getPriority() != secondRelation.getPriority()) {
            return locationsIds.get(firstRelation.getParentId());
        }
        LOGGER.warn("Multiple parents for {}: {}", locationId, parentRelations);
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

}
