package ws.palladian.extraction.location.scope;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.helper.geo.GeoCoordinate;

import java.util.List;

public abstract class AbstractRankingScopeDetector implements RankingScopeDetector {

    private final LocationExtractor extractor;

    protected AbstractRankingScopeDetector(LocationExtractor extractor) {
        this.extractor = extractor;
    }

    @Override
    public final GeoCoordinate getScope(String text) {
        List<LocationAnnotation> locationAnnotations = extractor.getAnnotations(text);
        Location scope = getScope(locationAnnotations);
        return scope != null ? scope.getCoordinate() : null;
    }

}
