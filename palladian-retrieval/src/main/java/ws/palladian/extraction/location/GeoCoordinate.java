package ws.palladian.extraction.location;

// FIXME this is a class for the API module
/**
 * <p>
 * Implementations of this interface represent geographic coordinates represented by latitude and longitude values.
 * </p>
 * 
 * @author pk
 */
public interface GeoCoordinate {

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
     * radius of {@link GeoUtils#EARTH_RADIUS_KM}).
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
