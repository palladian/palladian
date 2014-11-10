package ws.palladian.helper.geo;

/**
 * <p>
 * Implementations of this interface represent geographic coordinates represented by latitude and longitude values.
 * </p>
 * 
 * @author pk
 */
public interface GeoCoordinate {

    /**
     * <p>
     * Null object, which makes it easier to work with non-present coordinates (latitude and longitude are zero, and all
     * {@link #distance(GeoCoordinate)} invocations return a value of {@link GeoUtils#EARTH_MAX_DISTANCE_KM}).
     */
    GeoCoordinate NULL = new AbstractGeoCoordinate() {

        @Override
        public double getLatitude() {
            return 0;
        }

        @Override
        public double getLongitude() {
            return 0;
        }

        @Override
        public double distance(GeoCoordinate other) {
            return GeoUtils.EARTH_MAX_DISTANCE_KM;
        };

        public GeoCoordinate getCoordinate(double distance, double bearing) {
            return NULL;
        };

    };

    /**
     * @return The geographical latitude of this location.
     */
    double getLatitude();

    /**
     * @return The geographical longitude of this location.
     */
    double getLongitude();

    /**
     * <p>
     * Get the distance in kilometers between this and the given {@link GeoCoordinate} on the earth (assuming an earth
     * radius of {@link GeoUtils#EARTH_RADIUS_KM}). <b>Implemenation hint:</b> Calculations using the Haversine formula
     * are usually expensive. In case, this method is run in a busy loop, consider using
     * {@link GeoUtils#approximateDistance(GeoCoordinate, GeoCoordinate)} which sacrifices accuracy, but which is
     * magnitudes faster.
     * </p>
     * 
     * @param other The other location, not <code>null</code>.
     * @return The distance to the other location in kilometers.
     */
    double distance(GeoCoordinate other);

    /**
     * <p>
     * Convert this {@link GeoCoordinate} to DMS coordinates.
     * </p>
     * 
     * @return A DMS string representing the coordinate.
     */
    String toDmsString();

    /**
     * <p>
     * Calculates a (quadratic) bounding box around this {@link GeoCoordinate} with the specified distance in
     * kilometers.
     * </p>
     * 
     * @param distance The distance around the coordinate in kilometers, greater/equal zero.
     * @return An array with four elements specifying the coordinates of the bounding box in the following order:
     *         [south, west, north, east].
     */
    double[] getBoundingBox(double distance);

    /**
     * <p>
     * Get a new point form this {@link GeoCoordinate} with the specified distance and bearing.
     * </p>
     * 
     * @param distance The distance from this coordinate in kilometers, greater/equal zero.
     * @param bearing The bearing (angle) in degrees, which determines in which direction to move. A bearing of 0°
     *            denotes the direction north, 90° east, and so on.
     * @return A new {@link GeoCoordinate} with the specified distance and bearing.
     */
    GeoCoordinate getCoordinate(double distance, double bearing);

}
