package tud.iir.extraction.entity.ner.evaluation;

import tud.iir.extraction.entity.ner.Annotation;

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
