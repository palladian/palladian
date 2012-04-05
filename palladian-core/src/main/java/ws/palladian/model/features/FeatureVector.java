package ws.palladian.model.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <p>
 * A class to describe collections of {@code Feature}s extracted from some document. Based on its {@code FeatureVector}
 * the document can be processed by Information Retrieval components like classifiers or clusterers.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @author Philipp Katz
 */
public class FeatureVector {
    /**
     * <p>
     * A map of all {@code Feature}s in this vector. It maps from the {@code Feature}s {@code FeatureVector} wide unique
     * identifier to an actual {@code Feature} instance containing the value. The value might be of any java object
     * type.
     * </p>
     */
    protected final transient SortedMap<String, Feature<?>> features;

    /**
     * <p>
     * Creates a new empty {@code FeatureVector}. To fill it with {@link Feature}s call {@link #add(String, Feature)}.
     * </p>
     */
    public FeatureVector() {
        features = new TreeMap<String, Feature<?>>();
    }

    /**
     * <p>
     * Adds a new {@code Feature} to this {@code FeatureVector}.
     * </p>
     * 
     * @param identifier
     *            The {@code Feature}s {@code FeatureVector} wide unique identifier.
     * @param newFeature
     *            The actual {@code Feature} instance containing the value.
     * @deprecated use {@link #add(Feature)} instead.
     */
    @Deprecated
    public void add(String identifier, Feature<?> newFeature) {
        features.put(identifier, newFeature);
    }

    /**
     * <p>
     * Adds a new {@code Feature} to this {@code FeatureVector}.
     * </p>
     * 
     * @param newFeature
     *            The actual {@code Feature} instance containing the value.
     */
    public void add(Feature<?> newFeature) {
        features.put(newFeature.getName(), newFeature);
    }

    /**
     * <p>
     * Provides a {@code Feature} from this {@code FeatureVector}.
     * </p>
     * 
     * @param identifier
     *            The {@code FeatureVector} wide unique identifier of the requested {@code Feature}.
     * @return The {@code Feature} with identifier {@code identifier} or {@code null} if no such {@code Feature} exists.
     * @deprecated Prefer using {@link #get(FeatureDescriptor)} when a {@link FeatureDescriptor} is available. This
     *             improves type safety and avoids unnecessary casting.
     */
    @Deprecated
    public Feature<?> get(String identifier) {
        return features.get(identifier);
    }

    @SuppressWarnings("unchecked")
    public <F extends Feature<T>, T> List<F> getAll(Class<T> clazz) {
        List<F> collection = new ArrayList<F>();
        
        for (Entry<String, Feature<?>> featureEntry : features.entrySet()) {
            if(clazz.isInstance(featureEntry.getValue())) {
                collection.add((F) clazz.cast(featureEntry.getValue()));
            }
        }
        
        return collection;
    }
    
    /**
     * <p>
     * Provides a {@link Feature} from this {@link FeatureVector}.
     * </p>
     * 
     * @param descriptor The {@link FeatureDescriptor} providing a unique identifier and the concrete type of the
     *            requested {@link Feature}.
     * @return The {@link Feature} for the specified {@link FeatureDescriptor} or <code>null</code> if no such
     *         {@link Feature} exists.
     */
    public <T extends Feature<?>> T get(FeatureDescriptor<T> descriptor) {
        Feature<?> feature = features.get(descriptor.getIdentifier());
        return descriptor.getType().cast(feature);
    }

    @Override
    public String toString() {
        return features.values().toString();
    }

    /**
     * <p>
     * Converts this {@code FeatureVector} into an array of {@code Feature}s.
     * </p>
     * 
     * @return The vector as array.
     */
    public Feature<?>[] toValueArray() {
        Feature<?>[] ret = new Feature[features.size()];
        ret = features.values().toArray(ret);
        return ret;
    }

    /**
     * <p>
     * Counts the dimensions, i.e. how many entries the vector has.
     * </p>
     * 
     * @return The dimensions of this {@code FeatureVector}.
     */
    public int countDimensions() {
        return this.features.size();
    }

}
