package ws.palladian.processing.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @version 2.5
 */
public class FeatureVector implements Iterable<Feature<?>>, Classifiable {

    /**
     * <p>
     * The logger for objects of this class. Configure it using <code>/src/main/resources/log4j.properties</code>.
     * </p>
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureVector.class);

    /**
     * <p>
     * A map of all {@code Feature}s in this vector. It maps from the {@code Feature}s {@code FeatureVector} wide unique
     * identifier to an actual {@code Feature} instance containing the value. The value might be of any java object
     * type.
     * </p>
     */
    private final SortedMap<String, Feature<?>> features;

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
     * </p>
     * 
     * @param featureVector The feature vector which Features to copy.
     */
    public FeatureVector(FeatureVector featureVector) {
        features = new TreeMap<String, Feature<?>>(featureVector.features);
    }

    /**
     * <p>
     * Adds a new {@code Feature} to this {@code FeatureVector} or overwrites an existing one with the same name without
     * warning.
     * </p>
     * 
     * @param feature
     *            The actual {@code Feature} instance containing the value.
     */
    public void add(Feature<?> feature) {
        if (features.get(feature.getName())!=null) {
            LOGGER.warn("Please use a ListFeature to add multiple features with the same name.");
        }
        features.put(feature.getName(), feature);
    }

    /**
     * <p>
     * Adds all provided {@link Feature}s to this {@link FeatureVector} and overwrites existing {@link Feature}s with
     * the same name.
     * </p>
     * 
     * @param features The {@link Feature}s to add to this {@link FeatureVector}.
     */
    public void addAll(Iterable<? extends Feature<?>> features) {
        for (Feature<?> feature : features) {
            add(feature);
        }
    }

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
    public <T extends Feature<?>> T get(Class<T> type, String name) {
        try {
            return type.cast(features.get(name));
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * <p>
     * Provides the {@link Feature} with the provided name.
     * </p>
     * 
     * @param name The name of the queried {@link Feature}.
     * @return The queried {@link Feature} or {@code null} if no such {@link Feature} exists.
     */
    public Feature<?> get(String name) {
        return features.get(name);
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
        for (Feature<?> feature : features.values()) {
            if (type.isInstance(feature)) {
                selectedFeatures.add(type.cast(feature));
            }
        }
        return selectedFeatures;
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
        for (Feature<?> feature : this.features.values()) {
            featureList.add(feature);
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
    public boolean remove(String name) {
        return features.remove(name) != null;
    }

    @Override
    public Iterator<Feature<?>> iterator() {
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
        features.remove(feature.getName());
    }

    @Override
    public FeatureVector getFeatureVector() {
        return this;
    }
}