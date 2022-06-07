package ws.palladian.extraction.location.scope;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;

public final class MidpointScopeDetector extends AbstractRankingScopeDetector {

    private static final String NAME = "Midpoint";

    /** Subsequently try the following groups of location types. */
    private static final List<Set<LocationType>> GROUP_PRIORITY = Arrays.asList(
            EnumSet.of(LocationType.CITY, LocationType.POI), // prefer these -- very specific
            EnumSet.of(LocationType.UNIT, LocationType.REGION, LocationType.LANDMARK), // then try these
            EnumSet.of(LocationType.COUNTRY), // then this
            EnumSet.of(LocationType.CONTINENT) // least specific -- last resort
    );

    public MidpointScopeDetector(LocationExtractor extractor) {
        super(extractor);
    }

    @Override
    public Location getScope(Collection<LocationAnnotation> annotations) {
        Objects.requireNonNull(annotations, "locations must not be null");
        for (Set<LocationType> group : GROUP_PRIORITY) {
            List<Location> locations = annotations.stream() //
                    .map(LocationAnnotation::getLocation) //
                    .filter(l -> l.getCoords().isPresent()) // only keep with coordinates
                    .filter(l -> group.contains(l.getType())) //
                    .collect(Collectors.toList());
            if (locations.isEmpty()) {
                continue;
            }
            List<GeoCoordinate> coordinates = locations.stream().map(Location::getCoordinate)
                    .collect(Collectors.toList());
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
        return null;
    }

    @Override
    public String toString() {
        return NAME;
    }

}
