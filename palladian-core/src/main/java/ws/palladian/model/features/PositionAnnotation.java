package ws.palladian.model.features;

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
     * Creates a new {@code PositionAnnotation} completely initialized and pointing to the "originalContent" view of the
     * provided {@code PipelineDocument}.
     * </p>
     * 
     * @param document The document this {@code Annotation} points to.
     * @param startPosition The position of the first character of this {@code Annotation}.
     * @param endPosition The position of the first character after the end of this {@code Annotation}.
     * @param index The running index of this {@link Annotation}.
     */
    public PositionAnnotation(PipelineDocument document, int startPosition, int endPosition, int index) {
        this(document, "originalContent", startPosition, endPosition, index);
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
    public PositionAnnotation(PipelineDocument document, int startPosition, int endPosition) {
        this(document, "originalContent", startPosition, endPosition, -1);
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
     * @param index The running index of this {@link Annotation}.
     * @param value The text value of this {@link Annotation}.
     */
    public PositionAnnotation(PipelineDocument document, int startPosition, int endPosition, int index, String value) {
        this(document, "originalContent", startPosition, endPosition, index, value);
    }

    /**
     * <p>
     * Creates a new {@code PositionAnnotation} completely initialized.
     * </p>
     * 
     * @param document The document this {@code Annotation} points to.
     * @param viewName The name of the view in the provided document holding the content the {@code Annotation} points
     *            to.
     * @param startPosition The position of the first character of this {@code Annotation}.
     * @param endPosition The position of the first character after the end of this {@code Annotation}.
     * @param index The running index of this {@link Annotation}.
     */
    public PositionAnnotation(PipelineDocument document, String viewName, int startPosition, int endPosition, int index) {
        // return a copy of the String, elsewise we will run into memory problems,
        // as the original String from the document might never get GC'ed, as long
        // as we keep its Tokens in memory
        // http://fishbowl.pastiche.org/2005/04/27/the_string_memory_gotcha/
        // this(document, viewName, startPosition, endPosition, index, new String(document.getOriginalContent().substring(startPosition, endPosition)));
        
        // after further consideration, I think this does not make sense; an Annotation is conceptually inherently tied
        // to its document, so we point to the document's string. If there should be a use case of keeping position
        // annotations without the document, we might think about some "detach" method here, but currently I don't see
        // the need.
        this(document, viewName, startPosition, endPosition, index, document.getOriginalContent().substring(startPosition, endPosition));
    }

    /**
     * <p>
     * Creates a new {@code PositionAnnotation} completely initialized.
     * </p>
     * 
     * @param document The document this {@code Annotation} points to.
     * @param viewName The name of the view in the provided document holding the content the {@code Annotation} points
     *            to.
     * @param startPosition The position of the first character of this {@code Annotation}.
     * @param endPosition The position of the first character after the end of this {@code Annotation}.
     * @param index The running index of this {@link Annotation}.
     * @param value The text value of this {@link Annotation}.
     */
    public PositionAnnotation(PipelineDocument document, String viewName, int startPosition, int endPosition,
            int index, String value) {
        super(document, viewName);
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.index = index;
        this.value = value;
    }

    /**
     * <p>
     * Creates a new {@code PositionAnnotation} completely initialized.
     * </p>
     * 
     * @param document The document this {@code Annotation} points to.
     * @param viewName The name of the view in the provided document holding the content the {@code Annotation} points
     *            to.
     * @param startPosition The position of the first character of this {@code Annotation}.
     * @param endPosition The position of the first character after the end of this {@code Annotation}.
     * @param value The text value of this {@link Annotation}.
     */
    public PositionAnnotation(PipelineDocument document, String viewName, int startPosition, int endPosition) {
        this(document, viewName, startPosition, endPosition, -1);
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
        } else if (getDocument().equals(other.getDocument())) {
            return false;
        }
        return true;
    }

}
