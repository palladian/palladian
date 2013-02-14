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

    /**
     * <p>
     * Container for parent relations with an associated priority.
     * </p>
     * 
     * @author Philipp Katz
     */
    static class PriorizedRelation implements Comparable<PriorizedRelation> {

        public PriorizedRelation(int parentId, int priority) {
            this.parentId = parentId;
            this.priority = priority;
        }

        final int parentId;
        final int priority;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + parentId;
            result = prime * result + priority;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PriorizedRelation other = (PriorizedRelation)obj;
            if (parentId != other.parentId)
                return false;
            if (priority != other.priority)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return String.format("%s@%s", parentId, priority);
        }

        @Override
        public int compareTo(PriorizedRelation o) {
            return Integer.valueOf(priority).compareTo(o.priority);
        }

    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionLocationStore.class);

    private final Map<Integer, Location> locationsIds;
    private final MultiMap<String, Location> locationsNames;
    private final Map<Integer, Set<PriorizedRelation>> hierarchy;

    public CollectionLocationStore() {
        locationsIds = CollectionHelper.newHashMap();
        locationsNames = MultiMap.create();
        hierarchy = CollectionHelper.newHashMap();
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
    public void addHierarchy(int childId, int parentId, int priority) {
        if (childId == parentId) {
            throw new IllegalArgumentException("A child cannot be the parent of itself (id was " + childId + ")");
        }
        Set<PriorizedRelation> parentSet = hierarchy.get(childId);
        if (parentSet == null) {
            parentSet = CollectionHelper.newHashSet();
            hierarchy.put(childId, parentSet);
        }
        parentSet.add(new PriorizedRelation(parentId, priority));
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
        Set<PriorizedRelation> parentRelations = hierarchy.get(locationId);
        if (parentRelations == null || parentRelations.isEmpty()) {
            LOGGER.trace("No parent for {}", locationId);
            return null;
        }
        List<PriorizedRelation> parentList = new ArrayList<PriorizedRelation>(parentRelations);
        Collections.sort(parentList);
        PriorizedRelation firstRelation = parentList.get(0);
        if (parentRelations.size() == 1) {
            return locationsIds.get(firstRelation.parentId);
        }
        PriorizedRelation secondRelation = parentList.get(1);
        if (firstRelation.priority != secondRelation.priority) {
            return locationsIds.get(firstRelation.parentId);
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
