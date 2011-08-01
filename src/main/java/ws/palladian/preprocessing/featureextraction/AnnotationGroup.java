package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.preprocessing.PipelineDocument;

/**
 * <p>
 * A group of <code>n</code> Annotations.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public class AnnotationGroup extends Annotation {

    /**
     * <p>
     * Separator, when a String representation of the contained {@link Annotation}s is requested.
     * </p>
     */
    private static final String TOKEN_SEPARATOR = " ";

    /**
     * <p>
     * List of annotations within this group.
     * </p>
     */
    private List<Annotation> annotations = new ArrayList<Annotation>();

    /**
     * <p>
     * Creates a new {@link AnnotationGroup} for the specified {@link PipelineDocument}. {@link Annotation}s can be
     * added to this group using the {@link #add(Annotation)} method.
     * </p>
     * 
     * @param document
     *            The document this {@link AnnotationGroup} points to.
     */
    public AnnotationGroup(PipelineDocument document) {
        super(document);
    }

    @Override
    public int getStartPosition() {
        int startPosition = -1;
        if (!annotations.isEmpty()) {
            startPosition = annotations.get(0).getStartPosition();
        }
        return startPosition;
    }

    @Override
    public int getEndPosition() {
        int endPosition = -1;
        if (!annotations.isEmpty()) {
            endPosition = annotations.get(annotations.size() - 1).getEndPosition();
        }
        return endPosition;
    }

    @Override
    public String getValue() {
        StringBuilder valueBuilder = new StringBuilder();
        for (Annotation annotation : annotations) {
            valueBuilder.append(annotation.getValue()).append(TOKEN_SEPARATOR);
        }
        valueBuilder.deleteCharAt(valueBuilder.length() - 1);
        return valueBuilder.toString();
    }

    /**
     * <p>
     * Add an {@link Annotation} to this group by appending it to the end of the existing Annotations.
     * </p>
     * 
     * @param annotation
     */
    public void add(Annotation annotation) {
        annotations.add(annotation);
    }

    /**
     * <p>
     * Return the list of Annotations within this group.
     * </p>
     * 
     * @return
     */
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AnnotationGroup [getTokens()=");
        builder.append(getAnnotations());
        builder.append(", getStartPosition()=");
        builder.append(getStartPosition());
        builder.append(", getEndPosition()=");
        builder.append(getEndPosition());
        builder.append(", getValue()=");
        builder.append(getValue());
        builder.append(", getFeatureVector()=");
        builder.append(getFeatureVector());
        builder.append("]");
        return builder.toString();
    }

}