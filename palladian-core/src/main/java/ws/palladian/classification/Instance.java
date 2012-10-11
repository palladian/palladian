package ws.palladian.classification;

import ws.palladian.processing.Classified;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Instance used by classifiers to train new models.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 2.0.0
 * @since 0.1.8
 */
public class Instance implements Classified {

    /**
     * <p>
     * The {@link FeatureVector} used by a processing classifier to train new {@link Model}.
     * </p>
     */
    public final FeatureVector featureVector;
    /**
     * <p>
     * The target class this {@code Instance} belongs to.
     * </p>
     */
    public final String targetClass;

    /**
     * <p>
     * Creates a new completely initialized {@code Instance}.
     * </p>
     * 
     * @param targetClass The target class this {@code Instance} belongs to.
     * @param featureVector The {@link FeatureVector} used by a processing classifier to train new {@link Model}.
     */
    public Instance(String targetClass, FeatureVector featureVector) {
        super();

        this.targetClass = targetClass;
        this.featureVector = featureVector;
    }

    @Override
    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    @Override
    public String getTargetClass() {
        return targetClass;
    }

    @Deprecated
    public void setTargetClass(String instanceCategoryName) {

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
