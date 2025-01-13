package ws.palladian.extraction.location.scope;

import org.apache.commons.lang3.Validate;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.LocationFilters;
import ws.palladian.helper.collection.CollectionHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public final class FrequencyScopeDetector extends AbstractRankingScopeDetector {

    private static final String NAME = "Frequency";

    public FrequencyScopeDetector(LocationExtractor extractor) {
        super(extractor);
    }

    @Override
    public Location getScope(Collection<LocationAnnotation> annotations) {
        Validate.notNull(annotations, "locations must not be null");
        if (annotations.isEmpty()) {
            return null;
        }
        List<Location> locations = CollectionHelper.convertList(annotations, LocationAnnotation::getLocation);
        CollectionHelper.remove(locations, LocationFilters.coordinate());
        double maxCount = 0;
        Location selectedLocation = null;

        for (LocationAnnotation annotation : new HashSet<LocationAnnotation>(annotations)) {
            int count = Collections.frequency(locations, annotation.getLocation());
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
