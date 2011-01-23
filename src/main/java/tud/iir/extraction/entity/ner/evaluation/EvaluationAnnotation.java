package tud.iir.extraction.entity.ner.evaluation;

import tud.iir.extraction.entity.ner.Annotation;

/**
 * This Annotation is one that is used for evaluation and can be checked whether it was tagged.
 * 
 * @author David Urbansky
 * 
 */
public class EvaluationAnnotation extends Annotation {

    /** If true, the NER has found the annotation. */
    private boolean tagged = false;

    public EvaluationAnnotation(Annotation annotation) {
        super(annotation);

    }

    public void setTagged(boolean tagged) {
        this.tagged = tagged;
    }

    public boolean isTagged() {
        return tagged;
    }

}
