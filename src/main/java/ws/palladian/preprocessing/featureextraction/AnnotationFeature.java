package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.model.features.Feature;
import ws.palladian.preprocessing.PipelineDocument;

public class AnnotationFeature extends Feature<List<Annotation>> {

    private PipelineDocument document;

    public AnnotationFeature(String name, PipelineDocument document) {
        super(name, new ArrayList<Annotation>());
        this.document = document;
    }

    public void addToken(Annotation annotation) {
        getValue().add(annotation);
        annotation.setDocument(getDocument());
    }

    /**
     * @return the document
     */
    public PipelineDocument getDocument() {
        return document;
    }

    /**
     * @param document the document to set
     */
    public void setDocument(PipelineDocument document) {
        this.document = document;
    }

    public String toStringList() {
        StringBuilder sb = new StringBuilder();
        List<Annotation> annotations = getValue();
        for (Annotation annotation : annotations) {
            sb.append(annotation).append("\n");
        }
        return sb.toString();
    }
    
    public List<Annotation> getTokens(int startPosition, int endPosition) {
        List<Annotation> result = new ArrayList<Annotation>();
        for (Annotation current : getValue()) {
            if (current.getStartPosition() >= startPosition && current.getEndPosition() <= endPosition) {
                result.add(current);
            }
        }
        return result;
    }

}
