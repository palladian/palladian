package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import ws.palladian.helper.constants.Language;

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
     * Search for {@link Location}s by a given name.
     * </p>
     * 
     * @param locationName The name of the location to search, not <code>null</code>.
     * @return A list of locations matching the given name, or an empty list, if no matches were found, never
     *         <code>null</code>.
     */
    Collection<Location> retrieveLocations(String locationName);

    /**
     * <p>
     * Search for {@link Location}s by a given name in a specified set of {@link Language}s.
     * </p>
     * 
     * @param locationName The name of the location to search, not <code>null</code>.
     * @param languages A set of {@link Language}s in which the given name must be, not <code>null</code>. Names in
     *            other languages than the specified one(s) are not retrieved, while names without explicitly defined
     *            language always match.
     * @return A list of locations matching the given name, or an empty list, if no matches were found, never
     *         <code>null</code>.
     */
    Collection<Location> retrieveLocations(String locationName, EnumSet<Language> languages);

    /**
     * <p>
     * Get a {@link Location} by its unique identifier. The identifier is implementation specific; this means, in
     * general a location ID for a specific location must not be the same across different {@link LocationSource}s.
     * </p>
     * 
     * @param locationId The identifier of the location to retrieve.
     * @return The location for the given identifier, or <code>null</code> if no such location was found.
     */
    Location retrieveLocation(int locationId);

    /**
     * <p>
     * Get the logical hierarchy for a given {@link Location}. For example, "Baden-Württemberg" is contained in
     * "Germany", which is contained in "Europe", which is contained in "Earth". The given location as point of origin
     * is <b>not</b> included in the returned hierarchy. The order of the returned list is from specific to general
     * (e.g. the last element in the list would be "Earth").
     * </p>
     * 
     * @param location The identifier of the location for which to retrieve the hierarchy.
     * @return The hierarchy as list of parents for the given location, or an empty list, if no hierarchy was found,
     *         never <code>null</code>.
     */
    List<Location> getHierarchy(int locationId);

}
