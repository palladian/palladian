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
        
        Double lat1 = getLatitude();
        Double lng1 = getLongitude();
        Double lat2 = other.getLatitude();
        Double lng2 = other.getLongitude();
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return Integer.MAX_VALUE;
        }
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

        if (getLatitude() == null || getLongitude() == null) {
            return new double[0];
        }

        // http://vinsol.com/blog/2011/08/30/geoproximity-search-with-mysql/
        double lat1 = getLatitude() - distance / 111.04;
        double lat2 = getLatitude() + distance / 111.04;
        double long1 = getLongitude() - distance / Math.abs(Math.cos(Math.toRadians(getLatitude())) * 111.04);
        double long2 = getLongitude() + distance / Math.abs(Math.cos(Math.toRadians(getLatitude())) * 111.04);
        return new double[] {lat1, long1, lat2, long2};
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GeoCoordinate [");
        builder.append(getLatitude());
        builder.append(",");
        builder.append(getLongitude());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getLatitude() == null) ? 0 : getLatitude().hashCode());
        result = prime * result + ((getLongitude() == null) ? 0 : getLongitude().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractGeoCoordinate other = (AbstractGeoCoordinate)obj;
        if (getLatitude() == null) {
            if (other.getLatitude() != null)
                return false;
        } else if (!getLatitude().equals(other.getLatitude()))
            return false;
        if (getLongitude() == null) {
            if (other.getLongitude() != null)
                return false;
        } else if (!getLongitude().equals(other.getLongitude()))
            return false;
        return true;
    }

}
