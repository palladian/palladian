package ws.palladian.extraction.location.scope;

import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;

public class GridCell {

    final int xId;
    final int yId;
    final double lat1;
    final double lat2;
    final double lng1;
    final double lng2;
    final double gridSize;

    /** Instantiated by the {@link GridCreator}. */
    GridCell(int xId, int yId, double lat1, double lat2, double lng1, double lng2, double gridSize) {
        this.xId = xId;
        this.yId = yId;
        this.lat1 = lat1;
        this.lat2 = lat2;
        this.lng1 = lng1;
        this.lng2 = lng2;
        this.gridSize = gridSize;
    }

    /**
     * @return The cell identifier as string (e.g. <code>(12|23)</code>).
     */
    public String getIdentifier() {
        // XXX slow; use StringBuilder
        // return String.format("(%s|%s)", xId, yId);
        return new StringBuilder().append('(').append(xId).append('|').append(yId).append(')').toString();
    }

    /**
     * @return The northeastern coordinate of this cell.
     */
    public GeoCoordinate getNE() {
        return new ImmutableGeoCoordinate(lat2, lng2);
    }

    /**
     * @return The southeastern coordinate of this cell.
     */
    public GeoCoordinate getSE() {
        return new ImmutableGeoCoordinate(lat1, lng2);
    }

    /**
     * @return The southwestern coordinate of this cell.
     */
    public GeoCoordinate getSW() {
        return new ImmutableGeoCoordinate(lat1, lng1);
    }

    /**
     * @return The northwestern coordinate of this cell.
     */
    public GeoCoordinate getNW() {
        return new ImmutableGeoCoordinate(lat2, lng1);
    }

    /**
     * @return The center coordinate of this cell.
     */
    public GeoCoordinate getCenter() {
        double lat = GeoUtils.normalizeLatitude(lat1 + 0.5 * gridSize);
        double lng = GeoUtils.normalizeLongitude(lng1 + 0.5 * gridSize);
        return new ImmutableGeoCoordinate(lat, lng);
    }

//    public boolean contains(GridCell other) {
//        throw new UnsupportedOperationException("Implement me");
//    }

    public boolean intersects(GridCell other) {
        return lat1 < other.lat1 && other.lat1 < lat2 || //
                lat1 < other.lat2 && other.lat2 < lat2 || //
                lng1 < other.lng1 && other.lng1 < lng1 || //
                lng2 < other.lng2 && other.lng2 < lng2;
    }

    @Override
    public String toString() {
//        StringBuilder builder = new StringBuilder();
//        builder.append("GridCell [xId=");
//        builder.append(xId);
//        builder.append(", yId=");
//        builder.append(yId);
//        builder.append(", lat1=");
//        builder.append(lat1);
//        builder.append(", lat2=");
//        builder.append(lat2);
//        builder.append(", lng1=");
//        builder.append(lng1);
//        builder.append(", lng2=");
//        builder.append(lng2);
//        builder.append("]");
//        return builder.toString();
        return getIdentifier();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + xId;
        result = prime * result + yId;
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
        GridCell other = (GridCell)obj;
        if (xId != other.xId)
            return false;
        if (yId != other.yId)
            return false;
        return true;
    }

}
