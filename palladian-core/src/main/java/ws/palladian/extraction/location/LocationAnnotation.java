package ws.palladian.extraction.location;

import ws.palladian.helper.math.MathHelper;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;

/**
 * <p>
 * An {@link Annotation} in a text associated with geographic locations. The value for {@link #getTag()} is the string
 * value of the associated {@link LocationType}. More information about the specific disambiguated location can be
 * retrieved using {@link #getLocation()}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class LocationAnnotation extends ImmutableAnnotation {

    private final Location location;

    private final double trust;

    public LocationAnnotation(int startPosition, String value, Location location, double trust) {
        super(startPosition, value, location.getType().toString());
        this.location = location;
        this.trust = trust;
    }

    public LocationAnnotation(int startPosition, String value, Location location) {
        this(startPosition, value, location, -1);
    }

    public LocationAnnotation(Annotation annotation, Location location, double trust) {
        this(annotation.getStartPosition(), annotation.getValue(), location, trust);
    }

    public LocationAnnotation(Annotation annotation, Location location) {
        this(annotation, location, -1);
    }

    /**
     * @return The {@link Location} assigned to this annotation.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * @return A trust value, indicating how sure the location extractor was about the assigned location for this
     *         annotation. A value of <code>-1</code> indicates, that no trust value was assigned.
     */
    public double getTrust() {
        return trust;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LocationAnnotation [span=");
        builder.append(getStartPosition());
        builder.append("-");
        builder.append(getEndPosition());
        builder.append(", value=");
        builder.append(getValue());
        builder.append(", location=");
        builder.append(location);
        if (trust >= 0) {
            builder.append(", trust=");
            builder.append(MathHelper.round(trust, 2));
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getEndPosition();
        result = prime * result + ((location == null) ? 0 : location.getId());
        result = prime * result + getStartPosition();
        result = prime * result + ((getValue() == null) ? 0 : getValue().hashCode());
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
        LocationAnnotation other = (LocationAnnotation)obj;
        if (getEndPosition() != other.getEndPosition())
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (location.getId() != other.location.getId())
            return false;
        if (getStartPosition() != other.getStartPosition())
            return false;
        if (getValue() == null) {
            if (other.getValue() != null)
                return false;
        } else if (!getValue().equals(other.getValue()))
            return false;
        return true;
    }

}
