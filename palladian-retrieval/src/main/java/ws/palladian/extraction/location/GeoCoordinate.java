package ws.palladian.extraction.location;

// FIXME this is a class for the API module
public interface GeoCoordinate {

    /**
     * @return The geographical latitude of this location, or <code>null</code> if no coordinates exist.
     */
    Double getLatitude();

    /**
     * @return The geographical longitude of this location, or <code>null</code> if no coordinates exist.
     */
    Double getLongitude();

}
