package ws.palladian.extraction.location.sources;

import java.util.Collection;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.ProgressReporter;

/**
 * <p>
 * A {@link LocationStore} provided the ability to add and save new {@link Location}s. A typical example for a potential
 * implementation of a {@link LocationStore} is a relational database. Important: <b>Before</b> adding any data to an
 * instance, you must invoke {@link #startImport()}. After all locations have been added, invoke {@link #finishImport()}.
 * 
 * @author Philipp Katz
 */
public interface LocationStore {

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
    int getHighestId();
    
    /**
     * Invoke before starting import.
     */
    void startImport();

    /**
     * Invoke after finishing import.
     */
    default void finishImport() {
        finishImport(NoProgress.INSTANCE);
    }

    /**
     * Invoke after finishing import.
     *
     * @param progress Progress reporter.
     * @since 2.0
     */
    default void finishImport(ProgressReporter progress) {
        // no op
    }

}
