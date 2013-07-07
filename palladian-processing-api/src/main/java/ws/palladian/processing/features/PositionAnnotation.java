package ws.palladian.processing.features;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.processing.Classifiable;

/**
 * <p>
 * An annotation which points to text fragments. All position indices are zero based. The end is marked by the index of
 * the first character not belonging to the {@link PositionAnnotation}. Instances should be created using the
 * {@link PositionAnnotationFactory}.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @author David Urbansky
 */
// FIXME rename to TextAnnotation
public class PositionAnnotation extends Annotation implements Classifiable, Feature<String> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PositionAnnotation.class);

    // lazy-initialized field
    /**
     * <p>
     * A {@link FeatureVector} for this annotation. Annotations may have {@link FeatureVector}s, so it is possible to
     * classify them as well. This is for example important for named entity recognition or part of speech tagging.
     * </p>
     */
    private FeatureVector featureVector;

    /**
     * <p>
     * Creates a new {@link PositionAnnotation} completely initialized. <b>Important:</b> Prefer creating instances
     * using the {@link PositionAnnotationFactory}.
     * </p>
     * 
     * @param startPosition The position of the first character of this {@link PositionAnnotation}.
     * @param endPosition The position of the first character after the end of this {@link PositionAnnotation}.
     * @param index The running index of this {@link PositionAnnotation}.
     * @param value The text value of this {@link PositionAnnotation}.
     */
    public PositionAnnotation(String value, int startPosition) {
        super(startPosition, value, value + startPosition);
    }

    @Override
    public FeatureVector getFeatureVector() {
        if (featureVector == null) {
            featureVector = new FeatureVector();
        }
        return featureVector;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Annotation [name=");
        builder.append(getName());
        builder.append(", value=");
        builder.append(getValue());
        builder.append(", startPosition=");
        builder.append(getStartPosition());
        builder.append(", endPosition=");
        builder.append(getEndPosition());
        builder.append(", featureVector=");
        builder.append(getFeatureVector());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public String getName() {
        return getTag();
    }

    @Override
    public void setValue(String value) {
        // throw new UnsupportedOperationException("Don't do that!");
        LOGGER.warn("Modifications are not allowed and will be ignored.");
    }

}
