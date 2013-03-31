package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Immutable default implementation of a {@link Location}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class ImmutableLocation implements Location {

    private final int id;
    private final String primaryName;
    private final Collection<AlternativeName> alternativeNames;
    private final LocationType type;
    private final Double latitude;
    private final Double longitude;
    private final Long population;
    private final List<Integer> ancestorIds;

    /**
     * <p>
     * Create a new location with the specified attributes.
     * </p>
     * 
     * @param id The unique identifier of the location.
     * @param primaryName The primary name of the location, not <code>null</code>.
     * @param alternativeNames A list of potential alternative names for the location, may be <code>null</code>, if no
     *            alternative names exist.
     * @param type The type of the location, not <code>null</code>.
     * @param latitude The latitude, or <code>null</code> if no coordinates exist.
     * @param longitude The longitude, or <code>null</code> if no coordinates exist.
     * @param population The population, or <code>null</code> if no population values exist.
     * @param ancestorIds The IDs of ancestor {@link ImmutableLocation}s, or <code>null</code> if no ancestors exist.
     */
    public ImmutableLocation(int id, String primaryName, Collection<AlternativeName> alternativeNames, LocationType type,
            Double latitude, Double longitude, Long population, List<Integer> ancestorIds) {
        Validate.notNull(primaryName, "primaryName must not be null");
        Validate.notNull(type, "type must not be null");
        this.id = id;
        this.primaryName = primaryName;
        this.alternativeNames = alternativeNames != null ? alternativeNames : Collections.<AlternativeName> emptyList();
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.population = population;
        this.ancestorIds = ancestorIds != null ? ancestorIds : Collections.<Integer> emptyList();
    }

    /**
     * <p>
     * Create a new location with the specified attributes.
     * </p>
     * 
     * @param id The unique identifier of the location.
     * @param primaryName The primary name of the location, not <code>null</code>.
     * @param type The type of the location, not <code>null</code>.
     * @param latitude The latitude, or <code>null</code> if no coordinates exist.
     * @param longitude The longitude, or <code>null</code> if no coordinates exist.
     * @param population The population, or <code>null</code> if no population values exist.
     */
    public ImmutableLocation(int id, String primaryName, LocationType type, Double latitude, Double longitude, Long population) {
        this(id, primaryName, null, type, latitude, longitude, population, null);
    }

    /**
     * <p>
     * Copy an existing {@link Location} and add alternative names and ancestor IDs.
     * </p>
     * 
     * @param location The {@link Location} for which to create a copy, not <code>null</code>.
     * @param alternativeNames A list of potential alternative names for the location, may be <code>null</code>, if no
     *            alternative names exist.
     * @param ancestorIds The IDs of ancestor {@link ImmutableLocation}s, or <code>null</code> if no ancestors exist.
     */
    public ImmutableLocation(Location location, Collection<AlternativeName> alternativeNames, List<Integer> ancestorIds) {
        this(location.getId(), location.getPrimaryName(), alternativeNames, location.getType(), location.getLatitude(),
                location.getLongitude(), location.getPopulation(), ancestorIds);
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.extraction.location.Location#getId()
     */
    @Override
    public int getId() {
        return id;
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.extraction.location.Location#getPrimaryName()
     */
    @Override
    public String getPrimaryName() {
        return primaryName;
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.extraction.location.Location#getAlternativeNames()
     */
    @Override
    public Collection<AlternativeName> getAlternativeNames() {
        return Collections.unmodifiableCollection(alternativeNames);
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.extraction.location.Location#getType()
     */
    @Override
    public LocationType getType() {
        return type;
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.extraction.location.Location#getLatitude()
     */
    @Override
    public Double getLatitude() {
        return latitude;
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.extraction.location.Location#getLongitude()
     */
    @Override
    public Double getLongitude() {
        return longitude;
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.extraction.location.Location#getPopulation()
     */
    @Override
    public Long getPopulation() {
        return population;
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.extraction.location.Location#getAncestorIds()
     */
    @Override
    public List<Integer> getAncestorIds() {
        return Collections.unmodifiableList(ancestorIds);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Location [id=");
        builder.append(id);
        builder.append(", primaryName=");
        builder.append(primaryName);
        // builder.append(", alternativeNames=");
        // builder.append(alternativeNames);
        builder.append(", type=");
        builder.append(type);
        builder.append(", latitude=");
        builder.append(latitude);
        builder.append(", longitude=");
        builder.append(longitude);
        builder.append(", population=");
        builder.append(population);
        builder.append(", ancestorIds=");
        builder.append(ancestorIds);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
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
        ImmutableLocation other = (ImmutableLocation)obj;
        if (id != other.id)
            return false;
        return true;
    }

}
