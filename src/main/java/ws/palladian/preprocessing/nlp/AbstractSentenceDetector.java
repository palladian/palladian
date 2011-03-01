/**
 *
 */
package ws.palladian.preprocessing.nlp;

import java.util.Arrays;

import org.apache.log4j.Logger;

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
public abstract class AbstractSentenceDetector {

    /** the logger for this class */
    protected static final Logger LOGGER = Logger.getLogger(AbstractSentenceDetector.class);

    /** base model path */
    protected static final String MODEL_PATH = "data/models/";

    /** holds the model. **/
    protected Object model;

    /** holds the name of the chunker. **/
    protected String name;

    /** holds the sentences. **/
    protected String[] sentences;

    /**
     * chunks a sentence and writes parts in @see {@link #chunks} and @see {@link #tokens}. Method returns
     * <code>this</code> instance of
     * AbstractSentenceDetector, to allow convenient concatenations of method
     * invocations, like: <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @param sentence
     */
    public abstract AbstractSentenceDetector detect(String text);

    /**
     * Chunks a sentence with given model file path.
     * Method returns <code>this</code> instance of AbstractSentenceDetector, to allow
     * convenient concatenations of method invocations, like:
     * <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @param text The text to chunk into sentences.
     * @param modelFilePath The model file path pointing to the model containing information on how to chunk a text into
     */
    public abstract AbstractSentenceDetector detect(String text, String modelFilePath);

    /**
     * @return
     */
    public final Object getModel() {
        return model;
    }

    /**
     * @return
     */
    public final String getName() {
        return name;
    }

    /**
     * Provides the sentences extracted by the previous call to {@link #detect(String)} or
     * {@link #detect(String, String)}.
     * 
     * @return the extracted sentences.
     */
    public final String[] getSentences() {
        return Arrays.copyOf(sentences, sentences.length);
    }

    /**
     * Loads a default chunker model into the chunker. This method returns <code>this</code> instance of
     * AbstractSentenceDetector, to allow
     * convenient concatenations of method invocations, like:
     * <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @return The current sentence detector instance to allow for chaining of methods.
     */
    public abstract AbstractSentenceDetector loadModel();

    /**
     * loads the chunker model into the chunker. Method returns <code>this</code> instance of AbstractSentenceDetector,
     * to allow
     * convenient concatenations of method invocations, like:
     * <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @param modelFilePath
     * @return
     */
    public abstract AbstractSentenceDetector loadModel(String modelFilePath);

    /**
     * @param model
     */
    public final void setModel(Object model) {
        this.model = model;
    }

    /**
     * @param name
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * @param sentences
     *            the sentences to set
     */
    public final void setSentences(String[] sentences) {
        this.sentences = Arrays.copyOf(sentences, sentences.length);
    }

}
