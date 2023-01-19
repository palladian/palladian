package ws.palladian.helper.geo;

/**
 * <p>
 * A coordinate in the Universal Transverse Mercator (UTM) projected coordinate system.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="http://en.wikipedia.org/wiki/Universal_Transverse_Mercator_coordinate_system">Wikipedia: Universal
 * Transverse Mercator coordinate system</a>
 */
public class UtmCoordinate {

    private final double easting;
    private final double northing;
    private final int zone;
    private final char band;

    UtmCoordinate(double easting, double northing, int zone, char band) {
        this.easting = easting;
        this.northing = northing;
        this.zone = zone;
        this.band = band;
    }

    /**
     * @return The easting of this coordinate (x-coordinate) in meters.
     */
    public double getEasting() {
        return easting;
    }

    /**
     * @return The northing of this coordinate (y-coordinate) in meters.
     */
    public double getNorthing() {
        return northing;
    }

    /**
     * @return The zone of this coordinate [1,60].
     */
    public int getZone() {
        return zone;
    }

    /**
     * @return The band of this coordinate.
     */
    public char getBand() {
        return band;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s %s", zone, band, Math.round(easting), Math.round(northing));
    }

    /**
     * @return The UTM grid zone (e.g. 33U for Dresden, Germany).
     */
    public String getGridZone() {
        return String.format("%s%s", zone, band);
    }

}
