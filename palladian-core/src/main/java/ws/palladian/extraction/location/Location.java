package ws.palladian.extraction.location;

import java.util.List;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.processing.features.PositionAnnotation;

public class Location extends PositionAnnotation {

    private static final String LOCATION_ANNOTATION_NAME = "Location";

    private int id;

    private String primaryName;
    private List<AlternativeName> alternativeNames;
    private LocationType type;
    private Double latitude;
    private Double longitude;
    private Long population;

    public Location() {
        // FIXME
        super(LOCATION_ANNOTATION_NAME, 0, 1, 0, "");
    }

    @Deprecated
    public Location(PositionAnnotation annotation) {
        super(annotation);
    }

    @Deprecated
    public Location(Annotation a) {
        super(LOCATION_ANNOTATION_NAME, a.getOffset(), a.getOffset() + a.getLength(), 0, a.getEntity());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPrimaryName() {
        return primaryName;
    }

    public void setPrimaryName(String primaryName) {
        this.primaryName = primaryName;
    }

    public List<AlternativeName> getAlternativeNames() {
        return alternativeNames;
    }

    public void setAlternativeNames(List<AlternativeName> alternativeNames) {
        this.alternativeNames = alternativeNames;
    }

    public LocationType getType() {
        return type;
    }

    public void setType(LocationType type) {
        this.type = type;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Long getPopulation() {
        return population;
    }

    public void setPopulation(Long population) {
        this.population = population;
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
        builder.append(", startPos=");
        builder.append(getStartPosition());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Location other = (Location)obj;
        if (id != other.id)
            return false;
        return true;
    }

}
