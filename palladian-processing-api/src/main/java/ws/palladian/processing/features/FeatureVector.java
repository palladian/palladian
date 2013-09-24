/**
 * Created on 04.09.2013 12:28:38
 */
package ws.palladian.processing.features;

import java.util.Collection;

import ws.palladian.processing.Classifiable;

/**
 * <p>
 * Interface for a feature vector. A feature vector is a data structure that can hold features of a document and is
 * required to classify a document. Feature vectors are also used by Palladian pipelines to store all data extracted or
 * created during intermediate processing steps. The final result of a pipeline is stored as feature into a feature
 * vector as well.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.2
 */
public interface FeatureVector extends Iterable<Feature<?>>, Classifiable {

    /**
     * <p>
     * Adds a new {@code Feature} to this {@code FeatureVector} or overwrites an existing one with the same name without
     * warning.
     * </p>
     * 
     * @param feature
     *            The actual {@code Feature} instance containing the value.
     */
    void add(Feature<?> feature);

    /**
     * <p>
     * Adds all provided {@link Feature}s to this {@link FeatureVector} and overwrites existing {@link Feature} s with
     * the same name.
     * </p>
     * 
     * @param features The {@link Feature}s to add to this {@link FeatureVector}.
     */
    void addAll(Iterable<? extends Feature<?>> features);

    /**
     * <p>
     * Provides the {@link Feature} with the provided name cast to the provided feature subtype.
     * </p>
     * 
     * @param type The type to cast to.
     * @param name The name of the {@link Feature} to get and cast.
     * @return Either the requested {@link Feature} or {@code null} if the {@link Feature} is not available or not of
     *         the correct type.
     */
    <T extends Feature<?>> T get(Class<T> type, String name);

    /**
     * <p>
     * Provides the {@link Feature} with the provided name.
     * </p>
     * 
     * @param name The name of the queried {@link Feature}.
     * @return The queried {@link Feature} or {@code null} if no such {@link Feature} exists.
     */
    Feature<?> get(String name);

    /**
     * <p>
     * Provides all {@link Feature}s with the specified type from this {@link FeatureVector}.
     * </p>
     * 
     * @param type The type of the {@link Feature}s to retrieve.
     * @return A {@link Collection} of {@link Feature}s for the specified type or an empty Collection of no such
     *         {@link Feature}s exist, never <code>null</code>.
     */
    <T extends Feature<?>> Collection<T> getAll(Class<T> type);

    /**
     * <p>
     * Provides all direct {@link Feature}s of this {@link FeatureVector}. Remember that each {@link Feature} may have
     * {@link Feature}s itself. In such a case you need to get those features recursively.
     * </p>
     * 
     * @return All {@link Feature}s of this {@link FeatureVector}.
     */
    Collection<Feature<?>> getAll();

    /**
     * <p>
     * Get the dimension of this feature vector, i.e. how many {@link Feature}s the vector contains.
     * </p>
     * 
     * @return The size of this {@code FeatureVector}.
     */
    int size();

    /**
     * <p>
     * Removes all {@link Feature}s with the specified name from this {@link FeatureVector}.
     * </p>
     * 
     * @param name The name of the {@link Feature}s to remove.
     * @return <code>true</code> if the {@link Feature} was removed, <code>false</code> if there was no feature with the
     *         specified identifier to remove.
     */
    boolean remove(String name);

    /**
     * <p>
     * Empties this {@link FeatureVector}.
     * </p>
     */
    void clear();

}
