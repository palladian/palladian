package ws.palladian.extraction.location;

import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.processing.features.Annotated;

public class LocationAnnotation implements Annotated {

    private final int startPosition;
    private final int endPosition;
    private final String value;
    private final Location location;

    public LocationAnnotation(int startPosition, int endPosition, String value, Location location) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.value = value;
        this.location = location;
    }

    public LocationAnnotation(Annotated annotation, Location location) {
        this.startPosition = annotation.getStartPosition();
        this.endPosition = annotation.getEndPosition();
        this.value = annotation.getValue();
        this.location = location;
    }

    @Override
    public int getStartPosition() {
        return startPosition;
    }

    @Override
    public int getEndPosition() {
        return endPosition;
    }

    @Override
    public String getTag() {
        return location.getType().toString();
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public int compareTo(Annotated o) {
        return this.startPosition - o.getStartPosition();
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public boolean overlaps(Annotated annotated) {
        // FIXME this needs to go in parent -> duplicate of NerHelper
        return NerHelper.overlaps(this, annotated);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LocationAnnotation [startPosition=");
        builder.append(startPosition);
        builder.append(", endPosition=");
        builder.append(endPosition);
        builder.append(", value=");
        builder.append(value);
        builder.append(", location=");
        builder.append(location);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + endPosition;
        result = prime * result + ((location == null) ? 0 : location.getId());
        result = prime * result + startPosition;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        if (endPosition != other.endPosition)
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (location.getId() != other.location.getId())
            return false;
        if (startPosition != other.startPosition)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
