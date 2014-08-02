package ws.palladian.extraction.sentence;

import java.util.List;

import ws.palladian.core.Annotation;
import ws.palladian.core.Tagger;

/**
 * <p>
 * Abstract base class for all sentence detectors.
 * </p>
 * 
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public abstract class AbstractSentenceDetector implements Tagger {

    public abstract List<Annotation> getAnnotations(String text);

}
