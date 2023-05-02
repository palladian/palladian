package ws.palladian.helper.geo;

import java.io.Serializable;

public final class ImmutableGeoCoordinateReducedPrecision extends AbstractGeoCoordinate implements Serializable {
    private static final long serialVersionUID = 1L;

    private final float lat;
    private final float lng;

    /**
     * <p>
     * Create a new {@link ImmutableGeoCoordinateReducedPrecision} with the given latitude and longitude.
     * </p>
     *
     * @param lat The latitude, between -90 and 90 inclusive.
     * @param lng The longitude, between -180 and 180 inclusive.
     * @throws IllegalArgumentException in case latitude/longitude are out of given range.
     */
    public ImmutableGeoCoordinateReducedPrecision(float lat, float lng) {
        GeoUtils.validateCoordinateRange(lat, lng);
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    public double getLatitude() {
        return lat;
    }

    @Override
    public double getLongitude() {
        return lng;
    }
}
