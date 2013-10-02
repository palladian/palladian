package ws.palladian.extraction.location.scope;

import java.util.Collection;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.GeoUtils;
import ws.palladian.extraction.location.Location;

public final class MidpointScopeDetector implements ScopeDetector {

    private static final String NAME = "Midpoint";

    @Override
    public Location getScope(Collection<? extends Location> locations) {
        Validate.notNull(locations, "locations must not be null");
        if (locations.isEmpty()) {
            return null;
        }
        GeoCoordinate midpoint = GeoUtils.getMidpoint(locations);
        double smallestDistance = Double.MAX_VALUE;
        Location selectedCoordinate = null;
        for (Location location : locations) {
            if (location.getLatitude() == null || location.getLongitude() == null) {
                continue;
            }
            double distance = GeoUtils.getDistance(midpoint, location);
            if (distance < smallestDistance) {
                smallestDistance = distance;
                selectedCoordinate = location;
            }
        }
        return selectedCoordinate;
    }

    @Override
    public String toString() {
        return NAME;
    }

}
