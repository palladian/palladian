package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * This interface defines a geographic location, like a city, country, continent, etc.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface Location {

    /**
     * @return The unique identifier of this location.
     */
    int getId();

    /**
     * <p>
     * Get the primary name of this location. This is usually the location name of the location (e.g. "München").
     * Alternative spellings or variants in other languages can be retrieved via {@link #getAlternativeNames()}.
     * </p>
     * 
     * @return The primary name of this location, never <code>null</code>.
     */
    String getPrimaryName();

    /**
     * <p>
     * Get alternative spellings or variants in different languages for this location. For example, for the city
     * "München", there is the English variant "Munich", the Italian variant "Monaco", etc.
     * </p>
     * 
     * @return Alternative names for this location, or an empty {@link Collection} if no such names exist. Never
     *         <code>null</code>.
     */
    Collection<AlternativeName> getAlternativeNames();

    /**
     * @return The type of this location. {@link LocationType#UNDETERMINED} if no type was specified, but never
     *         <code>null</code>.
     */
    LocationType getType();

    /**
     * @return The geographical latitude of this location, or <code>null</code> if no coordinates exist.
     */
    Double getLatitude();

    /**
     * @return The geographical longitude of this location, or <code>null</code> if no coordinates exist.
     */
    Double getLongitude();

    /**
     * @return The population of this location, or <code>null</code> if no population values exist.
     */
    Long getPopulation();

    /**
     * <p>
     * Get the logical hierarchy for a given {@link Location}. For example, "Baden-Württemberg" is contained in
     * "Germany", which is contained in "Europe", which is contained in "Earth". The given location as point of origin
     * is <b>not</b> included in the returned hierarchy. The order of the returned list is from specific to general
     * (e.g. the last element in the list would be "Earth").
     * </p>
     * 
     * @return The hierarchy as list of parent IDs, or an empty {@link List}, if no hierarchy was found, never
     *         <code>null</code>.
     */
    List<Integer> getAncestorIds();

}