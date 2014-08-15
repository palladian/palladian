package ws.palladian.extraction.location;

import static ws.palladian.extraction.location.LocationExtractorUtils.LOCATION_COORDINATE_FUNCTION;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;
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

    @Deprecated
    public int countDescendants(Location location) {
        int count = 0;
        for (Location other : locations) {
            if (other.descendantOf(location)) {
                count++;
            }
        }
        return count;
    }

    @Deprecated
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
    
    public long totalPopulation() {
        long pop = 0;
        for (Location location : locations) {
            if (location.getPopulation()!=null){
                pop+=location.getPopulation();
            }
        }
        return pop;
    }

    public List<GeoCoordinate> getCoordinates() {
        return Collections.unmodifiableList(coordinates);
    }

    public List<Location> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    public List<Location> getLocationsWithCoordinates() {
        List<Location> result = CollectionHelper.newArrayList(locations);
        CollectionHelper.remove(result, LocationFilters.coordinate());
        return Collections.unmodifiableList(result);
    }

    public double largestDistance() {
//        return LocationExtractorUtils.getLargestDistance(coordinates);
        
        LocationStats withoutCoordinate = where(Filters.not(LocationFilters.coordinate()));
        
//        CollectionHelper.print(withoutCoordinate.getLocations());
        
        if (withoutCoordinate.count()> 0 && count() > 1) {
            return GeoUtils.EARTH_MAX_DISTANCE_KM;
        }
        
        LocationStats withCoordinate = where(LocationFilters.coordinate());
        
//        CollectionHelper.print(withCoordinate.getLocations());
        
        Collection<GeoCoordinate> theCoordinates = CollectionHelper.convertSet(withCoordinate.getLocations(), LocationExtractorUtils.LOCATION_COORDINATE_FUNCTION);
        return LocationExtractorUtils.getLargestDistance(theCoordinates);
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
    
    public LocationStats where(Filter<Location> filter) {
        return new LocationStats(CollectionHelper.filterSet(locations, filter));
    }

//    public LocationStats except(Collection<? extends Location> candidates) {
//      Set<Location> otherLocations = new HashSet<Location>(locations);
//      otherLocations.removeAll(candidates);
//      return new LocationStats(otherLocations);
//    }
//    
//    public LocationStats except(Location... locations) {
//        Validate.notNull(locations, "locations must not be null");
//        return except(Arrays.asList(locations));
//    }
    
    public int count(){
        return CollectionHelper.newHashSet(locations).size();
    }

    public double distance(GeoCoordinate coordinate) {
        double minDistance = GeoUtils.EARTH_MAX_DISTANCE_KM;
        for (Location loc : locations){
            if(loc.getCoordinate()!=null){
                minDistance=Math.min(minDistance, loc.getCoordinate().distance(coordinate));
            }
        }
        return minDistance;
    }

    public boolean contains(Location location) {
        return locations.contains(location);
    }

}
