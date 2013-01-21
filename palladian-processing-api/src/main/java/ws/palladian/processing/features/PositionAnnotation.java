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
 * @version 3.0
 * @since 0.1.7
 */
// TODO rename to TextAnnotation
public class PositionAnnotation extends NominalFeature implements Classifiable, Comparable<PositionAnnotation>,
        Annotated {

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

    /**
     * <p>
     * The running index of this {@link PositionAnnotation}.
     * </p>
     */
    private final int index;

    private final FeatureVector featureVector = new FeatureVector();

    /**
     * <p>
     * Creates a new {@link PositionAnnotation} completely initialized. <b>Important:</b> Prefer creating instances
     * using the {@link PositionAnnotationFactory}.
     * </p>
     * 
     * @param name A unique identifier for this {@link PositionAnnotation}, e.g. "token".
     * @param startPosition The position of the first character of this {@link PositionAnnotation}.
     * @param endPosition The position of the first character after the end of this {@link PositionAnnotation}.
     * @param index The running index of this {@link PositionAnnotation}.
     * @param value The text value of this {@link PositionAnnotation}.
     */
    public PositionAnnotation(String name, int startPosition, int endPosition, int index, String value) {
        super(name, value);
        Validate.isTrue(startPosition >= 0, "startPosition cannot be negative.");
        Validate.isTrue(endPosition > startPosition, "endPosition must be greater than startPosition.");

        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.index = index;
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.processing.features.Annotated#getStartPosition()
     */
    @Override
    public int getStartPosition() {
        return startPosition;
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.processing.features.Annotated#getEndPosition()
     */
    @Override
    public int getEndPosition() {
        return endPosition;
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.processing.features.Annotated#getIndex()
     */
    @Override
    public int getIndex() {
        return index;
    }
    
    /*
     * (non-Javadoc)
     * @see ws.palladian.processing.features.Annotated#getTag()
     */
    @Override
    public String getTag() {
        return getName();
    }


    @Override
    public FeatureVector getFeatureVector() {
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
        builder.append(", index=");
        builder.append(getIndex());
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
        result = prime * result + index;
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
        if (index != other.index) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(PositionAnnotation o) {
        return Integer.valueOf(this.startPosition).compareTo(o.startPosition);
    }

}
