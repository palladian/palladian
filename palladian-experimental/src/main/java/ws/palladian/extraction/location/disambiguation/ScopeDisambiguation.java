package ws.palladian.extraction.location.disambiguation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.scope.ScopeDetector;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.geo.GeoCoordinate;

public class ScopeDisambiguation implements LocationDisambiguation {

    private final ScopeDetector scopeDetector;

    public ScopeDisambiguation(ScopeDetector scopeDetector) {
        this.scopeDetector = Objects.requireNonNull(scopeDetector);
    }

    @Override
    public List<LocationAnnotation> disambiguate(String text, MultiMap<ClassifiedAnnotation, Location> locations) {
        List<LocationAnnotation> result = new ArrayList<>();

        List<? extends GeoCoordinate> scope = scopeDetector.getScopes(text);

        for (ClassifiedAnnotation annotation : locations.keySet()) {
            Collection<Location> candidates = locations.get(annotation);
            Location selected = null;
            double maxScore = Double.MIN_VALUE;
            for (Location location : candidates) {
                for (GeoCoordinate coordinate : scope) {
                    double distance = location.getCoordinate().distance(coordinate);
                    // score by population and distance
                    double score = location.getPopulationOptional().orElse(0l) / Math.pow(distance, 2);
                    if (location.getType() == LocationType.CONTINENT || location.getType() == LocationType.COUNTRY) {
                        score = Double.MAX_VALUE;
                    }
                    if (score > maxScore) {
                        maxScore = score;
                        selected = location;
                    }
                }
            }
            if (selected != null) {
                result.add(new LocationAnnotation(annotation, selected));
            }

        }
        return result;
    }
}
