package ws.palladian.extraction.location.sources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MruMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;

/**
 * <p>
 * Cache decorator, useful for Web- and database-based {@link LocationSource}s.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class CachingLocationSource extends MultiQueryLocationSource {

    /** The default cache size to use in case not specified. */
    public static final int DEFAULT_CACHE_SIZE = 5000;

    private final MruMap<String, Collection<Location>> nameCache;

    private final MruMap<Integer, Location> idCache;

    private final LocationSource wrapped;

    private final int size;

    private int cacheHits = 0;

    private int cacheMisses = 0;

    /**
     * <p>
     * Create a new {@link CachingLocationSource}.
     * </p>
     * 
     * @param wrapped The location source to wrap, not <code>null</code>.
     * @param size The size of the cache, greater zero.
     */
    public CachingLocationSource(LocationSource wrapped, int size) {
        Validate.notNull(wrapped, "wrapped must not be null");
        Validate.isTrue(size > 0, "size must be greater zero");
        this.wrapped = wrapped;
        this.nameCache = new MruMap<String, Collection<Location>>(size);
        this.idCache = new MruMap<Integer, Location>(size);
        this.size = size;
    }

    /**
     * <p>
     * Create a new {@link CachingLocationSource} with a cache size of {@value #DEFAULT_CACHE_SIZE}.
     * </p>
     * 
     * @param wrapped The location source to wrap, not <code>null</code>.
     */
    public CachingLocationSource(LocationSource wrapped) {
        this(wrapped, DEFAULT_CACHE_SIZE);
    }

    @Override
    public MultiMap<String, Location> getLocations(Collection<String> locationNames, Set<Language> languages) {
        MultiMap<String, Location> result = DefaultMultiMap.createWithSet();
        Set<String> needsLookup = CollectionHelper.newHashSet();

        for (String locationName : locationNames) {
            String identifier = createIdentifier(languages, locationName);
            Collection<Location> cachedLocations = nameCache.get(identifier);
            if (cachedLocations != null) {
                result.put(locationName, cachedLocations);
                cacheHits++;
            } else {
                needsLookup.add(locationName);
                cacheMisses++;
            }
        }

        // get the unresolved names from the underlying location source
        if (needsLookup.size() > 0) {
            MultiMap<String, Location> retrievedLocations = wrapped.getLocations(needsLookup, languages);
            for (String locationName : needsLookup) {
                Collection<Location> locations = retrievedLocations.get(locationName);
                String identifier = createIdentifier(languages, locationName);
                nameCache.put(identifier, locations != null ? locations : Collections.<Location> emptySet());
                if (locations != null) {
                    result.put(locationName, locations);
                }
            }
        }

        return result;
    }

    /**
     * Create an identifier for the hash key (locationName#GERMAN#ENGLISH).
     * 
     * @param languages The languages in the query.
     * @param locationName The searched location name.
     * @return An identifier combining name and languages.
     */
    private static String createIdentifier(Set<Language> languages, String locationName) {
        return locationName + "#" + StringUtils.join(languages, "#");
    }

    @Override
    public List<Location> getLocations(List<Integer> locationIds) {
        Map<Integer, Location> tempResult = CollectionHelper.newHashMap();
        Set<Integer> needsLookup = CollectionHelper.newHashSet();

        for (Integer locationId : locationIds) {
            Location cachedLocation = idCache.get(locationId);
            if (cachedLocation != null) {
                tempResult.put(locationId, cachedLocation);
                cacheHits++;
            } else {
                needsLookup.add(locationId);
                cacheMisses++;
            }
        }

        // get the unresolved IDs from the underlying location source
        if (needsLookup.size() > 0) {
            List<Location> retrievedLocations = wrapped.getLocations(new ArrayList<Integer>(needsLookup));
            for (Location location : retrievedLocations) {
                tempResult.put(location.getId(), location);
            }
        }

        List<Location> result = CollectionHelper.newArrayList();
        for (Integer locationId : locationIds) {
            result.add(tempResult.get(locationId));
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(CachingLocationSource.class.getSimpleName());
        stringBuilder.append(" (");
        stringBuilder.append(wrapped.getClass().getSimpleName());
        stringBuilder.append(", MaxCacheSize=").append(size);
        stringBuilder.append(", Hits=").append(cacheHits);
        stringBuilder.append(", Misses=").append(cacheMisses);
        stringBuilder.append(", NameCacheSize=").append(nameCache.size());
        stringBuilder.append(", IdCacheSize=").append(idCache.size());
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

}
