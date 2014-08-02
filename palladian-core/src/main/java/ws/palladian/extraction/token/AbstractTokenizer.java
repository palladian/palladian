package ws.palladian.extraction.token;

import java.util.List;

import ws.palladian.core.Annotation;
import ws.palladian.core.Tagger;

/**
 * <p>
 * Abstract base class for tokenizer annotators.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractTokenizer implements Tagger {
    
    public abstract List<Annotation> getAnnotations(String text);

}
