package ws.palladian.processing.features;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Default implementation for {@link Annotated} interface.
 * </p>
 * 
 * @author Philipp Katz
 */
public class Annotation implements Annotated {

    /** The position of the first character of this {@code Annotation}. */
    private final int startPosition;

    /** The {@link String} value marked by this annotation. */
    private final String value;

    /** The tag assigned to this annotation. */
    private final String tag;

    /**
     * <p>
     * Create a new {@link Annotation} at the given position, with the specified value and tag.
     * </p>
     * 
     * @param startPosition The start offset in the text, greater or equal zero.
     * @param value The value of the annotation, not <code>null</code> or empty.
     * @param tag An (optional) tag.
     */
    public Annotation(int startPosition, String value, String tag) {
        Validate.isTrue(startPosition >= 0, "startPosition cannot be negative.");
        Validate.notEmpty(value, "value must not be empty");
        this.startPosition = startPosition;
        this.value = value;
        this.tag = tag;
    }

    /**
     * <p>
     * Create a new {@link Annotation} by copying.
     * </p>
     * 
     * @param annotated The {@link Annotated} to copy, not <code>null</code>.
     */
    protected Annotation(Annotated annotated) {
        Validate.notNull(annotated, "annotated must not be null");
        this.startPosition = annotated.getStartPosition();
        this.value = annotated.getValue();
        this.tag = annotated.getTag();
    }

    @Override
    public final int compareTo(Annotated other) {
        return Integer.valueOf(this.startPosition).compareTo(other.getStartPosition());
    }

    @Override
    public final int getStartPosition() {
        return startPosition;
    }

    @Override
    public final int getEndPosition() {
        return startPosition + value.length();
    }

    @Override
    public final String getTag() {
        return tag;
    }

    @Override
    public final String getValue() {
        return value;
    }

    @Override
    public final boolean overlaps(Annotated other) {
        boolean overlaps = false;
        overlaps |= startPosition <= other.getStartPosition() && getEndPosition() >= other.getStartPosition();
        overlaps |= startPosition <= other.getEndPosition() && getEndPosition() >= other.getStartPosition();
        return overlaps;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getEndPosition();
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
        Annotated other = (Annotated)obj;
        if (getEndPosition() != other.getEndPosition()) {
            return false;
        }
        if (startPosition != other.getStartPosition()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Annotation [value=");
        builder.append(getValue());
        builder.append(", startPosition=");
        builder.append(getStartPosition());
        builder.append(", endPosition=");
        builder.append(getEndPosition());
        builder.append("]");
        return builder.toString();
    }

}
