package ws.palladian.extraction.location.scope;

import static ws.palladian.extraction.location.LocationExtractorUtils.LOCATION_COORDINATE_FUNCTION;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.GeoUtils;
import ws.palladian.extraction.location.Location;
import ws.palladian.helper.collection.CollectionHelper;

public final class MidpointScopeDetector implements ScopeDetector {

    private static final String NAME = "Midpoint";

    @Override
    public Location getScope(Collection<? extends Location> locations) {
        Validate.notNull(locations, "locations must not be null");
        if (locations.isEmpty()) {
            return null;
        }
        List<GeoCoordinate> coordinates = CollectionHelper.convertList(locations, LOCATION_COORDINATE_FUNCTION);
        GeoCoordinate midpoint = GeoUtils.getMidpoint(coordinates);
        double smallestDistance = Double.MAX_VALUE;
        Location selectedCoordinate = null;
        for (Location location : locations) {
            if (location.getLatitude() == null || location.getLongitude() == null) {
                continue;
            }
            double distance = midpoint.distance(location.getCoordinate());
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
