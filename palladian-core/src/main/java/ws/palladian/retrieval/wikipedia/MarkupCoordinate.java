package ws.palladian.retrieval.wikipedia;

import ws.palladian.extraction.location.AbstractGeoCoordinate;

/**
 * Utility class representing a location extracted from Wikipedia coordinate markup.
 */
public final class MarkupCoordinate extends AbstractGeoCoordinate {
    
    private final double lat;
    private final double lng;
    private final Long population;
    private final String display;
    private final String name;
    private final String type;
    private final String region;

    public MarkupCoordinate(double lat, double lng, String display, String type) {
        this(lat, lng, null, null, display, type, null);
    }

    public MarkupCoordinate(double lat, double lng, String name, Long population, String display, String type,
            String region) {
        this.lat = lat;
        this.lng = lng;
        this.name = name;
        this.population = population;
        this.display = display;
        this.type = type;
        this.region = region;
    }

    @Override
    public double getLatitude() {
        return lat;
    }

    @Override
    public double getLongitude() {
        return lng;
    }

    public String getDisplay() {
        return display;
    }

    public Long getPopulation() {
        return population;
    }
    
    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MarkupCoordinate [lat=");
        builder.append(lat);
        builder.append(", lng=");
        builder.append(lng);
        builder.append(", population=");
        builder.append(population);
        builder.append(", display=");
        builder.append(display);
        builder.append(", name=");
        builder.append(name);
        builder.append(", type=");
        builder.append(type);
        builder.append(", region=");
        builder.append(region);
        builder.append("]");
        return builder.toString();
    }

}
