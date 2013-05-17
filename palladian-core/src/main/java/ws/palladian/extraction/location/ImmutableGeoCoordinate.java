package ws.palladian.extraction.location;

public final class ImmutableGeoCoordinate implements GeoCoordinate {

    private final Double lat;
    private final Double lng;

    public ImmutableGeoCoordinate(Double lat, Double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    public Double getLatitude() {
        return lat;
    }

    @Override
    public Double getLongitude() {
        return lng;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DefaultGeoCoordinate [lat=");
        builder.append(lat);
        builder.append(", lng=");
        builder.append(lng);
        builder.append("]");
        return builder.toString();
    }

}