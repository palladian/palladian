package ws.palladian.processing.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import ws.palladian.processing.Classifiable;

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
public final class FeatureVector implements Iterable<Feature<?>>, Classifiable {

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

    public void addAll(Iterable<? extends Feature<?>> features) {
        for (Feature<?> feature : features) {
            add(feature);
        }
    }

    public <T extends Feature<?>> T getFeature(Class<T> type, String name) {
        List<T> selectedFeatures = getAll(type, name);
        if (selectedFeatures.isEmpty()) {
            return null;
        }
        return selectedFeatures.get(0);
    }

    /**
     * <p>
     * Provides the {@link Feature} with the provided name. If there are multiple {@link Feature}s with the same name it
     * provides an arbitrary one.
     * </p>
     * 
     * @param name The name of the queried {@link Feature}.
     * @return The queried {@link Feature} or {@code null} if no such {@link Feature} exists.
     */
    public Feature<?> getFeature(String name) {
        List<Feature<?>> selectedFeatures = getAll(name);
        if (selectedFeatures.isEmpty()) {
            return null;
        }
        return selectedFeatures.get(0);
    }

    /**
     * <p>
     * Provides all features under a certain name with a certain implementation type.
     * </p>
     * 
     * @param type The queried type.
     * @param name The name of the {@link Feature}s to provide.
     * @return The {@link List} of all {@link Feature}s with the provided type and name or an empty {@link List} if no
     *         such {@link Feature}s exist.
     */
    public <T extends Feature<?>> List<T> getAll(Class<T> type, String name) {
        List<T> selectedFeatures = new ArrayList<T>();
        for (Feature<?> feature : getAll(type)) {
            if (feature.getName().equals(name)) {
                selectedFeatures.add(type.cast(feature));
            }
        }
        // return selectedFeatures;
        // changed this to a immutable list, else wise it might cause confusion, because the returned list is not
        // intended to be modified -- Philipp, 2012-11-16.
        return Collections.unmodifiableList(selectedFeatures);
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
     * Provides all features available via the provided name.
     * </p>
     * 
     * @param name The name of the queried {@link Feature} or {@link Feature}s.
     * @return The {@link List} of all {@link Feature}s matching the provided name in this {@link FeatureVector} or an
     *         empty {@link List} if no such {@link Feature}s are available.
     */
    public List<Feature<?>> getAll(String name) {
        List<Feature<?>> featureList = features.get(name);
        if (featureList != null) {
            return Collections.unmodifiableList(featureList);
        }
        return Collections.emptyList();
    }

    /**
     * <p>
     * Provides all direct {@link Feature}s of this {@link FeatureVector}. Remember that each {@link Feature} may have
     * {@link Feature}s itself. In such a case you need to get those features recursively.
     * </p>
     * 
     * @return All {@link Feature}s of this {@link FeatureVector}.
     */
    public List<Feature<?>> getAll() {
        List<Feature<?>> featureList = new ArrayList<Feature<?>>();
        for (List<Feature<?>> features : this.features.values()) {
            featureList.addAll(features);
        }
        return Collections.unmodifiableList(featureList);
    }

    @Override
    public String toString() {
        return features.values().toString();
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
     * Removes all {@link Feature}s with the specified name from this {@link FeatureVector}.
     * </p>
     * 
     * @param name The name of the {@link Feature}s to remove.
     * @return <code>true</code> if the {@link Feature} was removed, <code>false</code> if there was no feature with the
     *         specified identifier to remove.
     */
    public boolean removeAll(String name) {
        return features.remove(name) != null;
    }

    @Override
    public Iterator<Feature<?>> iterator() {
        // return getFlat().iterator();
        return getAll().iterator();
    }

    /**
     * <p>
     * Empties this {@link FeatureVector}.
     * </p>
     */
    public void clear() {
        features.clear();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((features == null) ? 0 : features.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FeatureVector other = (FeatureVector)obj;
        if (features == null) {
            if (other.features != null)
                return false;
        } else if (!features.equals(other.features))
            return false;
        return true;
    }

    /**
     * <p>
     * Removes a {@link Feature} from this {@link FeatureVector}.
     * </p>
     * 
     * @param feature The {@link Feature} to remove.
     */
    public synchronized void remove(Feature<?> feature) {
        String[] featurePath = feature.getName().split("/");
        String localName = featurePath[featurePath.length - 1];
        List<Feature<?>> existingFeatures = new ArrayList<Feature<?>>(features.get(localName));
        existingFeatures.remove(feature);
        if (!existingFeatures.isEmpty()) {
            features.put(localName, existingFeatures);
        } else {
            features.remove(localName);
        }
    }

    @Override
    public FeatureVector getFeatureVector() {
        return this;
    }
}