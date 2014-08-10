package ws.palladian.extraction.sentence;

import java.util.List;

import ws.palladian.core.Annotation;
import ws.palladian.core.Tagger;

/**
 * <p>
 * Marker interface for all sentence detectors.
 * </p>
 *
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public interface SentenceDetector extends Tagger {

    @Override
    List<Annotation> getAnnotations(String text);

}
