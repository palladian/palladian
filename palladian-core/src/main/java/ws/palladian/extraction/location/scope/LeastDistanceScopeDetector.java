package ws.palladian.extraction.location.scope;

import static ws.palladian.extraction.location.LocationExtractorUtils.ANNOTATION_LOCATION_FUNCTION;
import static ws.palladian.extraction.location.LocationExtractorUtils.COORDINATE_FILTER;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.helper.collection.CollectionHelper;

public final class LeastDistanceScopeDetector extends AbstractRankingScopeDetector {

    private static final String NAME = "LeastDistance";

    public LeastDistanceScopeDetector(LocationExtractor extractor) {
        super(extractor);
    }

    @Override
    public Location getScope(Collection<LocationAnnotation> annotations) {
        Validate.notNull(annotations, "locations must not be null");
        List<Location> locationList = CollectionHelper.convertList(annotations, ANNOTATION_LOCATION_FUNCTION);
        CollectionHelper.removeNulls(locationList);
        CollectionHelper.remove(locationList, COORDINATE_FILTER);

        double minDistanceSum = Double.MAX_VALUE;
        Location scopeLocation = null;

        for (int i = 0; i < locationList.size(); i++) {
            double currentDistanceSum = 0;
            Location currentLocation = locationList.get(i);
            GeoCoordinate currentCoordinate = currentLocation.getCoordinate();
            for (int j = 0; j < locationList.size(); j++) {
                GeoCoordinate otherCoordinate = locationList.get(j).getCoordinate();
                currentDistanceSum += currentCoordinate.distance(otherCoordinate);
            }
            if (currentDistanceSum < minDistanceSum) {
                minDistanceSum = currentDistanceSum;
                scopeLocation = currentLocation;
            }
        }
        return scopeLocation;
    }

    @Override
    public String toString() {
        return NAME;
    }

}
