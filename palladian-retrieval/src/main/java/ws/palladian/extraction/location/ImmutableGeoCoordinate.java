package ws.palladian.extraction.location;

import org.apache.commons.lang3.Validate;

// FIXME this is a class for the API module
public final class ImmutableGeoCoordinate extends AbstractGeoCoordinate {

    private final double lat;
    private final double lng;

    /**
     * <p>
     * Create a new {@link ImmutableGeoCoordinate} with the given latitude and longitude.
     * </p>
     * 
     * @param lat The latitude, between -90 and 90 inclusive.
     * @param lng The longitude, between -180 and 180 inclusive.
     * @throws IllegalArgumentException in case latitude/longitude are out of given range.
     */
    public ImmutableGeoCoordinate(double lat, double lng) {
        Validate.isTrue(GeoUtils.validCoordinateRange(lat, lng), "latitude/longitude out of range (%f,%f)", lat, lng);
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