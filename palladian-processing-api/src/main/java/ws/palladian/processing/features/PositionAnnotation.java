package ws.palladian.processing.features;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.PipelineDocument;

/**
 * <p>
 * An annotation which points to text fragments in a view of a {@link PipelineDocument}. The position indices are zero
 * based. The end is marked by the index of the first character not belonging to the {@code Annotation}.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 3.0
 * @since 0.1.7
 */
// TODO rename to TextAnnotation
public final class PositionAnnotation extends Annotation<String> {

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
     * The running index of this {@link Annotation}.
     * </p>
     */
    private final int index;

    /**
     * <p>
     * The text value of this {@link Annotation}.
     * </p>
     */
    private String value;

    /**
     * <p>
     * Creates a new {@code PositionAnnotation} completely initialized.
     * </p>
     * 
     * @param document The document this {@code Annotation} points to.
     * @param startPosition The position of the first character of this {@code Annotation}.
     * @param endPosition The position of the first character after the end of this {@code Annotation}.
     * @param index The running index of this {@link Annotation}.
     */
    public <F> PositionAnnotation(PipelineDocument<String> document, int startPosition, int endPosition, int index) {
        this(document, startPosition, endPosition, index, null);
    }

    /**
     * <p>
     * Creates a new {@code PositionAnnotation} completely initialized and pointing to the "originalContent" view of the
     * provided {@code PipelineDocument}.
     * </p>
     * 
     * @param document The document this {@code Annotation} points to.
     * @param startPosition The position of the first character of this {@code Annotation}.
     * @param endPosition The position of the first character after the end of this {@code Annotation}.
     */
    public <F> PositionAnnotation(PipelineDocument<String> document, int startPosition, int endPosition) {
        this(document, startPosition, endPosition, -1);
    }

    public <F> PositionAnnotation(PipelineDocument<String> document, int startPosition, int endPosition, String value) {
        this(document, startPosition, endPosition, -1, value);
    }

    /**
     * <p>
     * Creates a new {@code PositionAnnotation} completely initialized and pointing to the "originalContent" view of the
     * provided {@code PipelineDocument}.
     * </p>
     * 
     * @param document The document this {@code Annotation} points to, not <code>null</code>.
     * @param startPosition The position of the first character of this {@code Annotation}.
     * @param endPosition The position of the first character after the end of this {@code Annotation}.
     * @param index The running index of this {@link Annotation}.
     * @param value The text value of this {@link Annotation}.
     */
    public <F> PositionAnnotation(PipelineDocument<String> document, int startPosition, int endPosition, int index,
            String value) {
        super(document);
        
        Validate.notNull(document, "document must not be null.");
        Validate.isTrue(startPosition >= 0, "startPosition cannot be negative.");
        Validate.isTrue(endPosition > startPosition, "endPosition must be greater than startPosition.");

        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.index = index;
        this.value = value;
    }

    @Override
    public Integer getStartPosition() {
        return startPosition;
    }

    @Override
    public Integer getEndPosition() {
        return endPosition;
    }

    @Override
    public Integer getIndex() {
        return index;
    }

    @Override
    public String getValue() {
    	if (value == null) {
    		value = getDocument().getContent().substring(startPosition, endPosition);
    	}
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
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
        builder.append(", index=");
        builder.append(getIndex());
        builder.append(", featureVector=");
        builder.append(getFeatureVector());
        builder.append("]");
        return builder.toString();
    }

    //
    // Attention: do not auto-generate the following methods,
    // they have been manually changed to consider the super#getDocument()
    //

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + endPosition;
        result = prime * result + startPosition;
        result = prime * result + index;
        result = prime * result + ((getDocument() == null) ? 0 : getDocument().hashCode());
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
        if (getDocument() == null) {
            if (other.getDocument() != null) {
                return false;
            }
        } else if (!getDocument().equals(other.getDocument())) {
            return false;
        }
        return true;
    }

}
