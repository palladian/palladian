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

    private static final String TOKEN_SEPARATOR = " ";
    private List<Annotation> annotations = new ArrayList<Annotation>();

    public AnnotationGroup(PipelineDocument document) {
        super(document, -1, -1);
    }

    public void add(Annotation annotation) {
        annotations.add(annotation);
        if (getStartPosition() == -1) {
            setStartPosition(annotation.getStartPosition());
        }
        setEndPosition(annotation.getEndPosition());

    }

    public List<Annotation> getTokens() {
        return annotations;
    }

    @Override
    public String getValue() {
        StringBuilder sb = new StringBuilder();
        for (Annotation annotation : annotations) {
            sb.append(annotation.getValue()).append(TOKEN_SEPARATOR);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AnnotationGroup [getTokens()=");
        builder.append(getTokens());
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