/**
 * 
 */
package tud.iir.preprocessing.nlp;

import org.apache.log4j.Logger;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public abstract class AbstractSentenceDetector {

    /** the logger for this class */
    protected static final Logger LOGGER = Logger
            .getLogger(AbstractSentenceDetector.class);

    /** base model path */
    protected static final String MODEL_PATH = "data/models/";

    /** holds the model. **/
    private Object model;

    /** holds the name of the chunker. **/
    private String name;

    /** holds the sentences. **/
    private String[] sentences;

    /**
     * loads the chunker model into the chunker. Method returns
     * <code>this</code> instance of AbstractSentenceDetector, to allow
     * convenient concatenations of method invocations, like:
     * <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @param configModelFilePath
     * @return
     */
    public abstract AbstractSentenceDetector loadModel(
            String configModelFilePath);

    /**
     * loads the default chunker model into the chunker.Method returns
     * <code>this</code> instance of AbstractSentenceDetector, to allow
     * convenient concatenations of method invocations, like:
     * <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @return
     */
    public abstract AbstractSentenceDetector loadDefaultModel();

    /**
     * chunks a sentence and writes parts in @see {@link #chunks} and @see
     * {@link #tokens}. Method returns <code>this</code> instance of
     * AbstractSentenceDetector, to allow convenient concatenations of method
     * invocations, like:
     * <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @param sentence
     */
    public abstract AbstractSentenceDetector detect(String text);

    /**
     * chunks a senntence with given model file path and writes it into @see
     * {@link #chunks} and @see {@link #tokens}. Method returns
     * <code>this</code> instance of AbstractSentenceDetector, to allow
     * convenient concatenations of method invocations, like:
     * <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @param sentence
     * @param configModelFilePath
     */
    public abstract AbstractSentenceDetector detect(String text,
            String configModelFilePath);

    /**
     * @return
     */
    public final Object getModel() {
        return model;
    }

    /**
     * @param model
     */
    public final void setModel(Object model) {
        this.model = model;
    }

    /**
     * @return
     */
    public final String getName() {
        return name;
    }

    /**
     * @param name
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * @return the sentences
     */
    public final String[] getSentences() {
        return sentences;
    }

    /**
     * @param sentences
     *            the sentences to set
     */
    public final void setSentences(String[] sentences) {
        this.sentences = sentences;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final LingPipeSentenceDetector lpsd = new LingPipeSentenceDetector();
        lpsd.loadDefaultModel();
        lpsd.detect("This is my sentence. This is another!");
        CollectionHelper.print(lpsd.getSentences());

        stopWatch.stop();
        LOGGER.info("time elapsed: " + stopWatch.getElapsedTimeString());

    }

}
