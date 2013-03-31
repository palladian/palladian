package ws.palladian.extraction.location.sources;

import java.util.Collection;
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
    private final Map<String, Collection<Location>> locationNameCache;
    private final Map<Integer, Location> locationIdCache;

    private int cacheHits = 0;
    private int cacheMisses = 0;

    /**
     * <p>
     * Create a new {@link CachingLocationSource} wrapping the given {@link LocationSource}.
     * </p>
     * 
     * @param locationSource The {@link LocationSource} for which to provide caching, not <code>null</code>.
     */
    public CachingLocationSource(LocationSource locationSource) {
        this.locationSource = locationSource;
        locationNameCache = CollectionHelper.newHashMap();
        locationIdCache = CollectionHelper.newHashMap();
    }

    @Override
    public Collection<Location> getLocations(String locationName) {
        Collection<Location> locations = locationNameCache.get(locationName);
        if (locations == null) {
            locations = locationSource.getLocations(locationName);
            locationNameCache.put(locationName, locations);
            cacheMisses++;
        } else {
            cacheHits++;
        }
        return locations;
    }

    // XXX if the same location is queried with different languages set, this will not yield currect results, because it
    // is already cached.
    @Override
    public Collection<Location> getLocations(String locationName, EnumSet<Language> languages) {
        Collection<Location> locations = locationNameCache.get(locationName);
        if (locations == null) {
            locations = locationSource.getLocations(locationName, languages);
            locationNameCache.put(locationName, locations);
            cacheMisses++;
        } else {
            cacheHits++;
        }
        return locations;
    }

    @Override
    public Location getLocation(int locationId) {
        Location location = locationIdCache.get(locationId);
        if (location == null) {
            location = locationSource.getLocation(locationId);
            locationIdCache.put(locationId, location);
            cacheMisses++;
        } else {
            cacheHits++;
        }
        return location;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CachingLocationSource (");
        stringBuilder.append("Hits=").append(cacheHits);
        stringBuilder.append(", Misses=").append(cacheMisses).append(")");
        return stringBuilder.toString();
    }

    @Override
    public List<Location> getLocations(List<Integer> locationIds) {
        List<Location> locations = CollectionHelper.newArrayList();
        for (Integer locationId : locationIds) {
            Location location = getLocation(locationId);
            if (location != null) {
                locations.add(location);
            }
        }
        return locations;
    }

}
