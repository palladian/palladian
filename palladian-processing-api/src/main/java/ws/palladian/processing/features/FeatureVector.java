package ws.palladian.processing.features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
public final class FeatureVector implements Iterable<Feature<?>> {

    /**
     * <p>
     * A map of all {@code Feature}s in this vector. It maps from the {@code Feature}s {@code FeatureVector} wide unique
     * identifier to an actual {@code Feature} instance containing the value. The value might be of any java object
     * type.
     * </p>
     */
    private final SortedMap<String, List<Feature<?>>> features;

    /**
     * <p>
     * Creates a new empty {@code FeatureVector}. To fill it with {@link Feature}s call {@link #add(String, Feature)}.
     * </p>
     */
    public FeatureVector() {
        features = new TreeMap<String, List<Feature<?>>>();
    }

    /**
     * <p>
     * Creates a new {@link FeatureVector} from the provided FeatureVector, i.e. a copy with all {@link Feature}s.
     * </p>
     * 
     * @param featureVector The feature vector which Features to copy.
     */
    public FeatureVector(FeatureVector featureVector) {
        features = new TreeMap<String, List<Feature<?>>>(featureVector.features);
    }

    /**
     * <p>
     * Adds a new {@code Feature} to this {@code FeatureVector}.
     * </p>
     * 
     * @param feature
     *            The actual {@code Feature} instance containing the value.
     */
    public void add(Feature<?> feature) {
        List<Feature<?>> list = features.get(feature.getName());
        if (list == null) {
            list = new ArrayList<Feature<?>>();
            features.put(feature.getName(), list);
        }
        list.add(feature);
    }

    @Deprecated
    private Feature<?> getFeature(String name) {
        List<Feature<?>> selectedFeatures = features.get(name);
        if (selectedFeatures == null || selectedFeatures.isEmpty()) {
            return null;
        }
        return selectedFeatures.get(0);
    }

    @Deprecated
    public <T extends Feature<?>> T getFeature(Class<T> type, String name) {
        List<T> selectedFeatures = getAll(type, name);
        if (selectedFeatures.isEmpty()) {
            return null;
        }
        return selectedFeatures.get(0);
    }

    @Deprecated
    public <T extends Feature<?>> List<T> getAll(Class<T> type, String name) {
        List<T> selectedFeatures = new ArrayList<T>();
        for (Feature<?> feature : getAll(type)) {
            if (feature.getName().equals(name)) {
                selectedFeatures.add(type.cast(feature));
            }
        }
        return selectedFeatures;
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
    public <T extends Feature<?>> List<T> getAll(Class<T> type) {
        List<T> selectedFeatures = new ArrayList<T>();
        for (List<Feature<?>> list : features.values()) {
            for (Feature<?> feature : list) {
                if (type.isInstance(feature)) {
                    selectedFeatures.add(type.cast(feature));
                }
            }
        }
        return selectedFeatures;
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
     * @deprecated Will be removed in the future.
     */
    @Deprecated
    public <T extends Feature<?>> T get(FeatureDescriptor<T> descriptor) {
        return getFeature(descriptor.getType(), descriptor.getIdentifier());
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
        // return features.values().toArray(new Feature[features.size()]);
        return getFlat().toArray(new Feature[0]);
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
     * @param name
     *            The {@link FeatureVector} wide unique identifier of the {@link Feature} to remove.
     * @return <code>true</code> if the {@link Feature} was removed, <code>false</code> if there was no feature with the
     *         specified identifier to remove.
     */
    public boolean remove(String name) {
        return features.remove(name) != null;
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

    /**
     * <p>
     * Adds all features from the provided {@code FeatureVector} to this {@code FeatureVector}.
     * </p>
     * 
     * @param featureVector The {@code FeatureVector} containing the {@link Feature}s to add.
     */
    // public void addAll(final FeatureVector featureVector) {
    // for (Feature<?> feature : featureVector) {
    // this.add(feature);
    // }
    // }

    private List<Feature<?>> getFlat() {
        List<Feature<?>> result = new ArrayList<Feature<?>>();
        for (Entry<String, List<Feature<?>>> entry : features.entrySet()) {
            result.addAll(entry.getValue());
        }
        return result;
    }

    public <T extends Feature<?>> List<T> getFeatures(Class<T> type, String path) {

        String[] pathElements = path.split("/");

        for (String pathElement : pathElements) {
            Feature<?> feature = getFeature(pathElement);
            if (feature instanceof AnnotationFeature) {
                return ((AnnotationFeature)feature).getFeatures(type, path.substring(path.indexOf("/") + 1));
            } else if (feature instanceof Collection) {
                List<T> ret = new ArrayList<T>((Collection<T>)feature);
                return ret;
            } else {
                return getAll(type, pathElement);
            }
        }

        return null;
    }

    public <T extends Feature<?>> Set<T> getFeatureBag(Class<T> featureClass, String featurePath) {
        return new HashSet<T>(getFeatures(featureClass, featurePath));
    }

    @Override
    public Iterator<Feature<?>> iterator() {
        return getFlat().iterator();
    }
}
