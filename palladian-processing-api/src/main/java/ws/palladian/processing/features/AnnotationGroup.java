package ws.palladian.processing.features;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.processing.PipelineDocument;

/**
 * <p>
 * A group of <code>n</code> Annotations.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class AnnotationGroup extends Annotation {

    /**
     * <p>
     * Separator, when a String representation of the contained {@link Annotation}s is requested.
     * </p>
     */
    private static final char TOKEN_SEPARATOR = ' ';

    /**
     * <p>
     * List of annotations within this group.
     * </p>
     */
    private List<Annotation> annotations = new ArrayList<Annotation>();

    /**
     * <p>
     * The value of this {@link AnnotationGroup}, in case it is overridden manually.
     * </p>
     */
    private String value;

    /**
     * <p>
     * Creates a new {@link AnnotationGroup} for the specified {@link PipelineDocument}. {@link Annotation}s can be
     * added to this group using the {@link #add(Annotation)} method.
     * </p>
     * 
     * @param document
     *            The document this {@link AnnotationGroup} points to.
     */
    public AnnotationGroup(PipelineDocument<?> document) {
        super(document);
        value = null;
    }

    @Override
    public Integer getStartPosition() {
        int startPosition = -1;
        if (!annotations.isEmpty()) {
            startPosition = annotations.get(0).getStartPosition();
        }
        return startPosition;
    }

    @Override
    public Integer getEndPosition() {
        int endPosition = -1;
        if (!annotations.isEmpty()) {
            endPosition = annotations.get(annotations.size() - 1).getEndPosition();
        }
        return endPosition;
    }

    @Override
    public Integer getIndex() {
        int index = -1;
        if (!annotations.isEmpty()) {
            index = annotations.get(0).getIndex();
        }
        return index;
    }

    @Override
    public String getValue() {
        if (value == null) {
            StringBuilder valueBuilder = new StringBuilder();
            for (int i = 0; i < annotations.size(); i++) {
                if (i > 0) {
                    valueBuilder.append(TOKEN_SEPARATOR);
                }
                valueBuilder.append(annotations.get(i).getValue());
            }
            value = valueBuilder.toString();
        }
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
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
        builder.append("AnnotationGroup [value=");
        builder.append(getValue());
        builder.append(", startPosition=");
        builder.append(getStartPosition());
        builder.append(", endPosition=");
        builder.append(getEndPosition());
        builder.append(", index=");
        builder.append(getIndex());
        builder.append(", featureVector()=");
        builder.append(getFeatureVector());
        // builder.append(", annotations=");
        // builder.append(getAnnotations());
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
        result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
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
        AnnotationGroup other = (AnnotationGroup)obj;
        if (annotations == null) {
            if (other.annotations != null) {
                return false;
            }
        } else if (!annotations.equals(other.annotations)) {
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