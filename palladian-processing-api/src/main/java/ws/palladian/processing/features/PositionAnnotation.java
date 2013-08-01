package ws.palladian.processing.features;

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
public class PositionAnnotation extends ImmutableAnnotation implements Classifiable, Feature<String> {

    /**
     * A {@link FeatureVector} for this annotation. Annotations may have {@link FeatureVector}s, so it is possible to
     * classify them as well. This is for example important for named entity recognition or part of speech tagging. This
     * field is lazy-initialized to save some memory.
     */
    private FeatureVector featureVector;

    /**
     * <p>
     * Creates a new {@link PositionAnnotation} completely initialized. <b>Important:</b> Prefer creating instances
     * using the {@link PositionAnnotationFactory}.
     * </p>
     * 
     * @param value The text value of this {@link PositionAnnotation}.
     * @param startPosition The position of the first character of this {@link PositionAnnotation}.
     */
    public PositionAnnotation(String value, int startPosition) {
        super(startPosition, value, createIdentifier(value, startPosition));
    }

    private static String createIdentifier(String value, int startPosition) {
        return String.format("%s:%s", value, startPosition);
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

}
