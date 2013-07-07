package ws.palladian.classification;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
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
public final class Instance implements Trainable {

    /** The {@link Classifiable} providing the {@link FeatureVector}. */
    private final Classifiable classifiable;

    /** The target class this {@code Instance} belongs to. */
    private final String targetClass;

    /**
     * <p>
     * Creates a new completely initialized {@code Instance}.
     * </p>
     * 
     * @param targetClass The target class this {@code Instance} belongs to.
     * @param featureVector The {@link FeatureVector} used by a processing classifier to train new {@link Model}.
     */
    public Instance(String targetClass, Classifiable classifiable) {
        Validate.notNull(targetClass, "targetClass must not be null");
        Validate.notNull(classifiable, "classifiable must not be null");
        this.targetClass = targetClass;
        this.classifiable = classifiable;
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
        return classifiable.getFeatureVector();
    }

    @Override
    public String getTargetClass() {
        return targetClass;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classifiable == null) ? 0 : classifiable.hashCode());
        result = prime * result + ((targetClass == null) ? 0 : targetClass.hashCode());
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
        Instance other = (Instance)obj;
        if (classifiable == null) {
            if (other.classifiable != null)
                return false;
        } else if (!classifiable.equals(other.classifiable))
            return false;
        if (targetClass == null) {
            if (other.targetClass != null)
                return false;
        } else if (!targetClass.equals(other.targetClass))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TrainableWrap [classifiable=");
        builder.append(classifiable);
        builder.append(", targetClass=");
        builder.append(targetClass);
        builder.append("]");
        return builder.toString();
    }
}
