package ws.palladian.extraction.location.scope;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;

public final class MidpointScopeDetector extends AbstractRankingScopeDetector {

    private static final String NAME = "Midpoint";

    public MidpointScopeDetector(LocationExtractor extractor) {
        super(extractor);
    }

    @Override
    public Location getScope(Collection<LocationAnnotation> annotations) {
        Objects.requireNonNull(annotations, "locations must not be null");
        List<Location> locations = annotations.stream() //
                .map(LocationAnnotation::getLocation) //
                .filter(l -> l.getCoords().isPresent()) // only keep with coordinates
                .collect(Collectors.toList());
        if (locations.isEmpty()) {
            return null;
        }
        List<GeoCoordinate> coordinates = locations.stream().map(Location::getCoordinate).collect(Collectors.toList());
        GeoCoordinate midpoint = GeoUtils.getMidpoint(coordinates);
        double smallestDistance = Double.MAX_VALUE;
        Location selectedCoordinate = null;
        for (Location location : locations) {
            double distance = midpoint.distance(location.getCoordinate());
            if (distance < smallestDistance) {
                smallestDistance = distance;
                selectedCoordinate = location;
            }
        }
        return selectedCoordinate;
    }

    @Override
    public String toString() {
        return NAME;
    }

}
