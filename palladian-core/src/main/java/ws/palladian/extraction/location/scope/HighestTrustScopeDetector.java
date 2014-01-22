package ws.palladian.extraction.location.scope;

import static ws.palladian.extraction.location.LocationExtractorUtils.ANNOTATION_LOCATION_FUNCTION;
import static ws.palladian.extraction.location.LocationExtractorUtils.COORDINATE_FILTER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.helper.collection.CollectionHelper;

public final class HighestTrustScopeDetector implements ScopeDetector {

    private static final String NAME = "Trust";

    @Override
    public Location getScope(Collection<LocationAnnotation> locations) {
        Validate.notNull(locations, "locations must not be null");
        List<LocationAnnotation> temp = new ArrayList<LocationAnnotation>(locations);
        Collections.sort(temp, new Comparator<LocationAnnotation>() {
            @Override
            public int compare(LocationAnnotation a1, LocationAnnotation a2) {
                int ret = Double.valueOf(a2.getTrust()).compareTo(a1.getTrust());
                if (ret != 0) {
                    return ret;
                }
                Long p1 = a1.getLocation().getPopulation();
                Long p2 = a2.getLocation().getPopulation();
                if (p1 == null) {
                    p1 = 0l;
                }
                if (p2 == null) {
                    p2 = 0l;
                }
                return Long.valueOf(p2).compareTo(p1);
            }
        });
        List<Location> locationList = CollectionHelper.convertList(temp, ANNOTATION_LOCATION_FUNCTION);
        CollectionHelper.removeNulls(locationList);
        CollectionHelper.remove(locationList, COORDINATE_FILTER);
        return CollectionHelper.getFirst(locationList);
    }

    @Override
    public String toString() {
        return NAME;
    }

}
