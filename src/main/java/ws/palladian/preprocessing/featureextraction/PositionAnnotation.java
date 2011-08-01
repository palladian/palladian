package ws.palladian.preprocessing.featureextraction;

import ws.palladian.preprocessing.PipelineDocument;

/**
 * <p>
 * An annotation which points to text fragments in a view of a {@link PipelineDocument}. The position indices are zero
 * based. The end is marked by the index of the first character not belonging to the {@code Annotation}.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @since 0.8
 * @version 1.0
 */
public final class PositionAnnotation extends Annotation {

    /**
     * <p>
     * The index of the first character of this {@code Annotation}.
     * </p>
     */
    private int startPosition;
    /**
     * <p>
     * The index of the first character after the end of this {@code Annotation}.
     * </p>
     */
    private int endPosition;

    /**
     * <p>
     * Creates a new {@code PositionAnnotation} completely initialized and pointing to the "originalContent" view of
     * the provided {@code PipelineDocument}.
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
    public int getStartPosition() {
        return startPosition;
    }

    @Override
    public int getEndPosition() {
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
}
