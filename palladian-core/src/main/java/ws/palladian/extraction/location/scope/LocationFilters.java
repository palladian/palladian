package ws.palladian.extraction.location.scope;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.geo.GeoCoordinate;

/**
 * Different {@link Filter}s for {@link Location}s.
 * 
 * @author pk
 */
public final class LocationFilters {
    
    /** {@link Filter} for removing {@link Location}s without coordinates. */
    private static final Filter<Location> COORDINATE_FILTER = new Filter<Location>() {
        @Override
        public boolean accept(Location location) {
            return location.getCoordinate() != null && location.getCoordinate() != GeoCoordinate.NULL;
        }
    };

    private LocationFilters() {
        // no instance
    }

    public static Filter<Location> childOf(final Location location) {
        Validate.notNull(location, "location must not be null");
        return new Filter<Location>() {
            @Override
            public boolean accept(Location item) {
                return item.childOf(location);
            }
        };
    }

    public static Filter<Location> descendantOf(final Location location) {
        Validate.notNull(location, "location must not be null");
        return new Filter<Location>() {
            @Override
            public boolean accept(Location item) {
                return item.descendantOf(location);
            }
        };
    }

    public static Filter<Location> ancestorOf(final Location location) {
        Validate.notNull(location, "location must not be null");
        return new Filter<Location>() {
            @Override
            public boolean accept(Location item) {
                return location.descendantOf(item);
            }
        };
    }

    public static Filter<Location> siblingOf(final Location location) {
        Validate.notNull(location, "location must not be null");
        return new Filter<Location>() {
            @Override
            public boolean accept(Location item) {
                return item.getAncestorIds().equals(location.getAncestorIds());
            }
        };
    }

    public static Filter<Location> parentOf(final Location location) {
        Validate.notNull(location, "location must not be null");
        return new Filter<Location>() {
            @Override
            public boolean accept(Location item) {
                return location.childOf(item);
            }
        };
    }
    
    /**
     * <p>
     * Create a filter which only accepts locations around a midpoint within a specified radius.
     * 
     * @param center The center coordinate, not <code>null</code>.
     * @param distance The maximum distance in kilometers for a location to be accepted, greater/equal zero.
     * @return A new filter centered around the given coordinate with the specified distance
     */
    public static Filter<Location> radius(GeoCoordinate center, double distance) {
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
    public static Filter<Location> population(final long minPopulation) {
        Validate.isTrue(minPopulation >= 0, "population must be greater/equal zero");
        return new Filter<Location>() {
            @Override
            public boolean accept(Location item) {
                return item.getPopulation() != null && item.getPopulation() >= minPopulation;
            }
        };
    }

    /**
     * <p>
     * Create a filter which only accepts the specified {@link LocationType}s.
     * 
     * @param types The types to accept, not <code>null</code>.
     * @return A new filter which accepts the given types.
     */
    public static Filter<Location> type(LocationType... types) {
        Validate.notNull(types, "types must not be null");
        return new LocationTypeFilter(types);
    }

    /**
     * @return A filter which only accepts locations which have a coordinate (rejecting those locations, where the
     *         coordinate is <code>null</code> or {@link GeoCoordinate#NULL}).
     */
    public static Filter<Location> coordinate() {
        return COORDINATE_FILTER;
    }

    /**
     * <p>
     * Filter {@link Location}s by {@link LocationType}.
     * 
     * @author Philipp Katz
     */
    private static class LocationTypeFilter implements Filter<Location> {

        private final Set<LocationType> types;

        public LocationTypeFilter(LocationType... types) {
            this.types = new HashSet<LocationType>(Arrays.asList(types));
        }

        @Override
        public boolean accept(Location item) {
            return types.contains(item.getType());
        }

    }
    
//    /**
//     * <p>
//     * Filter {@link Location}s by ID.
//     * </p>
//     * 
//     * @author pk
//     */
//    public static class LocationIdFilter implements Filter<Location> {
//        private final int id;
//
//        public LocationIdFilter(int id) {
//            this.id = id;
//        }
//
//        @Override
//        public boolean accept(Location item) {
//            return item.getId() == id;
//        }
//
//    }
    
    /**
     * <p>
     * A {@link Filter} for {@link Location}s which only accepts those locations within a specified radius around a
     * given center (e.g. give me all locations in distance 1 kilometers from point x). The logic is optimized for speed
     * to avoid costly distance calculations and uses a bounding box as blocker first.
     * 
     * @author pk
     */
    private static class LocationRadiusFilter implements Filter<Location> {

        private final GeoCoordinate center;
        private final double distance;
        private final double[] boundingBox;

        LocationRadiusFilter(GeoCoordinate center, double distance) {
            this.boundingBox = center.getBoundingBox(distance);
            this.center = center;
            this.distance = distance;
        }

        @Override
        public boolean accept(Location item) {
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
