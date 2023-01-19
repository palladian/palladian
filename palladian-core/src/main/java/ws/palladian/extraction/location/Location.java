package ws.palladian.extraction.location;

import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;

import java.util.*;

/**
 * <p>
 * This interface defines a geographic location, like a city, country, continent, etc. Use {@link AbstractLocation} for
 * your own implementation.
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
     * <code>null</code>.
     * @see #collectAlternativeNames()
     */
    Collection<AlternativeName> getAlternativeNames();

    Collection<String> getAlternativeNameStrings();

    /**
     * @return The type of this location. {@link LocationType#UNDETERMINED} if no type was specified, but never
     * <code>null</code>.
     */
    LocationType getType();

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
     * <code>null</code>.
     */
    List<Integer> getAncestorIds();

    /**
     * <p>
     * Determine, whether this location is hierarchical descendant of the given location.
     * </p>
     *
     * @param other The other location, not <code>null</code>.
     * @return <code>true</code> in case this location is descendant of the specified one, <code>false</code> otherwise.
     * @see #getAncestorIds()
     */
    boolean descendantOf(Location other);

    /**
     * <p>
     * Determine, whether this location is hierarchical child of the given location.
     * </p>
     *
     * @param other The other location, not <code>null</code>.
     * @return <code>true</code> in case this location is child of the specified one, <code>false</code> otherwise.
     * @see #getAncestorIds()
     */
    boolean childOf(Location other);

    /**
     * <p>
     * Determine, whether this location and the given one share a common name. Names are normalized according to the
     * rules given in {@link LocationExtractorUtils#normalizeName(String)}.
     * </p>
     *
     * @param other The other location, not <code>null</code>.
     * @return <code>true</code> in case at least one common name exists, <code>false</code> otherwise.
     */
    boolean commonName(Location other);

    /**
     * <p>
     * Get a {@link Set} of all names for this location, i.e. the primary name and all alternative names.
     * </p>
     *
     * @return {@link Set} with all alternative names.
     * @see #getAlternativeNames()
     */
    Set<String> collectAlternativeNames();

    /**
     * @return The geographical latitude of this location, or <code>null</code> if no coordinates exist.
     * @deprecated Use {@link #getCoordinate()} instead.
     */
    @Deprecated
    Double getLatitude();

    /**
     * @return The geographical longitude of this location, or <code>null</code> if no coordinates exist.
     * @deprecated Use {@link #getCoordinate()} instead.
     */
    @Deprecated
    Double getLongitude();

    /**
     * @return The geographical coordinate for this location, or <code>null</code> in case no coordinates exist.
     */
    GeoCoordinate getCoordinate();

    /**
     * @return The geographical coordinate for this location if present.
     */
    Optional<GeoCoordinate> getCoords();

    /**
     * <p>
     * Determine, whether this location has a specified (primary or alternative) name, either with no explicit language
     * or with the language specified as parameter.
     *
     * @param name      The name for which to check, not <code>null</code>. Match is case insensitive.
     * @param languages The languages which match (alternative names without language are always considered), not
     *                  <code>null</code>.
     * @return <code>true</code> in case the location the name in the specified language(s), or in case it has the name
     * without explicitly defined language or as primary name.
     */
    boolean hasName(String name, Set<Language> languages);

    /**
     * @return Meta data connected to the location.
     */
    Map<String, Object> getMetaData();
}
