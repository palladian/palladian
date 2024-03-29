package ws.palladian.extraction.location.sources;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;

import java.util.*;

/**
 * <p>
 * Common base class for {@link LocationSource}s which do not support getting multiple entities in one go (like
 * {@link LocationSource#getLocations(List)}). This implementation simply splits such queries to their single-argument
 * counterpart and combines the results.
 * </p>
 *
 * @author Philipp Katz
 */
public abstract class SingleQueryLocationSource implements LocationSource {

    @Override
    public final MultiMap<String, Location> getLocations(Collection<String> locationNames, Set<Language> languages) {
        MultiMap<String, Location> locationMap = DefaultMultiMap.createWithSet();
        for (String locationName : locationNames) {
            locationMap.put(locationName, getLocations(locationName, languages));
        }
        return locationMap;
    }

    @Override
    public MultiMap<String, Location> getLocations(Collection<String> locationNames, Set<Language> languages, GeoCoordinate coordinate, double distance) {
        throw new UnsupportedOperationException("Not supported by " + getClass().getName() + ".");
    }

    @Override
    public final List<Location> getLocations(List<Integer> locationIds) {
        List<Location> locations = new ArrayList<>();
        for (Integer locationId : locationIds) {
            Location location = getLocation(locationId);
            if (location != null) {
                locations.add(location);
            }
        }
        return locations;
    }

    @Override
    public List<Location> getLocations(GeoCoordinate coordinate, double distance) {
        throw new UnsupportedOperationException("Not supported by " + getClass().getName() + ".");
    }

    @Override
    public Iterator<Location> getLocations() {
        throw new UnsupportedOperationException("Not supported by " + getClass().getName() + ".");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Not supported by " + getClass().getName() + ".");
    }
}
