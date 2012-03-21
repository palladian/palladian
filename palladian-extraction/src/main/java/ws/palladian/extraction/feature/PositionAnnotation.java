package ws.palladian.extraction.feature;

import ws.palladian.extraction.PipelineDocument;

/**
 * <p>
 * An annotation which points to text fragments in a view of a {@link PipelineDocument}. The position indices are zero
 * based. The end is marked by the index of the first character not belonging to the {@code Annotation}.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 */
public final class PositionAnnotation extends Annotation {

    /**
     * <p>
     * The index of the first character of this {@code Annotation}.
     * </p>
     */
    private final int startPosition;
    /**
     * <p>
     * The index of the first character after the end of this {@code Annotation}.
     * </p>
     */
    private final int endPosition;

    /**
     * <p>
     * Creates a new {@code PositionAnnotation} completely initialized and pointing to the "originalContent" view of the
     * provided {@code PipelineDocument}.
     * </p>
     * 
     * @param document The document this {@code Annotation} points to.
     * @param startPosition The index of the first character of this {@code Annotation}.
     * @param endPosition The index of the first character after the end of this {@code Annotation}.
     */
    public PositionAnnotation(PipelineDocument document, int startPosition, int endPosition) {
        this(document, "originalContent", startPosition, endPosition);
    }

    /**
     * <p>
     * Creates a new {@code PositionAnnotation} completely initialized.
     * </p>
     * 
     * @param document The document this {@code Annotation} points to.
     * @param viewName The name of the view in the provided document holding the content the {@code Annotation} points
     *            to.
     * @param startPosition The index of the first character of this {@code Annotation}.
     * @param endPosition The index of the first character after the end of this {@code Annotation}.
     */
    public PositionAnnotation(PipelineDocument document, String viewName, int startPosition, int endPosition) {
        super(document, viewName);
        this.startPosition = startPosition;
        this.endPosition = endPosition;
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
    public String getValue() {
        String text = getDocument().getOriginalContent();
        // return text.substring(getStartPosition(), getEndPosition());

        // return a copy of the String, elsewise we will run into memory problems,
        // as the original String from the document might never get GC'ed, as long
        // as we keep its Tokens in memory
        // http://fishbowl.pastiche.org/2005/04/27/the_string_memory_gotcha/
        return new String(text.substring(getStartPosition(), getEndPosition()));
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
        if (getDocument() == null) {
            if (other.getDocument() != null) {
                return false;
            }
        } else if (getDocument().equals(other.getDocument())) {
            return false;
        }
        return true;
    }

}
