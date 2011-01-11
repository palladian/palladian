/**
 * 
 */
package tud.iir.extraction.event;

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
     * loads the chunker model into the chunker.
     * 
     * @param configModelFilePath
     * @return
     */
    public abstract boolean loadModel(String configModelFilePath);

    /**
     * loads the default chunker model into the chunker.
     * 
     * @return
     */
    public abstract boolean loadModel();

    /**
     * chunks a sentence and writes parts in @see {@link #chunks} and @see
     * {@link #tokens}.
     * 
     * @param sentence
     */
    public abstract void detect(String text);

    /**
     * chunks a senntence with given model file path and writes it into @see
     * {@link #chunks} and @see {@link #tokens}.
     * 
     * @param sentence
     * @param configModelFilePath
     */
    public abstract void detect(String text, String configModelFilePath);

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
        lpsd.loadModel();
        lpsd.detect("This is my sentence. This is another!");
        CollectionHelper.print(lpsd.getSentences());

        stopWatch.stop();
        LOGGER.info("time elapsed: " + stopWatch.getElapsedTimeString());

    }

}
