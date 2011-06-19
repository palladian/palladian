/**
 *
 */
package ws.palladian.preprocessing.nlp;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.AbstractPipelineProcessor;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.featureextraction.Annotation;

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
 *    Annotation[] sentences = sentenceDetector.getSentences();
 *    String firstSentence = sentences[0].getValue();
 * }
 * </pre>
 * 
 * It will return an array containing annotations for the two sentences: "This is my sentence." and "This is another!".
 * Annotations are pointers into a {@link PipelineDocument} created from the input String, marking the start index and
 * end index of the extracted sentence. To access the value just call {@link Annotation#getValue()}.
 * </p>
 * <p>
 * You can reuse an instance of this class if you want to. Simply call {@link #detect(String)} or
 * {@link #detect(String, String)} on a new {@code String}, consisting out of multiple sentences.
 * </p>
 * 
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.0
 */
public abstract class AbstractSentenceDetector extends AbstractPipelineProcessor {

    /**
     * <p>
     * Used for serializing and deserializing this object. Do change this value if the objects attriutes change and thus
     * old serialized version are no longer compatible.
     * </p>
     */
    private static final long serialVersionUID = -8764960870080954781L;

    /** the logger for this class */
    protected static final Logger LOGGER = Logger.getLogger(AbstractSentenceDetector.class);

    /** base model path */
    protected static final String MODEL_PATH = "data/models/";

    /**
     * <p>
     * The world wide unique identifier of the {@link Feature}s created by this annotator.
     * </p>
     */
    public static final String FEATURE_IDENTIFIER = "ws.palladian.features.sentence";

    /** holds the model. **/
    private Object model;

    /** holds the sentences. **/
    private Annotation[] sentences;

    /**
     * <p>
     * The name of the view the algorithm currently works on. This needs to be available to subclasses to create valid
     * annotations.
     * </p>
     * 
     * @see #detect(String)
     * @see #processDocument(PipelineDocument)
     */
    private String currentViewName;

    /**
     * <p>
     * Creates a new completely intialized sentence detector working on the provided input views if used as
     * {@code PipelineProcessor}.
     * </p>
     * 
     * @param inputViewNames The set of input view names for which sentences are detected.
     */
    public AbstractSentenceDetector(String[] inputViewNames) {
        super(inputViewNames);
    }

    /**
     * <p>
     * Creates anew completely initialized sentence detector working on the "originalContent" view if used as
     * {@code PipelineProcessor}.
     * </p>
     */
    public AbstractSentenceDetector() {
        super();
    }

    /**
     * <p>
     * Chunks a text into sentences. Method returns <code>this</code> instance of AbstractSentenceDetector, to allow
     * convenient concatenations of method invocations, like:
     * {@code new OpenNLPSentenceDetector().detect(...).getSentences();}
     * </p>
     * 
     * @param text The text to detect sentences on.
     * @return {@code this} object for convenient chaining of method calls.
     */
    public abstract AbstractSentenceDetector detect(String text);

    /**
     * <p>
     * Provides the trained model used to chunk a text into sentences. This is usually created from a text where
     * existing sentences are annotated. For further information look up literature about machine learning and natural
     * language processing.
     * </p>
     * 
     * @return The model currently used for chunking sentences.
     */
    protected final Object getModel() {
        return model;
    }

    /**
     * <p>
     * Provides the sentences extracted by the previous call to {@link #detect(String)} or
     * {@link #detect(String, String)}.
     * </p>
     * 
     * @return the extracted sentences.
     */
    public final Annotation[] getSentences() {
        return Arrays.copyOf(sentences, sentences.length);
    }

    /**
     * <p>
     * Resets and overwrites the model used by this sentence detector.
     * </p>
     * 
     * @param model The new model for this sentence detector.
     */
    protected final void setModel(Object model) {
        this.model = model;
    }

    /**
     * <p>
     * Resets ond overwrites the last extraction result with the current one in the form of sentece {@code Annotation}s.
     * </p>
     * 
     * @param sentences
     *            Extracted sentence {@code Annotation}
     */
    protected final void setSentences(Annotation[] sentences) {
        this.sentences = Arrays.copyOf(sentences, sentences.length);
    }

    @Override
    public final void processDocument(PipelineDocument document) {
        for (String inputViewName : getInputViewNames()) {
            setCurrentViewName(inputViewName);

            detect(document.getView(inputViewName));
            Annotation[] sentences = getSentences();
            List<Annotation> sentencesList = Arrays.asList(sentences);
            Feature<List<Annotation>> sentencesFeature = new Feature<List<Annotation>>(FEATURE_IDENTIFIER,
                    sentencesList);
            FeatureVector featureVector = document.getFeatureVector();
            featureVector.add(sentencesFeature);
        }
    }

    /**
     * <p>
     * Provides the name of the currently processed view from the input {@code PipelineDocument}. This is necessary so
     * subclasses are able to create {@code Annotation}s for this view.
     * </p>
     * 
     * @return The name of the currently processed view.
     */
    protected final String getCurrentViewName() {
        return this.currentViewName;
    }

    /**
     * <p>
     * Resets and overwrites the name of the currently processed view. This should be called by
     * {@link #processDocument(PipelineDocument)} each time the current view changes.
     * </p>
     * 
     * @param currentViewName The name of the currently processed view.
     */
    private void setCurrentViewName(String currentViewName) {
        this.currentViewName = currentViewName;
    }

}
