package ws.palladian.extraction.location;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import java.util.function.Predicate;
import ws.palladian.helper.geo.GeoCoordinate;

/**
 * Different {@link Predicate}s for {@link Location}s.
 * 
 * @author Philipp Katz
 */
public final class LocationFilters {
    
    /** {@link Predicate} for removing {@link Location}s without coordinates. */
    private static final Predicate<Location> COORDINATE_FILTER = location -> 
    	location.getCoordinate() != null && location.getCoordinate() != GeoCoordinate.NULL;

    private LocationFilters() {
        // no instance
    }

    public static Predicate<Location> childOf(final Location location) {
        Validate.notNull(location, "location must not be null");
        return item -> item.childOf(location);
    }

    public static Predicate<Location> descendantOf(final Location location) {
        Validate.notNull(location, "location must not be null");
        return item -> item.descendantOf(location);
    }

    public static Predicate<Location> ancestorOf(final Location location) {
        Validate.notNull(location, "location must not be null");
        return item -> location.descendantOf(item);
    }

    public static Predicate<Location> siblingOf(final Location location) {
        Validate.notNull(location, "location must not be null");
        return item -> item.getAncestorIds().equals(location.getAncestorIds());
    }

    public static Predicate<Location> parentOf(final Location location) {
        Validate.notNull(location, "location must not be null");
        return item -> location.childOf(item);
    }
    
    /**
     * <p>
     * Create a filter which only accepts locations around a midpoint within a specified radius.
     * 
     * @param center The center coordinate, not <code>null</code>.
     * @param distance The maximum distance in kilometers for a location to be accepted, greater/equal zero.
     * @return A new filter centered around the given coordinate with the specified distance
     */
    public static Predicate<Location> radius(GeoCoordinate center, double distance) {
        Validate.notNull(center, "center must not be null");
        Validate.isTrue(distance >= 0, "distance must be greater/equal zero");
        return new LocationRadiusFilter(center, distance);
    }

    /**
     * <p>
     * Create a filter which only accepts locations having a minimum specified population count (rejecting such
     * locations, with <code>null</code> population).
     * 
     * @param minPopulation The minimum population count, must be greater zero.
     * @return A filter for the specified minimum population count.
     */
    public static Predicate<Location> population(final long minPopulation) {
        Validate.isTrue(minPopulation >= 0, "population must be greater/equal zero");
        return item -> item.getPopulation() != null && item.getPopulation() >= minPopulation;
    }

    /**
     * <p>
     * Create a filter which only accepts the specified {@link LocationType}s.
     * 
     * @param types The types to accept, not <code>null</code>.
     * @return A new filter which accepts the given types.
     */
    public static Predicate<Location> type(LocationType... types) {
        Validate.notNull(types, "types must not be null");
        return new LocationTypeFilter(types);
    }

    /**
     * @return A filter which only accepts locations which have a coordinate (rejecting those locations, where the
     *         coordinate is <code>null</code> or {@link GeoCoordinate#NULL}).
     */
    public static Predicate<Location> coordinate() {
        return COORDINATE_FILTER;
    }

    /**
     * <p>
     * Filter {@link Location}s by {@link LocationType}.
     * 
     * @author Philipp Katz
     */
    private static class LocationTypeFilter implements Predicate<Location> {

        private final Set<LocationType> types;

        public LocationTypeFilter(LocationType... types) {
            this.types = new HashSet<LocationType>(Arrays.asList(types));
        }

        @Override
        public boolean test(Location item) {
            return types.contains(item.getType());
        }

    }
    
    /**
     * <p>
     * A {@link Predicate} for {@link Location}s which only accepts those locations within a specified radius around a
     * given center (e.g. give me all locations in distance 1 kilometers from point x). The logic is optimized for speed
     * to avoid costly distance calculations and uses a bounding box as blocker first.
     * 
     * @author Philipp Katz
     */
    private static class LocationRadiusFilter implements Predicate<Location> {

        private final GeoCoordinate center;
        private final double distance;
        private final double[] boundingBox;

        LocationRadiusFilter(GeoCoordinate center, double distance) {
            this.boundingBox = center.getBoundingBox(distance);
            this.center = center;
            this.distance = distance;
        }

        @Override
        public boolean test(Location item) {
            GeoCoordinate coordinate = item.getCoordinate();
            if (coordinate == null) {
                return false;
            }
            // use the bounding box as blocker function first, this avoids the more expensive distance calculation, in
            // case the coordinate is outside the box anyways
            double lng = coordinate.getLongitude();
            if (lng < boundingBox[1] || lng > boundingBox[3]) {
                return false;
            }
            double lat = coordinate.getLatitude();
            if (lat < boundingBox[0] || lat > boundingBox[2]) {
                return false;
            }
            // we're inside the bounding box, but are we inside the circle?
            return coordinate.distance(center) < distance;
        }

    }

}
