package ws.palladian.extraction.location.scope;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.Location;

public final class FrequencyScopeDetector implements ScopeDetector {

    private static final String NAME = "Frequency";

    @Override
    public Location getScope(Collection<? extends Location> locations) {
        Validate.notNull(locations, "locations must not be null");
        if (locations.isEmpty()) {
            return null;
        }
        double maxCount = 0;
        Location selectedLocation = null;

        for (Location location : new HashSet<Location>(locations)) {
            if (location.getCoordinate() == null) {
                continue;
            }
            int count = Collections.frequency(locations, location);
            if (count >= maxCount || selectedLocation == null) {
                maxCount = count;
                selectedLocation = location;
            }
        }

        return selectedLocation;
    }

    @Override
    public String toString() {
        return NAME;
    }

}
