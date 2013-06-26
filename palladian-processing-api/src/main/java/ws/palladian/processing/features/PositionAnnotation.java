package ws.palladian.processing.features;

import org.apache.commons.lang3.Validate;

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
public class PositionAnnotation implements Classifiable, Annotated, Feature<String> {

    /**
     * <p>
     * The position of the first character of this {@code Annotation}.
     * </p>
     */
    private final int startPosition;
    /**
     * <p>
     * The position of the first character after the end of this {@code Annotation}.
     * </p>
     */
    private final int endPosition;

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
     * The {@link String} value marked by this annotation.
     * </p>
     */
    private String value;

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
    public PositionAnnotation(String value, int startPosition, int endPosition) {
        Validate.isTrue(startPosition >= 0, "startPosition cannot be negative.");
        Validate.isTrue(endPosition > startPosition, "endPosition must be greater than startPosition.");
        Validate.notEmpty(value);

        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.featureVector = null;
        this.value = value;
    }

    /**
     * <p>
     * Create a new {@link PositionAnnotation} by copying an existing one.
     * </p>
     * 
     * @param annotation The {@link PositionAnnotation} to copy.
     */
    public PositionAnnotation(PositionAnnotation annotation) {
        Validate.isTrue(annotation.getStartPosition() >= 0, "startPosition cannot be negative.");
        Validate.isTrue(annotation.getEndPosition() > annotation.getStartPosition(),
                "endPosition must be greater than startPosition.");

        this.startPosition = annotation.getStartPosition();
        this.endPosition = annotation.getEndPosition();
        this.featureVector = null;
        this.value = annotation.value;
    }

    @Override
    public int getStartPosition() {
        return startPosition;
    }

    @Override
    public int getEndPosition() {
        return endPosition;
    }

    @Override
    public String getTag() {
        return getName();
    }

    @Override
    public FeatureVector getFeatureVector() {
        if (featureVector == null) {
            featureVector = new FeatureVector();
        }
        return featureVector;
    }

    @Override
    // FIXME this needs to go in parent -> duplicate of NerHelper
    public boolean overlaps(Annotated annotated) {
        if (getStartPosition() <= annotated.getStartPosition() && getEndPosition() >= annotated.getStartPosition()
                || getStartPosition() <= annotated.getEndPosition() && getEndPosition() >= annotated.getStartPosition()) {
            return true;
        }
        return false;
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + endPosition;
        result = prime * result + startPosition;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PositionAnnotation other = (PositionAnnotation)obj;
        if (endPosition != other.endPosition) {
            return false;
        }
        if (startPosition != other.startPosition) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Annotated o) {
        return Integer.valueOf(this.startPosition).compareTo(o.getStartPosition());
    }

    @Override
    public String getName() {
        return value + startPosition + endPosition;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

}
