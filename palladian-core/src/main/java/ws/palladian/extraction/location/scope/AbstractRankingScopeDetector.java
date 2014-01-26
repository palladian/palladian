package ws.palladian.extraction.location.scope;

import java.util.List;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;

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
