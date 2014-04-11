package ws.palladian.extraction.token;

import java.util.List;

import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * Abstract base class for tokenizer annotators.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class BaseTokenizer implements Tagger {
    
    public abstract List<Annotation> getAnnotations(String text);

}
