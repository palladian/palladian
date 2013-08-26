package ws.palladian.extraction.location.sources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;

/**
 * <p>
 * {@link LocationSource} for combining multiple sources. Only retrieval by name is allowed, because ID retrieval makes
 * no sense over multiple sources.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class CombinedLocationSource extends MultiQueryLocationSource {

    public static enum QueryMode {
        /** Query all locations sources in the given order, in case of a match, remaining sources are not checked. */
        FIRST,
        /** Query all location sources and combine their results. */
        COMBINE
    }

    private final List<LocationSource> locationSources;
    private final QueryMode queryMode;

    public CombinedLocationSource(QueryMode queryMode, Collection<LocationSource> locationSources) {
        this.queryMode = queryMode;
        this.locationSources = new ArrayList<LocationSource>(locationSources);
    }

    public CombinedLocationSource(QueryMode queryMode, LocationSource... locationSources) {
        this(queryMode, Arrays.asList(locationSources));
    }

    @Override
    public MultiMap<String, Location> getLocations(Collection<String> locationNames, Set<Language> languages) {
        MultiMap<String, Location> result = DefaultMultiMap.createWithSet();
        for (LocationSource locationSource : locationSources) {
            MultiMap<String, Location> current = locationSource.getLocations(locationNames, languages);
            if (current.size() > 0) {
                result.addAll(current);
                if (queryMode == QueryMode.FIRST) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public List<Location> getLocations(List<Integer> locationIds) {
        throw new UnsupportedOperationException("Getting by IDs is not supported by " + getClass().getName());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CombinedLocationSource [locationSources=");
        builder.append(locationSources);
        builder.append(", mode=");
        builder.append(queryMode);
        builder.append("]");
        return builder.toString();
    }

}
