package ws.palladian.extraction.location;

import java.util.Collection;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.math.MathHelper;

public final class GeoUtils {

    public static final double getDistance(GeoCoordinate c1, GeoCoordinate c2) {
        Validate.notNull(c1, "c1 must not be null");
        Validate.notNull(c2, "c2 must not be null");
        Double lat1 = c1.getLatitude();
        Double lng1 = c1.getLongitude();
        Double lat2 = c2.getLatitude();
        Double lng2 = c2.getLongitude();
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return Integer.MAX_VALUE;
        }
        return MathHelper.computeDistanceBetweenWorldCoordinates(lat1, lng1, lat2, lng2);
    }

    /**
     * <p>
     * Get the geographical midpoint of the given locations. Locations without latitude/longitude values are ignored.
     * The maths behind the calculation are described <a href="http://www.geomidpoint.com/calculation.html">here</a>.
     * The idea of the geographical midpoint can be described as follows (taken from link): <i>Imagine that several
     * weights are placed at various points on a world globe and then the globe is allowed to rotate freely until the
     * heaviest part of the globe is pulled by gravity until it is facing downward. Then the lowest point on the globe
     * would be the geographic midpoint for all of the weighted locations.</i>.
     * </p>
     * 
     * @param coordinates The {@link Location}s, not empty.
     * @return An array with the midpoint, first element is latitude, second element is longitude.
     */
    public static final GeoCoordinate getMidpoint(Collection<? extends GeoCoordinate> coordinates) {
        Validate.notEmpty(coordinates, "locations must not be empty");
        double x = 0;
        double y = 0;
        double z = 0;
        int count = 0;
        for (GeoCoordinate location : coordinates) {
            if (location.getLatitude() == null || location.getLongitude() == null) {
                continue;
            }
            double latRad = location.getLatitude() * Math.PI / 180;
            double lngRad = location.getLongitude() * Math.PI / 180;
            x += Math.cos(latRad) * Math.cos(lngRad);
            y += Math.cos(latRad) * Math.sin(lngRad);
            z += Math.sin(latRad);
            count++;
        }
        x /= count;
        y /= count;
        z /= count;
        final double t = Math.pow(10, -9);
        if (Math.abs(x) < t || Math.abs(y) < t || Math.abs(z) < t) {
            return new ImmutableGeoCoordinate(0., 0.);
        }
        double lngRad = Math.atan2(y, x);
        double hypRad = Math.sqrt(x * x + y * y);
        double latRad = Math.atan2(z, hypRad);
        double lng = (lngRad * 180 / Math.PI);
        double lat = (latRad * 180 / Math.PI);
        return new ImmutableGeoCoordinate(lat, lng);
    }

    /**
     * <p>
     * Calculates a (quadratic) bounding box around the given {@link GeoCoordinate} with the specified distance in
     * kilometers.
     * </p>
     * 
     * @param c The {@link GeoCoordinate} around which to create the bounding box, not <code>null</code>.
     * @param distance The distance around the coordinate in kilometers, greater/equal zero.
     * @return An array with four elements specifying the coordinates of the bounding box in the following order:
     *         [south, west, north, east].
     */
    public static final double[] getBoundingBox(GeoCoordinate c, double distance) {
        Validate.notNull(c, "c must not be null");
        Validate.isTrue(distance >= 0, "distance must be equal/greater zero");

        // http://vinsol.com/blog/2011/08/30/geoproximity-search-with-mysql/
        double lat1 = c.getLatitude() - distance / 111.04;
        double lat2 = c.getLatitude() + distance / 111.04;
        double long1 = c.getLongitude() - distance / Math.abs(Math.cos(Math.toRadians(c.getLatitude())) * 111.04);
        double long2 = c.getLongitude() + distance / Math.abs(Math.cos(Math.toRadians(c.getLatitude())) * 111.04);
        return new double[] {lat1, long1, lat2, long2};
    }

    private GeoUtils() {
        // no instances.
    }

}
