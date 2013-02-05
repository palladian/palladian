package ws.palladian.extraction.location;

import java.util.List;
import java.util.Map;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Cache decorator, useful for Web-based {@link LocationSource}s.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class CachingLocationSource implements LocationSource {

    private final LocationSource locationSource;
    private final Map<String, List<Location>> locationNameCache;
    private final Map<Integer, Location> locationIdCache;
    private final Map<Integer, List<Location>> locationHierachyCache;

    private static int cacheHits = 0;
    private static int cacheFails = 0;

    /**
     * @param locationSource
     */
    public CachingLocationSource(LocationSource locationSource) {
        this.locationSource = locationSource;
        locationNameCache = CollectionHelper.newHashMap();
        locationIdCache = CollectionHelper.newHashMap();
        locationHierachyCache = CollectionHelper.newHashMap();
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        List<Location> locations = locationNameCache.get(locationName);
        if (locations == null) {
            locations = locationSource.retrieveLocations(locationName);
            locationNameCache.put(locationName, locations);
            cacheFails++;
        } else {
            cacheHits++;
        }
        return locations;
    }

    @Override
    public Location retrieveLocation(int locationId) {
        Location location = locationIdCache.get(locationId);
        if (location == null) {
            location = locationSource.retrieveLocation(locationId);
            locationIdCache.put(locationId, location);
            cacheFails++;
        } else {
            cacheHits++;
        }
        return location;
    }

    @Override
    public List<Location> getHierarchy(Location location) {
        List<Location> locations = locationHierachyCache.get(location.getId());
        if (locations == null) {
            locations = locationSource.getHierarchy(location);
            locationHierachyCache.put(location.getId(), locations);
            cacheFails++;
        } else {
            cacheHits++;
        }
        return locations;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CachingLocationSource (");
        stringBuilder.append("Hits=").append(cacheHits);
        stringBuilder.append(", Fails=").append(cacheFails).append(")");
        return stringBuilder.toString();
    }

}
