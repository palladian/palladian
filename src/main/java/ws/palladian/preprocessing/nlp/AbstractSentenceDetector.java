/**
 *
 */
package ws.palladian.preprocessing.nlp;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.featureextraction.Token;

/**
 * Abstract base class for all sentence detectors. Subclasses of this class provide an implementation to split texts
 * into sentences.
 * <p>
 * A call to a sentence detector using the default model provided by <tt>PALLADIAN</tt> might look like:
 * 
 * <pre>
 * {@code
 * AbstractSentenceDetector sentenceDetector = new ...();
 *    sentenceDetector.loadModel();
 *    sentenceDetector.detect("This is my sentence. This is another!");
 *    String[] sentences = sentenceDetector.getSentences();
 * }
 * </pre>
 * 
 * It will return an array containing the two sentences: "This is my sentence." and "This is another!".
 * <p>
 * You can reuse an instance of this class if you want to. Simply call {@link #detect(String)} or
 * {@link #detect(String, String)} on a new {@code String}, consisting out of multiple sentences.
 * 
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 */
public abstract class AbstractSentenceDetector implements PipelineProcessor {

    /**
     * 
     */
    private static final long serialVersionUID = -8764960870080954781L;

    /** the logger for this class */
    protected static final Logger LOGGER = Logger.getLogger(AbstractSentenceDetector.class);

    /** base model path */
    protected static final String MODEL_PATH = "data/models/";
    
    public final static String FEATURE_IDENTIFIER ="ws.palladian.features.sentence";

    /** holds the model. **/
    protected Object model;

    /** holds the name of the chunker. **/
    private String name;

    /** holds the sentences. **/
    protected Token[] sentences;

    /**
     * chunks a sentence and writes parts in @see {@link #chunks} and @see {@link #tokens}. Method returns
     * <code>this</code> instance of
     * AbstractSentenceDetector, to allow convenient concatenations of method
     * invocations, like: <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @param sentence
     */
    public abstract AbstractSentenceDetector detect(String text);

    protected final Object getModel() {
        return model;
    }

    /**
     * @return
     */
    protected final String getName() {
        return name;
    }

    /**
     * Provides the sentences extracted by the previous call to {@link #detect(String)} or
     * {@link #detect(String, String)}.
     * 
     * @return the extracted sentences.
     */
    public final Token[] getSentences() {
        return Arrays.copyOf(sentences, sentences.length);
    }
    
    /**
     * @param model
     */
    protected final void setModel(Object model) {
        this.model = model;
    }

    /**
     * Sets the name of this sentence detector. //TODO why does a sentence detector require a name.
     * 
     * @param name The new name of this detector.
     */
    protected final void setName(String name) {
        this.name = name;
    }

    /**
     * @param sentences
     *            the sentences to set
     */
    protected final void setSentences(Token[] sentences) {
        this.sentences = Arrays.copyOf(sentences, sentences.length);
    }
    
    @Override
    public void process(PipelineDocument document) {
        detect(document.getOriginalContent());
        Token[] sentences = getSentences();
        List<Token> sentencesList = Arrays.asList(sentences);
        Feature<List<Token>> sentencesFeature = new Feature<List<Token>>(FEATURE_IDENTIFIER, sentencesList);
        FeatureVector featureVector = document.getFeatureVector();
        featureVector.add(sentencesFeature);
    }

}
