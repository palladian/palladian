package ws.palladian.processing.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class BasicFeatureVector implements FeatureVector {

    /** The logger for objects of this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicFeatureVector.class);

    /** Flag to avoid spamming of log warning messages. */
    private static boolean showedWarning = false;

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
    public BasicFeatureVector() {
        features = new TreeMap<String, Feature<?>>();
    }

    /**
     * <p>
     * Creates a new {@link BasicFeatureVector} from the provided FeatureVector, i.e. a copy with all
     * {@link Feature}s.
     * </p>
     * 
     * @param featureVector The feature vector which Features to copy.
     */
    public BasicFeatureVector(FeatureVector featureVector) {
        features = new TreeMap<String, Feature<?>>();
        for (Feature<?> feature : featureVector.getAll()) {
            features.put(feature.getName(), feature);
        }
    }

    @Override
    public void add(Feature<?> feature) {
        if (features.get(feature.getName()) != null && !showedWarning) {
            LOGGER.warn("Please use a ListFeature to add multiple features with the same name.");
            showedWarning = true;
        }
        features.put(feature.getName(), feature);
    }

    @Override
    public void addAll(Iterable<? extends Feature<?>> features) {
        for (Feature<?> feature : features) {
            add(feature);
        }
    }

    @Override
    public <T extends Feature<?>> T get(Class<T> type, String name) {
        try {
            return type.cast(features.get(name));
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Override
    public Feature<?> get(String name) {
        Feature<?> ret = features.get(name);
        if (ret == null) {
            LOGGER.warn("Unable to find feature with name " + name);
        }
        return ret;
    }

    @Override
    public <T extends Feature<?>> List<T> getAll(Class<T> type) {
        List<T> selectedFeatures = new ArrayList<T>();
        for (Feature<?> feature : features.values()) {
            if (type.isInstance(feature)) {
                selectedFeatures.add(type.cast(feature));
            }
        }
        return selectedFeatures;
    }

    @Override
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

    @Override
    public int size() {
        return features.size();
    }

    @Override
    public boolean remove(String name) {
        return features.remove(name) != null;
    }

    @Override
    public Iterator<Feature<?>> iterator() {
        return getAll().iterator();
    }

    @Override
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
        BasicFeatureVector other = (BasicFeatureVector)obj;
        if (features == null) {
            if (other.features != null)
                return false;
        } else if (!features.equals(other.features))
            return false;
        return true;
    }

    @Override
    public FeatureVector getFeatureVector() {
        return this;
    }

}