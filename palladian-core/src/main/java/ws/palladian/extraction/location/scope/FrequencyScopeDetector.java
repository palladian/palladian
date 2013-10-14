package ws.palladian.extraction.location.scope;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;

public final class FrequencyScopeDetector implements ScopeDetector {

    private static final String NAME = "Frequency";

    @Override
    public Location getScope(Collection<LocationAnnotation> annotations) {
        Validate.notNull(annotations, "locations must not be null");
        if (annotations.isEmpty()) {
            return null;
        }
        double maxCount = 0;
        Location selectedLocation = null;

        for (LocationAnnotation annotation : new HashSet<LocationAnnotation>(annotations)) {
            if (annotation.getLocation().getCoordinate() == null) {
                continue;
            }
            int count = Collections.frequency(annotations, annotation);
            if (count >= maxCount || selectedLocation == null) {
                maxCount = count;
                selectedLocation = annotation.getLocation();
            }
        }

        return selectedLocation;
    }

    @Override
    public String toString() {
        return NAME;
    }

}
