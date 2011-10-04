package ws.palladian.preprocessing.nlp.ner.evaluation;

import ws.palladian.preprocessing.nlp.ner.Annotation;

/**
 * <p>This Annotation is one that is used for evaluation and can be checked whether it was tagged.</p>
 * 
 * @author David Urbansky
 * 
 */
public class EvaluationAnnotation extends Annotation {

    private static final long serialVersionUID = -543382211815689320L;
    
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
