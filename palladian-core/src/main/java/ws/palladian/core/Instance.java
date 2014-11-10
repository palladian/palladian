package ws.palladian.core;

/**
 * <p>
 * An instance (in the machine learning sense) for training e.g. a classifier. The instance consists of a feature
 * vector, a (manually) assigned category (e.g. "SPAM" or "NO-SPAM") and (optionally) a weight, which allows to
 * prioritize of different training samples (not supported by all classifiers).
 * 
 * <p>
 * Instances are usually created using the {@link InstanceBuilder}.
 * 
 * @author pk
 *
 */
public interface Instance {

    /**
     * @return The feature vector, not <code>null</code>.
     */
    FeatureVector getVector();

    /**
     * @return The name of the category, not <code>null</code>.
     */
    String getCategory();

    /**
     * @return The weight of this instance, which must be equal/greater <code>1</code> (return a value of one for no
     *         particular weighting).
     */
    int getWeight();

}
