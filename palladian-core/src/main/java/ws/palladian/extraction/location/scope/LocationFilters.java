package ws.palladian.extraction.location.scope;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.geo.GeoCoordinate;

public final class LocationFilters {

    public static Filter<Location> child(final Location location) {
        Validate.notNull(location, "location must not be null");
        return new Filter<Location>() {
            @Override
            public boolean accept(Location item) {
                return item.childOf(location);
            }
        };
    }

    public static Filter<Location> descendant(final Location location) {
        Validate.notNull(location, "location must not be null");
        return new Filter<Location>() {
            @Override
            public boolean accept(Location item) {
                return item.descendantOf(location);
            }
        };
    }

    public static Filter<Location> ancestor(final Location location) {
        Validate.notNull(location, "location must not be null");
        return new Filter<Location>() {
            @Override
            public boolean accept(Location item) {
                return location.descendantOf(item);
            }
        };
    }

    public static Filter<Location> sibling(final Location location) {
        Validate.notNull(location, "location must not be null");
        return new Filter<Location>() {
            @Override
            public boolean accept(Location item) {
                return item.getAncestorIds().equals(location.getAncestorIds());
            }
        };
    }
    
    public static Filter<Location> parent(final Location location) {
        Validate.notNull(location, "location must not be null");
        return new Filter<Location>() {
            @Override
            public boolean accept(Location item) {
                return location.childOf(item);
            }
        };
    }
    
    public static Filter<Location> inRadius(GeoCoordinate center, double distance) {
        return new LocationRadiusFilter(center, distance);
    }
    
    /**
     * <p>
     * A {@link Filter} for {@link Location}s which only accepts those locations within a specified radius around a
     * given center (e.g. give me all locations in distance 1 kilometers from point x). The logic is optimized for speed
     * to avoid costly distance calculations and uses a bounding box as blocker first.
     * </p>
     * 
     * @author pk
     */
    private static class LocationRadiusFilter implements Filter<Location> {

        private final GeoCoordinate center;
        private final double distance;
        private final double[] boundingBox;

        /**
         * <p>
         * Create a new {@link LocationRadiusFilter} centered around the given coordinate with the specified distance.
         * </p>
         * 
         * @param center The center coordinate, not <code>null</code>.
         * @param distance The maximum distance in kilometers for a location to be accepted, greater/equal zero.
         */
        public LocationRadiusFilter(GeoCoordinate center, double distance) {
            Validate.notNull(center, "center must not be null");
            Validate.isTrue(distance >= 0, "distance must be greater/equal zero");
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

    public static Filter<Location> minPopulation(final long population) {
        return new Filter<Location>() {
            @Override
            public boolean accept(Location item) {
                return item.getPopulation() != null && item.getPopulation() >= population;
            }
        };
    }
    
    /**
     * <p>
     * Filter {@link Location}s by {@link LocationType}.
     * </p>
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

    /**
     * <p>
     * Filter {@link Location}s by ID.
     * </p>
     * 
     * @author pk
     */
    public static class LocationIdFilter implements Filter<Location> {
        private final int id;

        public LocationIdFilter(int id) {
            this.id = id;
        }

        @Override
        public boolean accept(Location item) {
            return item.getId() == id;
        }

    }
    
    public static Filter<Location> type(LocationType... types) {
        return new LocationTypeFilter(types);
    }

}
