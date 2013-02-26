package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * A geographic location, like a city, country, continent, etc.
 * </p>
 * 
 * @author Philipp Katz
 */
public class Location {

    private final int id;
    private final String primaryName;
    private final List<AlternativeName> alternativeNames;
    private final LocationType type;
    private final Double latitude;
    private final Double longitude;
    private final Long population;

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
     * @param population The population, or <code>null</code> if no populartion values exist.
     */
    public Location(int id, String primaryName, List<AlternativeName> alternativeNames, LocationType type,
            Double latitude, Double longitude, Long population) {
        Validate.notNull(primaryName, "primaryName must not be null");
        Validate.notNull(type, "type must not be null");
        this.id = id;
        this.primaryName = primaryName;
        this.alternativeNames = alternativeNames != null ? alternativeNames : Collections.<AlternativeName> emptyList();
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.population = population;
    }

    public int getId() {
        return id;
    }

    public String getPrimaryName() {
        return primaryName;
    }

    public List<AlternativeName> getAlternativeNames() {
        return Collections.unmodifiableList(alternativeNames);
    }

    public LocationType getType() {
        return type;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Long getPopulation() {
        return population;
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
        Location other = (Location)obj;
        if (id != other.id)
            return false;
        return true;
    }

}
