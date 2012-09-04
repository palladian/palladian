package ws.palladian.processing.features;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
public class FeatureVector implements Iterable<Feature<?>> {
    /**
     * <p>
     * Used for serializing objects of this class. Should only change if the attribute set of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -1873151817193636793L;
    /**
     * <p>
     * A map of all {@code Feature}s in this vector. It maps from the {@code Feature}s {@code FeatureVector} wide unique
     * identifier to an actual {@code Feature} instance containing the value. The value might be of any java object
     * type.
     * </p>
     */
    protected final SortedMap<String, Feature<?>> features;

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
     * Creates a new {@link FeatureVector} from the provided FeatureVector, i.e. a copy with all {@link Feature}s.
     * 
     * @param featureVector The feature vector which Features to copy.
     */
    public FeatureVector(FeatureVector featureVector) {
        features = new TreeMap<String, Feature<?>>(featureVector.features);
    }

    /**
     * <p>
     * Adds a new {@code Feature} to this {@code FeatureVector}. If a feature with this identifier already exists, it
     * will be replaced by the supplied one.
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
     * Adds a new {@code Feature} to this {@code FeatureVector}. If a feature with this identifier already exists, it
     * will be replaced by the supplied one.
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

    /**
     * <p>
     * Provides all {@link Feature}s with the specified type from this {@link FeatureVector}.
     * </p>
     * 
     * @param type The type of the {@link Feature}s to retrieve.
     * @return A {@link List} of {@link Feature}s for the specified type or an empty List of no such {@link Feature}s
     *         exist, never <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public <T> List<Feature<T>> getAll(Class<T> type) {
        List<Feature<T>> ret = new ArrayList<Feature<T>>();
        for (Feature<?> feature : features.values()) {
            if (type.isInstance(feature.getValue())) {
                ret.add((Feature<T>)feature);
            }
        }
        return ret;
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
        if (feature == null) {
            return null;
        } else {
            return descriptor.getType().cast(feature);
        }
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
    public Feature<?>[] toArray() {
        return features.values().toArray(new Feature[features.size()]);
    }

    /**
     * <p>
     * Get the dimension of this feature vector, i.e. how many {@link Feature}s the vector contains.
     * </p>
     * 
     * @return The size of this {@code FeatureVector}.
     */
    public int size() {
        return features.size();
    }

    /**
     * <p>
     * Removes a {@link Feature} from this {@link FeatureVector}.
     * </p>
     * 
     * @param identifier
     *            The {@link FeatureVector} wide unique identifier of the {@link Feature} to remove.
     * @return <code>true</code> if the {@link Feature} was removed, <code>false</code> if there was no feature with the
     *         specified identifier to remove.
     */
    public boolean remove(String identifier) {
        return features.remove(identifier) != null;
    }

    /**
     * <p>
     * Removes a {@link Feature} from this {@link FeatureVector}.
     * </p>
     * 
     * @param descriptor The {@link FeatureDescriptor} providing a unique identifier and the concrete type of the
     *            {@link Feature} to remove.
     * @return <code>true</code> if the {@link Feature} was removed, <code>false</code> if there was no feature with the
     *         specified identifier to remove.
     */
    public boolean remove(FeatureDescriptor<?> featureDescriptor) {
        return features.remove(featureDescriptor.getIdentifier()) != null;
    }

    @Override
    public Iterator<Feature<?>> iterator() {
        return features.values().iterator();
    }

    /**
     * <p>
     * Adds all features from the provided {@code FeatureVector} to this {@code FeatureVector}.
     * </p>
     * 
     * @param featureVector The {@code FeatureVector} containing the {@link Feature}s to add.
     */
    public void addAll(final FeatureVector featureVector) {
        for (Feature<?> feature : featureVector) {
            this.add(feature);
        }
    }
}
