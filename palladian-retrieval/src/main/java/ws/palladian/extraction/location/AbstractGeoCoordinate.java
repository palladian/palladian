package ws.palladian.extraction.location;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Default implementation for {@link GeoCoordinate} with implemented utility functionality.
 * </p>
 * 
 * @author pk
 */
public abstract class AbstractGeoCoordinate implements GeoCoordinate {

    @Override
    public double distance(GeoCoordinate other) {
        Validate.notNull(other, "other must not be null");

        double lat1 = getLatitude();
        double lng1 = getLongitude();
        double lat2 = other.getLatitude();
        double lng2 = other.getLongitude();
        return 2
                * GeoUtils.EARTH_RADIUS_KM
                * Math.asin(Math.sqrt(Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
                        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.pow(Math.sin(Math.toRadians(lng2 - lng1) / 2), 2)));
    }

    @Override
    public String toDmsString() {
        Double lat = getLatitude();
        Double lng = getLongitude();
        if (lat == null || lng == null) {
            return StringUtils.EMPTY;
        }
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
        String latString = String.format(GeoUtils.DMS_SUFFIX_FORMAT, latParts[0], latParts[1], latParts[2], latSuffix);
        String lngString = String.format(GeoUtils.DMS_SUFFIX_FORMAT, lngParts[0], lngParts[1], lngParts[2], lngSuffix);
        return latString + "," + lngString;
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

    @Override
    public double[] getBoundingBox(double distance) {
        Validate.isTrue(distance >= 0, "distance must be equal/greater zero");

        // http://vinsol.com/blog/2011/08/30/geoproximity-search-with-mysql/
        double lat1 = getLatitude() - distance / 111.04;
        double lat2 = getLatitude() + distance / 111.04;
        double long1 = getLongitude() - distance / Math.abs(Math.cos(Math.toRadians(getLatitude())) * 111.04);
        double long2 = getLongitude() + distance / Math.abs(Math.cos(Math.toRadians(getLatitude())) * 111.04);
        return new double[] {lat1, long1, lat2, long2};
    }

    @Override
    public String toString() {
        return String.format("(%s,%s)", getLatitude(), getLongitude());
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(getLatitude());
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getLongitude());
        result = prime * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractGeoCoordinate other = (AbstractGeoCoordinate)obj;
        if (Double.doubleToLongBits(getLatitude()) != Double.doubleToLongBits(other.getLatitude()))
            return false;
        if (Double.doubleToLongBits(getLongitude()) != Double.doubleToLongBits(other.getLongitude()))
            return false;
        return true;
    }
}
