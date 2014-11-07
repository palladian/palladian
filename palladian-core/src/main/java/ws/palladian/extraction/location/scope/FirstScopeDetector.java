package ws.palladian.extraction.location.scope;

import java.util.Collection;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;

public final class FirstScopeDetector implements ScopeDetector {

    private static final String NAME = "First";

    @Override
    public Location getScope(Collection<LocationAnnotation> locations) {
        Validate.notNull(locations, "locations must not be null");
        for (LocationAnnotation annotation : locations) {
            Location location = annotation.getLocation();
            if (location.getCoordinate() != null) {
                return location;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return NAME;
    }

}
