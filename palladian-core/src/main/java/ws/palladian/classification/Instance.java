package ws.palladian.classification;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.Classified;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Instance used by classifiers to train new models.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 2.0.0
 * @since 0.1.8
 */
public class Instance implements Classified {

    /**
     * <p>
     * The {@link FeatureVector} used by a processing classifier to train new {@link Model}.
     * </p>
     */
    private final FeatureVector featureVector;

    /**
     * <p>
     * The target class this {@code Instance} belongs to.
     * </p>
     */
    private final String targetClass;

    /**
     * <p>
     * Creates a new completely initialized {@code Instance}.
     * </p>
     * 
     * @param targetClass The target class this {@code Instance} belongs to.
     * @param featureVector The {@link FeatureVector} used by a processing classifier to train new {@link Model}.
     */
    public Instance(String targetClass, FeatureVector featureVector) {
        Validate.notNull(targetClass, "targetClass must not be null");
        Validate.notNull(featureVector, "featureVector must not be null");
        this.targetClass = targetClass;
        this.featureVector = featureVector;
    }

    /**
     * <p>
     * Creates a new completely initialized {@link Instance} with an empty {@link FeatureVector}.
     * </p>
     * 
     * @param targetClass The target class this {@link Instance} belongs to, not <code>null</code>.
     */
    public Instance(String targetClass) {
        this(targetClass, new FeatureVector());
    }

    @Override
    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    @Override
    public String getTargetClass() {
        return targetClass;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Instance [featureVector=");
        builder.append(featureVector);
        builder.append(", target=");
        builder.append(targetClass);
        builder.append("]");
        return builder.toString();
    }
}
