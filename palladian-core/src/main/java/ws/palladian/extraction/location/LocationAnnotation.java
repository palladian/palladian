package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.List;

import ws.palladian.extraction.entity.Annotation;

// XXX this is ugly. Rethink inheritance hierarchy. Make use of a generic Annotation interface
// (see refactoring branch).
public class LocationAnnotation extends Annotation implements Location {

    private final Location location;

    public LocationAnnotation(Annotation annotation, Location location) {
        super(annotation.getStartPosition(), annotation.getValue(), location.getType().toString());
        this.location = location;
    }

    public LocationAnnotation(int startPos, int endPos, String name, LocationType type, Double lat, Double lng) {
        super(startPos, name, type.toString());
        setLength(endPos - startPos);
        this.location = new ImmutableLocation(0, name, null, type, lat, lng, null, null);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Annotation [offset=");
        builder.append(getStartPosition());
        builder.append(", length=");
        builder.append(getLength());
        builder.append(", entity=");
        builder.append(getValue());
        builder.append(", tag=");
        builder.append(getTag());
        builder.append(", location=");
        builder.append(location);
        builder.append("]");
        return builder.toString();
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
    public Double getLatitude() {
        return location.getLatitude();
    }

    @Override
    public Double getLongitude() {
        return location.getLongitude();
    }

    @Override
    public Long getPopulation() {
        return location.getPopulation();
    }

    @Override
    public List<Integer> getAncestorIds() {
        return location.getAncestorIds();
    }

}
