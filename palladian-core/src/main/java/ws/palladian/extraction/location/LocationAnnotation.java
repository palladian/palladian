package ws.palladian.extraction.location;

import ws.palladian.extraction.entity.Annotation;

// XXX this is ugly. Rethink inheritance hierarchy. Make use of a generic Annotation interface
// (see refactoring branch).
public class LocationAnnotation extends Annotation {

    private final Location location;

    public LocationAnnotation(Annotation annotation, Location location) {
        super(annotation.getOffset(), annotation.getEntity(), location.getType().toString());
        this.location = location;
    }

    public LocationAnnotation(int startPos, int endPos, String name, LocationType type, Double lat, Double lng) {
        super(startPos, name, type.toString());
        setLength(endPos - startPos);
        this.location = new Location(0, name, null, type, lat, lng, null);
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Annotation [offset=");
        builder.append(getOffset());
        builder.append(", length=");
        builder.append(getLength());
        builder.append(", entity=");
        builder.append(getEntity());
        builder.append(", tag=");
        builder.append(getMostLikelyTagName());
        builder.append(", location=");
        builder.append(location);
        builder.append("]");
        return builder.toString();
    }

}
