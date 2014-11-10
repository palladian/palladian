package ws.palladian.extraction.location.sources;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.AbstractLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;

/**
 * <p>
 * A simple, in-memory {@link LocationStore}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class CollectionLocationStore extends SingleQueryLocationSource implements LocationStore {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionLocationStore.class);

    private final Map<Integer, LinkedLocation> idLocation;
    private final MultiMap<String, LinkedLocation> namesLocations;

    public CollectionLocationStore() {
        idLocation = CollectionHelper.newHashMap();
        namesLocations = DefaultMultiMap.createWithSet();
    }

    @Override
    public Collection<Location> getLocations(String locationName, Set<Language> languages) {
        LOGGER.warn("getLocations(String,EnumSet<Language>) is not supported, ignoring language parameter");
        return Collections.<Location> unmodifiableCollection(namesLocations.get(locationName.toLowerCase()));
    }

    @Override
    public void save(Location location) {
        LinkedLocation linkedLocation = getOrCreate(location.getId());
        linkedLocation.merge(location);
        namesLocations.add(location.getPrimaryName().toLowerCase(), linkedLocation);
        Collection<AlternativeName> alternativeNames = location.getAlternativeNames();
        if (alternativeNames != null) {
            for (AlternativeName alternativeName : location.getAlternativeNames()) {
                namesLocations.add(alternativeName.getName().toLowerCase(), linkedLocation);
            }
        }
    }

    private LinkedLocation getOrCreate(int locationId) {
        LinkedLocation linkedLocation = idLocation.get(locationId);
        if (linkedLocation == null) {
            linkedLocation = new LinkedLocation(locationId);
            idLocation.put(locationId, linkedLocation);
        }
        return linkedLocation;
    }

    @Override
    public void addHierarchy(int childId, int parentId) {
        if (childId == parentId) {
            throw new IllegalArgumentException("A child cannot be the parent of itself (id was " + childId + ")");
        }
        LinkedLocation parentLocation = getOrCreate(parentId);
        LinkedLocation childLocation = getOrCreate(childId);
        childLocation.parent = parentLocation;
    }

    @Override
    public Location getLocation(int locationId) {
        return idLocation.get(locationId);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CollectionLocationStore [#locationsIds=");
        builder.append(idLocation.size());
        builder.append(", #namesLocations=");
        builder.append(namesLocations.size());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void addAlternativeNames(int locationId, Collection<AlternativeName> alternativeNames) {
        LinkedLocation linkedLocation = getOrCreate(locationId);
        if (linkedLocation != null) {
            linkedLocation.alternativeNames.addAll(alternativeNames);
            for (AlternativeName alternativeName : alternativeNames) {
                namesLocations.add(alternativeName.getName().toLowerCase(), linkedLocation);
            }
        }
    }

    @Override
    public int getHighestId() {
        if (idLocation.isEmpty()) {
            return 0;
        }
        return Collections.max(idLocation.keySet());
    }

    /**
     * <p>
     * In-memory representation of a {@link Location}. This class is mutable and can be updated with new data using
     * {@link #merge(Location)}. It keeps a pointer to its parent in the hierarchy.
     * </p>
     * 
     * @author Philipp Katz
     */
    private static final class LinkedLocation extends AbstractLocation {

        final int id;
        String primaryName;
        final Set<AlternativeName> alternativeNames = CollectionHelper.newHashSet();
        LocationType type;
        GeoCoordinate coordinate;
        Long population;
        LinkedLocation parent;

        public LinkedLocation(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getPrimaryName() {
            return primaryName;
        }

        @Override
        public Collection<AlternativeName> getAlternativeNames() {
            return alternativeNames;
        }

        @Override
        public LocationType getType() {
            return type;
        }
        
        @Override
        public GeoCoordinate getCoordinate() {
            return coordinate;
        }

        @Override
        public Long getPopulation() {
            return population;
        }

        @Override
        public List<Integer> getAncestorIds() {
            List<Integer> parentIds = CollectionHelper.newArrayList();
            if (parent != null) {
                parent.collectAncestors(parentIds);
            }
            return parentIds;
        }

        void collectAncestors(List<Integer> parentIds) {
            parentIds.add(id);
            if (parent != null) {
                parent.collectAncestors(parentIds);
            }
        }

        void merge(Location location) {
            if (location.getId() != id) {
                throw new IllegalArgumentException();
            }
            if (location.getPrimaryName() != null) {
                this.primaryName = location.getPrimaryName();
            }
            if (location.getAlternativeNames() != null) {
                this.alternativeNames.addAll(location.getAlternativeNames());
            }
            if (location.getType() != null) {
                this.type = location.getType();
            }
            if (location.getCoordinate() != null) {
                this.coordinate = location.getCoordinate();
            }
            if (location.getPopulation() != null) {
                this.population = location.getPopulation();
            }
        }
    }

}
