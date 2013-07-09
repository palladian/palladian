package ws.palladian.extraction.location;

import ws.palladian.processing.features.Annotated;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * An {@link Annotation} in a text associated with geographic locations. The value for {@link #getTag()} is the string
 * value of the associated {@link LocationType}. More information about the specific disambiguated location can be
 * retrieved using {@link #getLocation()}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class LocationAnnotation extends Annotation {

    private final Location location;
    
    public LocationAnnotation(int startPosition, String value, Location location) {
        super(startPosition, value, location.getType().toString());
        this.location = location;
    }

    public LocationAnnotation(Annotated annotation, Location location) {
        this(annotation.getStartPosition(), annotation.getValue(), location);
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LocationAnnotation [startPosition=");
        builder.append(getStartPosition());
        builder.append(", endPosition=");
        builder.append(getEndPosition());
        builder.append(", value=");
        builder.append(getValue());
        builder.append(", location=");
        builder.append(location);
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
