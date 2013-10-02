package ws.palladian.extraction.location;

import org.apache.commons.lang3.Validate;

// FIXME this is a class for the API module
public final class ImmutableGeoCoordinate extends AbstractGeoCoordinate {

    private final Double lat;
    private final Double lng;

    /**
     * <p>
     * Create a new {@link ImmutableGeoCoordinate} with the given latitude and longitude.
     * </p>
     * 
     * @param lat The latitude, between -90 and 90 inclusive, not <code>null</code>.
     * @param lng The longitude, between -180 and 180 inclusive, not <code>null</code>.
     * @throws IllegalArgumentException in case latitude/longitude are out of given range.
     */
    public ImmutableGeoCoordinate(Double lat, Double lng) {
        Validate.inclusiveBetween(-90., 90., lat);
        Validate.inclusiveBetween(-180., 180., lng);
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

}