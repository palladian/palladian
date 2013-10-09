package ws.palladian.retrieval.wikipedia;

import ws.palladian.extraction.location.AbstractGeoCoordinate;

/**
 * Utility class representing a location extracted from Wikipedia coordinate markup.
 */
public final class MarkupLocation extends AbstractGeoCoordinate {
    double lat;
    double lng;
    Long population;
    String display;
    String name;
    String type;
    String region;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MarkupLocation [lat=");
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
}