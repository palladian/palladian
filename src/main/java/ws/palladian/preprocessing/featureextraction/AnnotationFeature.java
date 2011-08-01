package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;

/**
 * <p>
 * An {@link AnnotationFeature} is a feature consisting of a List of {@link Annotation}s. Annotations are pointers to
 * parts of text in a {@link PipelineDocument} which can be characterized by a {@link FeatureVector}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class AnnotationFeature extends Feature<List<Annotation>> {

    /**
     * Create a new {@link AnnotationFeature}.
     * 
     * @param name A unique Identifier for the AnnotationFeature.
     */
    public AnnotationFeature(String name) {
        super(name, new ArrayList<Annotation>());
    }

    /**
     * Add an {@link Annotation} to this Feature.
     * 
     * @param annotation
     */
    public void add(Annotation annotation) {
        getValue().add(annotation);
    }

    /**
     * Gives all {@link Annotation}s within the specified range.
     * 
     * @param startPosition
     * @param endPosition
     * @return
     */
    public List<Annotation> getAnnotations(int startPosition, int endPosition) {
        List<Annotation> result = new ArrayList<Annotation>();
        for (Annotation current : getValue()) {
            if (current.getStartPosition() >= startPosition && current.getEndPosition() <= endPosition) {
                result.add(current);
            }
        }
        return result;
    }

    /**
     * Return a human-readable list of all contained {@link Annotation}s.
     * 
     * @return
     */
    public String toStringList() {
        StringBuilder sb = new StringBuilder();
        List<Annotation> annotations = getValue();
        for (Annotation annotation : annotations) {
            sb.append(annotation).append("\n");
        }
        return sb.toString();
    }

}
