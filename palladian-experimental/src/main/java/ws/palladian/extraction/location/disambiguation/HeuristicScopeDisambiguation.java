package ws.palladian.extraction.location.disambiguation;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.scope.ScopeDetector;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.geo.GeoCoordinate;

/**
 * @author Philipp Katz
 */
public class HeuristicScopeDisambiguation extends HeuristicDisambiguation {
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HeuristicScopeDisambiguation.class);

    private final ScopeDetector scopeDetector;

    public HeuristicScopeDisambiguation(ScopeDetector scopeDetector) {
        this.scopeDetector = scopeDetector;
    }

    @Override
    protected Set<Location> getAnchors(String text, MultiMap<? extends Annotation, Location> locations) {
        Set<Location> anchors = super.getAnchors(text, locations);

        List<? extends GeoCoordinate> scopes = scopeDetector.getScopes(text);
        // List<? extends GeoCoordinate> scopes = Collections.singletonList(scopeDetector.getScope(text));
        for (Location location : locations.allValues()) {
            for (GeoCoordinate scope : scopes) {
                if (location.getCoordinate().distance(scope) < ANCHOR_DISTANCE_THRESHOLD) {
                    if (anchors.add(location)) {
                        LOGGER.debug("Added to anchors via scope: {}", location);
                        break;
                    }
                }
            }
        }

        return anchors;
    }

    @Override
    public String toString() {
        return "HeuristicDisambiguationWithScope";
    }
}
