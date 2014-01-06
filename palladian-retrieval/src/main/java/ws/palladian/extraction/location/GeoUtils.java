package ws.palladian.extraction.location;

import static java.lang.Math.toRadians;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Various utility functions and constants for geographic purposes.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class GeoUtils {

    public static final String DMS = "([-+]?\\d{1,3}(?:\\.\\d{1,10})?)[°ºd:]" + // degree
            "(?:\\s?(\\d{1,2}(?:\\.\\d{1,10})?))?['′:]?" + // minute
            "(?:\\s?(\\d{1,2}(?:\\.\\d{1,10})?))?(?:\"|″|'')?" + // second
            "(?:\\s?(N|S|W|E|North|South|West|East))?"; // direction

    /** The radius of the earth in kilometers. */
    public static final double EARTH_RADIUS_KM = 6371;

    /** Circumference of the earth at the equator in kilometers. */
    public static final double EARTH_CIRCUMFERENCE_KM = 40075.16;

    /** For parsing a single DMS expression. */
    private static final Pattern PATTERN_PARSE_DMS = Pattern.compile(DMS);

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
     * @param coordinates The {@link GeoCoordinate}s, not empty or <code>null</code>.
     * @return An array with the midpoint, first element is latitude, second element is longitude.
     */
    public static final GeoCoordinate getMidpoint(Collection<? extends GeoCoordinate> coordinates) {
        Validate.notEmpty(coordinates, "locations must not be empty");
        int count = coordinates.size();
        if (count == 1) { // shortcut
            return CollectionHelper.getFirst(coordinates);
        }
        double x = 0;
        double y = 0;
        double z = 0;
        for (GeoCoordinate location : coordinates) {
            double latRad = Math.toRadians(location.getLatitude());
            double lngRad = Math.toRadians(location.getLongitude());
            x += Math.cos(latRad) * Math.cos(lngRad);
            y += Math.cos(latRad) * Math.sin(lngRad);
            z += Math.sin(latRad);
        }
        x /= count;
        y /= count;
        z /= count;
        if (Math.abs(x) < 1e-9 || Math.abs(y) < 1e-9 || Math.abs(z) < 1e-9) {
            return new ImmutableGeoCoordinate(0., 0.);
        }
        double lngRad = Math.atan2(y, x);
        double hypRad = Math.sqrt(x * x + y * y);
        double latRad = Math.atan2(z, hypRad);
        double lng = Math.toDegrees(lngRad);
        double lat = Math.toDegrees(latRad);
        return new ImmutableGeoCoordinate(lat, lng);
    }

    /**
     * <p>
     * Calculate the center of minimum distance (also called "median center" or "geometric median" sometimes). This is
     * the point that minimizes the total distances to all given coordinates. In contrast to the midpoint, this cannot
     * be calculated by a single formula but has to be determined iteratively. The algorithm employed here is described
     * on <a href="http://www.geomidpoint.com/calculation.html">GeoMidpoint</a>.
     * </p>
     * 
     * @param coordinates The {@link GeoCoordinate}s, not empty or <code>null</code>.
     * @return A {@link GeoCoordinate} representing the center of minimum distance.
     * @see <a href="http://en.wikipedia.org/wiki/Geometric_median">Wikipedia: Geometric median</a>
     * @see Elementary Statistics for Geographers, James E. Burt, Gerald M. Barber, Guilford Press, 1996
     */
    public static final GeoCoordinate getCenterOfMinimumDistance(Collection<? extends GeoCoordinate> coordinates) {
        Validate.notEmpty(coordinates, "coordinates must not be empty");

        if (coordinates.size() == 1) { // shortcut
            return CollectionHelper.getFirst(coordinates);
        }

        // algorithm implemented from explanation at: http://www.geomidpoint.com/calculation.html
        GeoCoordinate currentPoint = getMidpoint(coordinates); // step 1
        double minimumDistance = 0; // step 2: minimum distance = sum of distances to midpoint
        for (GeoCoordinate coordinate : coordinates) {
            minimumDistance += currentPoint.distance(coordinate);
        }

        // step 3: get total distance between each coordinate in the collection and other coordinates,
        // if total distance is smaller, this location becomes currentPoint, and update minimumDistance
        for (GeoCoordinate coordinate1 : coordinates) {
            double currentDistance = 0;
            for (GeoCoordinate coordinate2 : coordinates) {
                currentDistance += coordinate1.distance(coordinate2);
            }
            if (currentDistance < minimumDistance) {
                minimumDistance = currentDistance;
                currentPoint = coordinate1;
            }
        }

        double testDistance = EARTH_RADIUS_KM * Math.PI / 2; // step 4
        // steps 5-8
        boolean foundNewSmallest = false;
        int iterations = 0; // prevent infinite loops, just in case
        while (iterations++ < 5000 && (foundNewSmallest || testDistance >= 2.0e-8 * EARTH_RADIUS_KM)) {
            GeoCoordinate[] testPoints = getTestPoints(currentPoint, testDistance);
            double tempMinimumDistance = Double.MAX_VALUE;
            GeoCoordinate tempCurrentPoint = null;
            for (GeoCoordinate testPoint : testPoints) {
                double currentDistance = 0;
                for (GeoCoordinate coordinate : coordinates) {
                    currentDistance += testPoint.distance(coordinate);
                }
                if (currentDistance < tempMinimumDistance) {
                    tempMinimumDistance = currentDistance;
                    tempCurrentPoint = testPoint;
                }
            }
            if (tempMinimumDistance < minimumDistance) {
                currentPoint = tempCurrentPoint;
                minimumDistance = tempMinimumDistance;
                foundNewSmallest = true;
            } else {
                testDistance /= 2;
                foundNewSmallest = false;
            }
        }
        return currentPoint;
    }

    /**
     * Get eight "test points" around the given coordinate, with the specified distance and bearings of [0, 45, 90, ...
     * 315].
     * 
     * @param coordinate The center coordinate.
     * @param distance The distance.
     * @return An array with eight coordinates around the specified coordinate, each with the specified distance.
     */
    static GeoCoordinate[] getTestPoints(GeoCoordinate coordinate, double distance) {
        GeoCoordinate[] result = new GeoCoordinate[8];
        for (int i = 0; i < 8; i++) {
            result[i] = coordinate.getCoordinate(distance, i * 45);
        }
        return result;
    }

    /**
     * Normalize a longitude value to an interval -180 ... 180°.
     */
    public static double normalizeLongitude(double lng) {
        return (lng + 3 * Math.PI) % (2 * Math.PI) - Math.PI;
    }

    /**
     * <p>
     * Convert a DMS coordinate (degrees, minutes, seconds) to decimal degree.
     * </p>
     * 
     * @param dmsString The string with the DMS coordinate, not <code>null</code> or empty.
     * @return The double value with decimal degree.
     * @throws NumberFormatException in case the string could not be parsed.
     */
    public static final double parseDms(String dmsString) {
        Validate.notEmpty(dmsString, "dmsString must not be empty");
        Matcher matcher = PATTERN_PARSE_DMS.matcher(dmsString);
        if (!matcher.matches()) {
            throw new NumberFormatException("The string " + dmsString + " could not be parsed in DMS format.");
        }
        double degrees = Double.valueOf(matcher.group(1)); // degree value, including sign
        int sign; // the sign, determined either from hemisphere/meridien, or degree sign
        String ws = matcher.group(4);
        if (ws != null) {
            sign = "W".equals(ws) || "S".equals(ws) || "West".equals(ws) || "South".equals(ws) ? -1 : 1;
        } else {
            sign = matcher.group(1).startsWith("-") ? -1 : 1;
        }
        double minutes = matcher.group(2) != null ? Double.valueOf(matcher.group(2)) : 0;
        double seconds = matcher.group(3) != null ? Double.valueOf(matcher.group(3)) : 0;
        return sign * (Math.abs(degrees) + minutes / 60. + seconds / 3600.);
    }

    /**
     * <p>
     * Use "<a href="http://en.wikipedia.org/wiki/Equirectangular_projection>Equirectangular approximation</a>" to
     * quickly calculate the distance between two coordinates. This performs better than
     * {@link GeoCoordinate#distance(GeoCoordinate)} but is less exact. For small distances, the discrepancy is
     * negligible.
     * </p>
     * 
     * @param c1 First coordinate, not <code>null</code>.
     * @param c2 Second coordinate, not <code>null</code>.
     * @return The approximate distance between the two coordinates.
     */
    // XXX consider moving directly to GeoCoordinate
    public static final double approximateDistance(GeoCoordinate c1, GeoCoordinate c2) {
        Validate.notNull(c1, "c1 must not be null");
        Validate.notNull(c2, "c2 must not be null");
        double lat1 = toRadians(c1.getLatitude());
        double lat2 = toRadians(c2.getLatitude());
        double lon2 = toRadians(c2.getLongitude());
        double lon1 = toRadians(c1.getLongitude());
        double x = (lon2 - lon1) * Math.cos((lat1 + lat2) / 2);
        double y = (lat2 - lat1);
        return Math.sqrt(x * x + y * y) * EARTH_RADIUS_KM;
    }

    /**
     * <p>
     * Check, if the given latitude and longitude pair are in valid coordinate range (i.e. -90 <= latitude <= 90 and
     * -180 <= longitude <= 180).
     * </p>
     * 
     * @param lat The latitude.
     * @param lng The longitude.
     * @return <code>true</code> in case the latitude and longitude are valid for a coordinate, <code>false</code>
     *         otherwise.
     */
    public static boolean validCoordinateRange(double lat, double lng) {
        return -90 <= lat && lat <= 90 && -180 <= lng && lng <= 180;
    }

    private GeoUtils() {
        // no instances.
    }

}
