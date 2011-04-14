package ws.palladian.model.features;

import java.util.HashMap;
import java.util.Map;

/**
 * A class to describe collections of {@code Feature}s extracted from some
 * document. Based on its {@code FeatureVector} the document can be processed by
 * Information Retrieval components like classifiers or clusterers.
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 */
public final class FeatureVector {
    /**
     * A map of all {@code Feature}s in this vector. It maps from the {@code Feature}s {@code FeatureVector} wide unique
     * identifier to an
     * actual {@code Feature} instance containing the value. The value might be
     * of any java object type.
     */
    private final transient Map<String, Feature<?>> features;

    /**
     * Creates a new empty {@code FeatureVector}. Too fill it with {@link Feature}s call {@link #add(String, Feature)}.
     */
    public FeatureVector() {
        features = new HashMap<String, Feature<?>>();
    }

    /**
     * Adds a new {@code Feature} to this {@code FeatureVector}.
     * 
     * @param identifier
     *            The {@code Feature}s {@code FeatureVector} wide unique
     *            identifier.
     * @param newFeature
     *            The actual {@code Feature} instance containing the value.
     */
    public void add(String identifier, Feature<?> newFeature) {
        features.put(identifier, newFeature);
    }

    /**
     * Provides a {@code Feature} from this {@code FeatureVector}.
     * 
     * @param identifier
     *            The {@code FeatureVector} wide unique identifier of the
     *            requested {@code Feature}.
     * @return The {@code Feature} with identifier {@code identifier} or {@code null} if no such {@code Feature} exists.
     */
    public Feature<?> get(String identifier) {
        return features.get(identifier);
    }
}
