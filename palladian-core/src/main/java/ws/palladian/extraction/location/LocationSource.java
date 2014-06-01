package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;

/**
 * <p>
 * A {@link LocationSource} primarily provides the ability to search for geographical locations by a given name.
 * Implementations of this interface might by geographical web services, or relational databases.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public interface LocationSource {

    /**
     * <p>
     * Search for {@link Location}s by a given name in a specified set of {@link Language}s.
     * </p>
     * 
     * @param locationName The name of the location to search, not <code>null</code>.
     * @param languages A set of {@link Language}s in which the given name must be, not <code>null</code>. Names in
     *            other languages than the specified one(s) are not retrieved, while names without explicitly defined
     *            language always match.
     * @return A collection of locations matching the given name, or an empty collection, if no matches were found,
     *         never <code>null</code>.
     */
    Collection<Location> getLocations(String locationName, Set<Language> languages);

    /**
     * <p>
     * Search for multiple {@link Location}s by given names in a specified set of {@link Language}s. When multiple
     * {@link Location}s need to be searched at one go, this allows implementations performance optimizations in
     * contrast to {@link #getLocation(int)}. E.g. database implementations can do this with only one round trip.
     * </p>
     * 
     * @param locationName The names of the location to search, not <code>null</code>.
     * @param languages A set of {@link Language}s in which the given name must be, not <code>null</code>. Names in
     *            other languages than the specified one(s) are not retrieved, while names without explicitly defined
     *            language always match.
     * @return A map containing the given query names as keys and the found locations as value collections, or empty
     *         collections, if no matches were found, never <code>null</code>.
     */
    MultiMap<String, Location> getLocations(Collection<String> locationNames, Set<Language> languages);

    /**
     * <p>
     * Get a {@link Location} by its unique identifier. The identifier is implementation specific; this means, in
     * general a location ID for a specific location must not be the same across different {@link LocationSource}s.
     * </p>
     * 
     * @param locationId The identifier of the location to retrieve.
     * @return The location for the given identifier, or <code>null</code> if no such location was found.
     */
    Location getLocation(int locationId);

    /**
     * <p>
     * Get a list of {@link Location}s by their IDs. When multiple {@link Location}s need to be fetched at one go, this
     * allows implementations performance optimizations in contrast to {@link #getLocation(int)}. E.g. database
     * implementations can do this with only one round trip.
     * </p>
     * 
     * @param locationIds The IDs for the {@link Location}s to retrieve, not <code>null</code>.
     * @return List of {@link Location}s in the same order as the provided IDs. If a location for a specific ID could
     *         not be found, the returned list might be smaller than the list of supplied IDs.
     */
    List<Location> getLocations(List<Integer> locationIds);

    /**
     * <p>
     * Get a List of all {@link Location}s which are within a specified distance from the given {@link GeoCoordinate}.
     * </p>
     * 
     * @param coordinate The {@link GeoCoordinate} representing the center around which to search.
     * @param distance The maximum distance from the given coordinate, must be greater/equal zero.
     * @return A list of {@link Location}s which are within the given distance from the specified coordinate ordered by
     *         distance (closest first), or an empty list, if no locations are found, never <code>null</code>.
     */
    List<Location> getLocations(GeoCoordinate coordinate, double distance);

    /**
     * <p>
     * Get an {@link Iterator} over all locations within this location source.
     * </p>
     * 
     * @return An iterator over all locations within this location source.
     * @throws UnsupportedOperationException In case iterating is not supported (e.g. in Web APIs).
     */
    Iterator<Location> getLocations();

    /**
     * @return The number of locations in this location source.
     * @throws UnsupportedOperationException In case this information is not provided (e.g. in Web APIs).
     */
    int size();

}
