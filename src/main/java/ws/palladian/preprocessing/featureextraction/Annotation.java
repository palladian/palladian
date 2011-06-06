package ws.palladian.preprocessing.featureextraction;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;

/**
 * Abstract super class defining an Annotation.
 * @author Philipp Katz
 */
public abstract class Annotation {
    
    private PipelineDocument document;
    private FeatureVector featureVector;
    
    public Annotation(PipelineDocument document) {
        this.document = document;
        featureVector = new FeatureVector();
    }
    
    public PipelineDocument getDocument() {
        return document;
    }

    /**
     * @return the startPosition
     */
    public abstract int getStartPosition();

    /**
     * @return the endPosition
     */
    public abstract int getEndPosition();

    public abstract String getValue();

    public FeatureVector getFeatureVector() {
        return featureVector;
    }

}