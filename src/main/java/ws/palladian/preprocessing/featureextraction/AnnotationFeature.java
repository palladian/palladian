package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.model.features.Feature;

public class AnnotationFeature extends Feature<List<Annotation>> {

    public AnnotationFeature(String name) {
        super(name, new ArrayList<Annotation>());
    }

    public void add(Annotation annotation) {
        getValue().add(annotation);
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

    /**
     * Gives all {@link Annotation}s in the specified range.
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

}
