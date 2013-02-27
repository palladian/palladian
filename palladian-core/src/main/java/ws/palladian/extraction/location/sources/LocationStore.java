package ws.palladian.extraction.location.sources;

import java.util.Collection;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;

/**
 * <p>
 * A {@link LocationStore} extends a {@link LocationSource} with the ability to add and save new {@link Location}s. A
 * typical example for a potential implementation of a {@link LocationStore} is a relational database.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface LocationStore extends LocationSource {

    /**
     * <p>
     * Add a {@link Location} to the location store. In case the provided {@link Location} already exists in the store,
     * it should be overwritten/updated. Concrete implementations should decide about equality between {@link Location}s
     * individually; in general there are two strategies: 1) Use identifiers, as provided by {@link Location#getId()}
     * (useful when locations are imported from a specific source and the identifiers are replicated to the store). 2)
     * Use geographical/semantic properties to decide about equality (e.g. check via coordinates and type, if a
     * similar/identical item already exists.
     * </p>
     * 
     * @param location The location to add, not <code>null</code>.
     */
    void save(Location location);

    /**
     * <p>
     * Add a hierarchy relation between two locations, identified by their IDs (see {@link Location#getId()}). An
     * example for a hierarchy would be the tuple "Baden-Württemberg" (child), "Germany" (parent). A hierarchy should
     * only be created for <b>directly adjacent</b> levels and not be spanning intermediate levels; for example,
     * <b>no</b> explicit hierarchy must be created between "Regierungsbezirk Stuttgart" (child) and "Germany" (parent),
     * because "Regierungsbezirk Stuttgart" is contained in "Baden-Württemberg".
     * </p>
     * 
     * @param childId The identifier of the child {@link Location}, not equals {@code parentId}.
     * @param parentId The identifier of the parent {@link Location}, not equal {@code childId}.
     * @param priority The priority for the hierarchy relation, smaller values denote a higher priority.
     */
    void addHierarchy(LocationRelation hierarchy);

    /**
     * <p>
     * Add a {@link Collection} of {@link AlternativeName}s to the location with the specified ID.
     * </p>
     * 
     * @param locationId The identifier of the location to which to add the alternative names.
     * @param alternativeNames The {@link Collection} of {@link AlternativeName}s, not <code>null</code>.
     */
    void addAlternativeNames(int locationId, Collection<AlternativeName> alternativeNames);

    /**
     * <p>
     * Return the highest location id in the source so that other importers can avoid using duplicate location ids.
     * </p>
     * 
     * @return The highest location id in the source.
     */
    Integer getHighestId();
}
