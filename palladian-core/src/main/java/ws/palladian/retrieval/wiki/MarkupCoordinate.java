package ws.palladian.retrieval.wiki;

import ws.palladian.helper.geo.AbstractGeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;

/**
 * <p>
 * Utility class representing a coordinate extracted from Wikipedia coordinate markup. It provides some additional
 * properties such as 'display' type, population in some case, and a coordinate type.
 * </p>
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Wikipedia:WikiProject_Geographical_coordinates">WikiProject Geographical
 *      coordinates</a>
 * @author pk
 */
public final class MarkupCoordinate extends AbstractGeoCoordinate {

    private final double lat;
    private final double lng;
    private final Long population;
    private final String display;
    private final String name;
    private final String type;
    private final String region;

    public MarkupCoordinate(double lat, double lng, String display, String type) {
        this(lat, lng, null, null, display, type, null);
    }

    public MarkupCoordinate(double lat, double lng, String name, Long population, String display, String type,
            String region) {
        GeoUtils.validateCoordinateRange(lat, lng);
        this.lat = lat;
        this.lng = lng;
        this.name = name;
        this.population = population;
        this.display = display;
        this.type = type;
        this.region = region;
    }

    @Override
    public double getLatitude() {
        return lat;
    }

    @Override
    public double getLongitude() {
        return lng;
    }

    /**
     * @return The display type of this coordinate on the Wikipedia page, <code>null</code> in case no such information
     *         exists.
     */
    public String getDisplay() {
        return display;
    }

    /**
     * @return The population assigned with this coordinate, or <code>null</code> in case no such information exists.
     */
    public Long getPopulation() {
        return population;
    }

    /**
     * @return The type of this coordinate, see link above for a description of different available types.
     */
    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MarkupCoordinate [lat=");
        builder.append(lat);
        builder.append(", lng=");
        builder.append(lng);
        builder.append(", population=");
        builder.append(population);
        builder.append(", display=");
        builder.append(display);
        builder.append(", name=");
        builder.append(name);
        builder.append(", type=");
        builder.append(type);
        builder.append(", region=");
        builder.append(region);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((display == null) ? 0 : display.hashCode());
        long temp;
        temp = Double.doubleToLongBits(lat);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lng);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((population == null) ? 0 : population.hashCode());
        result = prime * result + ((region == null) ? 0 : region.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        MarkupCoordinate other = (MarkupCoordinate)obj;
        if (display == null) {
            if (other.display != null)
                return false;
        } else if (!display.equals(other.display))
            return false;
        if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
            return false;
        if (Double.doubleToLongBits(lng) != Double.doubleToLongBits(other.lng))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (population == null) {
            if (other.population != null)
                return false;
        } else if (!population.equals(other.population))
            return false;
        if (region == null) {
            if (other.region != null)
                return false;
        } else if (!region.equals(other.region))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

}
