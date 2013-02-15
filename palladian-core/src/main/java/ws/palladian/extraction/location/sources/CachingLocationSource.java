package ws.palladian.extraction.location.sources;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;

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

    // XXX if the same location is queried with different languages set, this will not yield currect results, because it
    // is already cached.
    @Override
    public List<Location> retrieveLocations(String locationName, EnumSet<Language> languages) {
        List<Location> locations = locationNameCache.get(locationName);
        if (locations == null) {
            locations = locationSource.retrieveLocations(locationName, languages);
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
    public List<Location> getHierarchy(int locationId) {
        List<Location> locations = locationHierachyCache.get(locationId);
        if (locations == null) {
            locations = locationSource.getHierarchy(locationId);
            locationHierachyCache.put(locationId, locations);
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
