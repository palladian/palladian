package ws.palladian.helper.geo;

import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.util.FastMath;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Distance;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;
import static org.apache.commons.math3.util.FastMath.cos;
import static org.apache.commons.math3.util.FastMath.sin;

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

    /** The maximum possible distance between two points on earth (i.e. half the circumference). */
    public static final double EARTH_MAX_DISTANCE_KM = GeoUtils.EARTH_CIRCUMFERENCE_KM * 0.5;

    /** For parsing a single DMS expression. */
    private static final Pattern PATTERN_PARSE_DMS = Pattern.compile(DMS);

    /** Distance function between two {@link GeoCoordinate}s. */
    public static final Distance<GeoCoordinate> DISTANCE = GeoCoordinate::distance;

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
    public static GeoCoordinate getMidpoint(Collection<? extends GeoCoordinate> coordinates) {
        Validate.notEmpty(coordinates, "locations must not be empty");
        int count = coordinates.size();
        if (count == 1) { // shortcut
            return CollectionHelper.getFirst(coordinates);
        }
        double x = 0;
        double y = 0;
        double z = 0;
        for (GeoCoordinate location : coordinates) {
            double latRad = FastMath.toRadians(location.getLatitude());
            double lngRad = FastMath.toRadians(location.getLongitude());
            x += FastMath.cos(latRad) * FastMath.cos(lngRad);
            y += FastMath.cos(latRad) * FastMath.sin(lngRad);
            z += FastMath.sin(latRad);
        }
        x /= count;
        y /= count;
        z /= count;
        if (Math.abs(x) < 1e-9 || Math.abs(y) < 1e-9 || Math.abs(z) < 1e-9) {
            return GeoCoordinate.from(0., 0.);
        }
        double lngRad = FastMath.atan2(y, x);
        double hypRad = FastMath.sqrt(x * x + y * y);
        double latRad = FastMath.atan2(z, hypRad);
        double lng = FastMath.toDegrees(lngRad);
        double lat = FastMath.toDegrees(latRad);
        return GeoCoordinate.from(lat, lng);
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
    public static GeoCoordinate getCenterOfMinimumDistance(Collection<? extends GeoCoordinate> coordinates) {
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
     * @param distance   The distance.
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
     * <p>
     * Convert a DMS coordinate (degrees, minutes, seconds) to decimal degree.
     * </p>
     *
     * @param dmsString The string with the DMS coordinate, not <code>null</code> or empty.
     * @return The double value with decimal degree.
     * @throws NumberFormatException in case the string could not be parsed.
     */
    public static double parseDms(String dmsString) {
        Validate.notEmpty(dmsString, "dmsString must not be empty");
        Matcher matcher = PATTERN_PARSE_DMS.matcher(dmsString);
        if (!matcher.matches()) {
            throw new NumberFormatException("The string " + dmsString + " could not be parsed in DMS format.");
        }
        double degrees = Double.parseDouble(matcher.group(1)); // degree value, including sign
        int sign; // the sign, determined either from hemisphere/meridien, or degree sign
        String ws = matcher.group(4);
        if (ws != null) {
            sign = "W".equals(ws) || "S".equals(ws) || "West".equals(ws) || "South".equals(ws) ? -1 : 1;
        } else {
            sign = matcher.group(1).startsWith("-") ? -1 : 1;
        }
        double minutes = matcher.group(2) != null ? Double.parseDouble(matcher.group(2)) : 0;
        double seconds = matcher.group(3) != null ? Double.parseDouble(matcher.group(3)) : 0;
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
     * @return The approximate distance between the two coordinates in kilometers.
     */
    public static double approximateDistance(double lat1, double lng1, double lat2, double lng2) {
        double rlat1 = toRadians(lat1);
        double rlat2 = toRadians(lat2);
        double rlng1 = toRadians(lng1);
        double rlng2 = toRadians(lng2);

        double x = (rlng2 - rlng1) * FastMath.cos((rlat1 + rlat2) / 2);
        double y = (rlat2 - rlat1);
        return Math.sqrt(x * x + y * y) * EARTH_RADIUS_KM;
    }

    public static double approximateDistance(GeoCoordinate c1, GeoCoordinate c2) {
        Validate.notNull(c1, "c1 must not be null");
        Validate.notNull(c2, "c2 must not be null");
        return approximateDistance(c1.getLatitude(), c1.getLongitude(), c2.getLatitude(), c2.getLongitude());
    }

    /**
     * Compute the exact distance between two coordinates.
     *
     * @param lat1 Latitude Point 1.
     * @param lng1 Longitude Point 1.
     * @param lat2 Latitude Point 2.
     * @param lng2 Longitude Point 2.
     * @return The distance between the two points in kilometers.
     */
    public static double computeDistance(double lat1, double lng1, double lat2, double lng2) {
        double rlat1 = toRadians(lat1);
        double rlng1 = toRadians(lng1);
        double rlat2 = toRadians(lat2);
        double rlng2 = toRadians(lng2);
        double dLat = (rlat2 - rlat1) / 2;
        double dLon = (rlng2 - rlng1) / 2;
        double a = sin(dLat) * sin(dLat) + cos(rlat1) * cos(rlat2) * sin(dLon) * sin(dLon);
        return 2 * EARTH_RADIUS_KM * FastMath.atan2(sqrt(a), sqrt(1 - a));
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
     * otherwise.
     */
    public static boolean isValidCoordinateRange(double lat, double lng) {
        return -90 <= lat && lat <= 90 && -180 <= lng && lng <= 180;
    }

    /**
     * <p>
     * Check, if the given latitude and longitude pair are in valid coordinate range (i.e. -90 <= latitude <= 90 and
     * -180 <= longitude <= 180).
     * </p>
     *
     * @param lat The latitude.
     * @param lng The longitude.
     * @throws IllegalArgumentException In case the latitude and/or longitude are out of range.
     */
    public static void validateCoordinateRange(double lat, double lng) {
        if (!isValidCoordinateRange(lat, lng)) {
            String message = String.format(Locale.US, "latitude and/or longitude out of range (%f,%f)", lat, lng);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * <p>
     * Normalizes a latitude value to a range of [-90 .. 90] capping it to the bounds, if necessary.
     * </p>
     *
     * @param lat The latitude value to normalize.
     * @return A latitude in the range of [-90 ... 90].
     */
    public static double normalizeLatitude(double lat) {
        if (lat > 90) {
            return 90;
        }
        if (lat < -90) {
            return -90;
        }
        return lat;
    }

    /**
     * <p>
     * Normalizes a longitude value to a range of [-180 .. 180] by wrapping it, if necessary.
     * </p>
     *
     * @param lng The longitude value to normalize.
     * @return A latitude in the range of [-180 ... 180].
     */
    public static double normalizeLongitude(double lng) {
        double result = lng;
        while (result < -180) {
            result += 360;
        }
        while (result > 180) {
            result -= 360;
        }
        return result;
    }

    /**
     * <p>
     * For each pair in the given Collection of {@link GeoCoordinate}s determine the distance, and return the highest
     * distance.
     * </p>
     *
     * @param locations {@link Collection} of {@link GeoCoordinate}s, not <code>null</code>.
     * @return The maximum distance between any pair in the given {@link Collection}, or zero in case the collection was
     * empty.
     * @see #largestDistanceBelow(double, Collection) is faster, if you just care about a maximum value.
     */
    public static double getLargestDistance(Collection<? extends GeoCoordinate> coordinates) {
        Validate.notNull(coordinates, "coordinates must not be null");
        if (coordinates.contains(null) && coordinates.size() > 1) { // multiple null coordinates?
            return EARTH_MAX_DISTANCE_KM;
        }
        double largestDistance = 0;
        List<GeoCoordinate> temp = new ArrayList<>(new HashSet<>(coordinates));
        for (int i = 0; i < temp.size(); i++) {
            GeoCoordinate c1 = temp.get(i);
            for (int j = i + 1; j < temp.size(); j++) {
                GeoCoordinate c2 = temp.get(j);
                largestDistance = Math.max(largestDistance, c1.distance(c2));
            }
        }
        return largestDistance;
    }

    /** For mapping Geohash from decimal (index) to Base 32 representation. */
    private static final String BASE_32_ALPHABET = "0123456789bcdefghjkmnpqrstuvwxyz";

    /**
     * Converts the given coordinate to a
     * <a href="https://en.wikipedia.org/wiki/Geohash">Geohash</a>.
     *
     * <p>
     * A geohash is a single string of letters and digits of an arbitrary length –
     * the longer, the more precise. A geohash guarantees, that two hashes with a
     * shared prefix are close together (however the reverse is not guaranteed).
     * This still makes geohashes useful e.g. for indexation, etc.
     *
     * <p>
     * A hash can be shortened at the end, which means losing precision. For the
     * precision depending on the number of characters, see <a href=
     * "https://en.wikipedia.org/wiki/Geohash#Digits_and_precision_in_km">here</a>.
     *
     * <p>
     * For a coordinate of <code>42.605, -5.603</code>, the geohash of length 5 is
     * <code>ezs42</code>.
     *
     * @param coordinate The coordinate for which to calculate the hash.
     * @param length     The length of the hash in characters (at least one).
     * @return The hash.
     */
    public static String getGeohash(GeoCoordinate coordinate, int length) {
        Objects.requireNonNull(coordinate, "coordinate must not be null");
        if (length < 1) {
            throw new IllegalArgumentException("length must be greater zero");
        }

        double latMin = -90;
        double latMid = 0;
        double latMax = 90;
        double lngMin = -180;
        double lngMid = 0;
        double lngMax = 180;
        BigInteger binary = BigInteger.ZERO;
        for (int idx = 0; idx < 5 * length; idx++) {
            binary = binary.shiftLeft(1);
            if (idx % 2 == 0) {
                if (coordinate.getLongitude() < lngMid) {
                    lngMax = lngMid;
                } else {
                    lngMin = lngMid;
                    binary = binary.add(BigInteger.ONE);
                }
                lngMid = (lngMin + lngMax) / 2;
            } else {
                if (coordinate.getLatitude() < latMid) {
                    latMax = latMid;
                } else {
                    latMin = latMid;
                    binary = binary.add(BigInteger.ONE);
                }
                latMid = (latMin + latMax) / 2;
            }
        }

        StringBuilder hash = new StringBuilder();
        for (int idx = 5 * (length - 1); idx >= 0; idx -= 5) {
            int bits = binary.shiftRight(idx).and(BigInteger.valueOf(0b11111)).intValue();
            hash.append(BASE_32_ALPHABET.charAt(bits));
        }
        return hash.toString();
    }

    /**
     * Parse the given geohash into a latitude and longitude coordinates pair.
     *
     * <p>
     * For more details see {@link #getGeohash(GeoCoordinate, int)}.
     *
     * @param geohash The geohash.
     * @return The parsed coordinate.
     * @throws IllegalArgumentException If the hash contains unexpected characters.
     */
    public static GeoCoordinate parseGeohash(String geohash) {
        Objects.requireNonNull(geohash, "geohash must not be null");
        if (geohash.length() < 1) {
            throw new IllegalArgumentException("hash must be non-empty");
        }

        // (1) decode base 32ghs to binary
        BigInteger result = BigInteger.ZERO;
        for (char ch : geohash.toCharArray()) {
            int decimal = BASE_32_ALPHABET.indexOf(ch);
            if (decimal == -1) {
                throw new IllegalArgumentException("Invalid character: " + ch);
            }
            result = result.shiftLeft(5).add(BigInteger.valueOf(decimal));
        }

        // (2) split odd/even bits to longitude/latitude
        int mod = geohash.length() % 2; // different treatment of odd/even length hashes
        double latMin = -90;
        double latMid = 0;
        double latMax = 90;
        double latMean = 0;
        double lngMin = -180;
        double lngMid = 0;
        double lngMax = 180;
        double lngMean = 0;
        for (int idx = geohash.length() * 5 - 1; idx >= 0; idx--) {
            int currentBit = result.shiftRight(idx).and(BigInteger.ONE).intValue();
            if (idx % 2 == mod) {
                if (currentBit == 0) {
                    latMean = (latMin + latMid) / 2;
                    latMax = latMid;
                } else {
                    latMean = (latMid + latMax) / 2;
                    latMin = latMid;
                }
                latMid = (latMin + latMax) / 2;
            } else {
                if (currentBit == 0) {
                    lngMean = (lngMin + lngMid) / 2;
                    lngMax = lngMid;
                } else {
                    lngMean = (lngMid + lngMax) / 2;
                    lngMin = lngMid;
                }
                lngMid = (lngMin + lngMax) / 2;
            }
        }
        return GeoCoordinate.from(latMean, lngMean);
    }

    private GeoUtils() {
        // no instances.
    }

}
