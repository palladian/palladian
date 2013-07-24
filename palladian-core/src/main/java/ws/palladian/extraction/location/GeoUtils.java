package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.math.MathHelper;

/**
 * @author Philipp Katz
 */
public final class GeoUtils {

    private static final String DMS_FORMAT = "%d°%d′%d″";
    private static final String DMS_PREFIX_FORMAT = "%s" + DMS_FORMAT;
    private static final String DMS_SUFFIX_FORMAT = DMS_FORMAT + "%s";

    public static final String DMS = "([-+]?\\d{1,3}(?:\\.\\d{1,10})?)[°d:]" + // degree
            "(?:\\s?(\\d{1,2}(?:\\.\\d{1,10})?))?['′:]?" + // minute
            "(?:\\s?(\\d{1,2}(?:\\.\\d{1,10})?))?(?:\"|″|'')?" + // second
            "(?:\\s?(N|S|W|E|North|South|West|East))?"; // direction

    /** For parsing a single DMS expression. */
    private static final Pattern PATTERN_PARSE_DMS = Pattern.compile(DMS);

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

    /**
     * <p>
     * Convert decimal degrees to a DMS coordinate.
     * </p>
     * 
     * @param decimal The decimal value to convert.
     * @return The DMS string.
     */
    public static final String decimalToDms(double decimal) {
        String sign = decimal < 0 ? "-" : "";
        int[] parts = getParts(decimal);
        return String.format(DMS_PREFIX_FORMAT, sign, parts[0], parts[1], parts[2]);
    }

    private static int[] getParts(double decimal) {
        int[] parts = new int[3];
        double temp = Math.abs(decimal);

        parts[0] = (int)temp;

        double mod = temp % 1;
        temp = mod * 60;
        parts[1] = (int)temp;

        mod = temp % 1;
        temp = mod * 60;
        parts[2] = (int)temp;
        return parts;
    }

    /**
     * <p>
     * Convert {@link GeoCoordinate} to DMS coordinates.
     * </p>
     * 
     * @param c The coordinate to convert.
     * @return A DMS string representing the coordinate.
     */
    public static final String coordinateToDms(GeoCoordinate c) {
        Validate.notNull(c, "c must not be null");

        double lat = c.getLatitude();
        double lng = c.getLongitude();
        int[] latParts = getParts(lat);
        int[] lngParts = getParts(lng);
        String latSuffix = StringUtils.EMPTY;
        if (lat > 0) {
            latSuffix = "N";
        } else if (lat < 0) {
            latSuffix = "S";
        }
        String lngSuffix = StringUtils.EMPTY;
        if (lng > 0) {
            lngSuffix = "E";
        } else if (lng < 0) {
            lngSuffix = "W";
        }
        String latString = String.format(DMS_SUFFIX_FORMAT, latParts[0], latParts[1], latParts[2], latSuffix);
        String lngString = String.format(DMS_SUFFIX_FORMAT, lngParts[0], lngParts[1], lngParts[2], lngSuffix);
        return latString + "," + lngString;
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

    private GeoUtils() {
        // no instances.
    }

}
