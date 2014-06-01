package ws.palladian.extraction.location.scope;

import static ws.palladian.extraction.location.LocationExtractorUtils.LOCATION_COORDINATE_FUNCTION;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationExtractorUtils;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;
import ws.palladian.helper.math.FatStats;
import ws.palladian.helper.math.Stats;

/**
 * Provides various statistics for lists of {@link Location}s.
 * 
 * @author pk
 */
public class LocationStats {

    private final List<Location> locations;

    private final List<GeoCoordinate> coordinates;

    public LocationStats(Collection<? extends Location> locations) {
        this.locations = CollectionHelper.newArrayList(locations);
        this.coordinates = CollectionHelper.convertList(locations, LOCATION_COORDINATE_FUNCTION);
        CollectionHelper.removeNulls(coordinates);
    }

    public int getMaxHierarchyDepth() {
        int maxDepth = 1;
        for (Location location : locations) {
            maxDepth = Math.max(maxDepth, location.getAncestorIds().size());
        }
        return maxDepth;
    }

    public double getMaxDistance(GeoCoordinate coordinate) {
        double maxDistance = Double.MIN_VALUE;
        for (Location other : locations) {
            if (other.getCoordinate() != null) {
                maxDistance = Math.max(maxDistance, other.getCoordinate().distance(coordinate));
            }
        }
        return maxDistance;
    }

    public int countDescendants(Location location) {
        int count = 0;
        for (Location other : locations) {
            if (other.descendantOf(location)) {
                count++;
            }
        }
        return count;
    }

    public int countAncestors(Location location) {
        int count = 0;
        for (Location other : locations) {
            if (location.descendantOf(other)) {
                count++;
            }
        }
        return count;
    }

    public GeoCoordinate getMidpoint() {
        return GeoUtils.getMidpoint(coordinates);
    }

    public GeoCoordinate getCenterOfMinimumDistance() {
        return GeoUtils.getCenterOfMinimumDistance(coordinates);
    }

    public Location getBiggest() {
        return LocationExtractorUtils.getBiggest(locations);
    }

    public long getBiggestPopulation() {
        return LocationExtractorUtils.getHighestPopulation(locations);
    }

    public List<GeoCoordinate> getCoordinates() {
        return Collections.unmodifiableList(coordinates);
    }

    public List<Location> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    public List<Location> getLocationsWithCoordinates() {
        List<Location> result = CollectionHelper.newArrayList(locations);
        CollectionHelper.remove(result, LocationExtractorUtils.COORDINATE_FILTER);
        return Collections.unmodifiableList(result);
    }

    public double getLargestDistance() {
        return LocationExtractorUtils.getLargestDistance(coordinates);
    }

    public int count(Location location) {
        return Collections.frequency(locations, location);
    }

    public double getMaxMidpointDistance() {
        return getMaxDistance(getMidpoint());
    }

    public double getMaxCenterDistance() {
        return getMaxDistance(getCenterOfMinimumDistance());
    }
    
    public Stats getDistanceStats(Location location) {
        Set<Location> others = new HashSet<Location>(getLocationsWithCoordinates());
        others.remove(location);
        Stats distances = new FatStats();
        for (Location other : others) {
            distances.add(location.getCoordinate().distance(other.getCoordinate()));
        }
        return distances;
    }

}
