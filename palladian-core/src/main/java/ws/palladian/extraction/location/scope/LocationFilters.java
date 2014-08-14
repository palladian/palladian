package ws.palladian.extraction.location.scope;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.Location;
import ws.palladian.helper.functional.Filter;

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

}
