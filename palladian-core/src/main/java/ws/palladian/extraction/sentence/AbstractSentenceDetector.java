package ws.palladian.extraction.sentence;

import java.util.List;

import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotation;
import edu.stanford.nlp.ling.CoreAnnotations.PositionAnnotation;

/**
 * <p>
 * Abstract base class for all sentence detectors. Subclasses of this class provide an implementation to split texts
 * into sentences.
 * </p>
 * <p>
 * A call to a sentence detector might look like:
 * 
 * <pre>
 * {@code
 * AbstractSentenceDetector sentenceDetector = new ...();
 *    sentenceDetector.detect("This is my sentence. This is another!");
 *    PositionAnnotation[] sentences = sentenceDetector.getSentences();
 *    String firstSentence = sentences[0].getValue();
 * }
 * </pre>
 * 
 * It will return an array containing annotations for the two sentences: "This is my sentence." and "This is another!".
 * Annotations are pointers into a {@link PipelineDocument} created from the input String, marking the start index and
 * end index of the extracted sentence. To access the value just call {@link PositionAnnotation#getValue()}.
 * </p>
 * <p>
 * You can reuse an instance of this class if you want to. Simply call {@link #detect(String)} or
 * {@link #detect(String, String)} on a new {@code String}, consisting out of multiple sentences.
 * </p>
 * 
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public abstract class AbstractSentenceDetector implements Tagger {

    public abstract List<Annotation> getAnnotations(String text);

}
