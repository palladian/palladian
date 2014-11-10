package ws.palladian.extraction.location.disambiguation;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.FeatureVector;
import ws.palladian.extraction.location.Location;

/**
 * <p>
 * A {@link Location} with extracted features represented by a {@link FeatureVector}.
 * </p>
 * 
 * @author pk
 */
public final class ClassifiableLocation {

    private final Location location;
    private final FeatureVector featureVector;

    public ClassifiableLocation(Location location, FeatureVector featureVector) {
        Validate.notNull(location, "location must not be null");
        Validate.notNull(featureVector, "featureVector must not be null");
        this.location = location;
        this.featureVector = featureVector;
    }

    public Location getLocation() {
        return location;
    }

    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + featureVector.hashCode();
        result = prime * result + location.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ClassifiableLocation other = (ClassifiableLocation)obj;
        if (!featureVector.equals(other.featureVector)) {
            return false;
        }
        if (!location.equals(other.location)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ClassifiableLocation [");
        builder.append(location.getPrimaryName());
        builder.append(", ");
        builder.append(featureVector);
        builder.append("]");
        return builder.toString();
    }

}