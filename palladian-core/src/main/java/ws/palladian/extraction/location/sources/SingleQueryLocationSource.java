package ws.palladian.extraction.location.sources;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;

/**
 * <p>
 * Common base class for {@link LocationSource}s which do not support getting multiple entities in one go (like
 * {@link LocationSource#getLocations(List)}). This implementation simply splits such queries to their single-argument
 * counterpart and combines the results.
 * </p>
 * 
 * @author Philipp Katz
 */
abstract class SingleQueryLocationSource implements LocationSource {

    @Override
    public Collection<Location> getLocations(Collection<String> locationNames, Set<Language> languages) {
        Collection<Location> locations = CollectionHelper.newHashSet();
        for (String locationName : locationNames) {
            locations.addAll(getLocations(locationName, languages));
        }
        return locations;
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
