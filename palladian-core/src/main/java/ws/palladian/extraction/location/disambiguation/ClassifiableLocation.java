package ws.palladian.extraction.location.disambiguation;

import java.util.Collection;
import java.util.List;

import ws.palladian.core.FeatureVector;
import ws.palladian.extraction.location.AbstractLocation;
import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;

/**
 * <p>
 * A {@link Classifiable} {@link Location} with associated features.
 * </p>
 * 
 * @author pk
 */
public final class ClassifiableLocation extends AbstractLocation {

    private final Location location;
    private final FeatureVector featureVector;

    public ClassifiableLocation(Location location, FeatureVector featureVector) {
        this.location = location;
        this.featureVector = featureVector;
    }
    
    @Override
    public GeoCoordinate getCoordinate() {
        return location.getCoordinate();
    }

    @Override
    public int getId() {
        return location.getId();
    }

    @Override
    public String getPrimaryName() {
        return location.getPrimaryName();
    }

    @Override
    public Collection<AlternativeName> getAlternativeNames() {
        return location.getAlternativeNames();
    }

    @Override
    public LocationType getType() {
        return location.getType();
    }

    @Override
    public Long getPopulation() {
        return location.getPopulation();
    }

    @Override
    public List<Integer> getAncestorIds() {
        return location.getAncestorIds();
    }

    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((featureVector == null) ? 0 : featureVector.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
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
        ClassifiableLocation other = (ClassifiableLocation)obj;
        if (featureVector == null) {
            if (other.featureVector != null)
                return false;
        } else if (!featureVector.equals(other.featureVector))
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LocationInstance [location=");
        builder.append(location);
        builder.append(", featureVector=");
        builder.append(featureVector);
        builder.append("]");
        return builder.toString();
    }

}