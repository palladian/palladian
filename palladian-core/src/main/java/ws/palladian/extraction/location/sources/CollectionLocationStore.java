package ws.palladian.extraction.location.sources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.extraction.location.AbstractLocation;
import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;

import java.util.*;
import java.util.stream.Collectors;

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

    private final Map<Integer, MutableLocation> idLocation = new HashMap<>();

    private final MultiMap<String, MutableLocation> namesLocations = DefaultMultiMap.createWithSet();

    @Override
    public Collection<Location> getLocations(String locationName, Set<Language> languages) {
        LOGGER.warn("getLocations(String,EnumSet<Language>) is not supported, ignoring language parameter");
        return Collections.<Location>unmodifiableCollection(namesLocations.get(locationName.toLowerCase()));
    }

    @Override
    public void save(Location location) {
        MutableLocation locationCopy = new MutableLocation(location);
        idLocation.put(location.getId(), locationCopy);
        namesLocations.add(location.getPrimaryName().toLowerCase(), locationCopy);
        for (AlternativeName alternativeName : location.getAlternativeNames()) {
            namesLocations.add(alternativeName.getName().toLowerCase(), locationCopy);
        }
    }

    @Override
    public Location getLocation(int locationId) {
        return idLocation.get(locationId);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CollectionLocationStore [#locations=");
        builder.append(idLocation.size());
        builder.append(", #names=");
        builder.append(namesLocations.size());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void addAlternativeNames(int locationId, Collection<AlternativeName> alternativeNames) {
        MutableLocation location = idLocation.get(locationId);
        if (location == null) {
            throw new IllegalArgumentException("No location with ID " + locationId + " in collection.");
        }
        location.alternativeNames.addAll(alternativeNames);
        for (AlternativeName alternativeName : alternativeNames) {
            namesLocations.add(alternativeName.getName().toLowerCase(), location);
        }
    }

    @Override
    public int getHighestId() {
        return idLocation.isEmpty() ? 0 : Collections.max(idLocation.keySet());
    }

    @Override
    public void startImport() {
        // nothing to to
    }

    @Override
    public void finishImport() {
        // nothing to to
    }

    /**
     * An in-memory representation of a {@link Location}.
     *
     * @author Philipp Katz
     */
    private static final class MutableLocation extends AbstractLocation {

        final int id;
        String primaryName;
        Set<AlternativeName> alternativeNames;
        LocationType type;
        GeoCoordinate coordinate;
        Long population;
        List<Integer> ancestorIds;

        public MutableLocation(Location location) {
            this.id = location.getId();
            this.primaryName = location.getPrimaryName();
            this.alternativeNames = new HashSet<>(location.getAlternativeNames());
            this.type = location.getType();
            this.coordinate = location.getCoordinate();
            this.population = location.getPopulation();
            this.ancestorIds = location.getAncestorIds();
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
        public Collection<String> getAlternativeNameStrings() {
            if (alternativeNames == null) {
                return new HashSet<>();
            }
            return alternativeNames.stream().map(AlternativeName::getName).collect(Collectors.toSet());
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
            return ancestorIds;
        }

        @Override
        public Map<String, Object> getMetaData() {
            return null;
        }
    }

}
