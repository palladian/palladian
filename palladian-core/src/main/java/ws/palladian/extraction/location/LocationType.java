package ws.palladian.extraction.location;

/**
 * <p>
 * This enumeration provides available types for different {@link Location}s.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public enum LocationType {
    /** A continent, like <i>Asia</i>. */
    CONTINENT,
    /** A country, like <i>Japan</i>. */
    COUNTRY,
    /** A city, like <i>Tokyo</i>. */
    CITY,
    /** A ZIP code for a city. */
    ZIP,
    /** The name of a street. */
    STREET,
    /** The number of a building within a street. */
    STREETNR,
    /** A political or administrative unit like state, county, district. */
    UNIT,
    /** An area independent from or spanning multiple political or administrative units . */
    REGION,
    /** A human-made point of interest, like hotels, museums, universities, monuments, etc. */
    POI,
    /** Geographic features like rivers, canyons, lakes, islands, waterfalls, etc. */
    LANDMARK,
    /** An undetermined or unknown type. */
    UNDETERMINED
}
