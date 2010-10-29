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
            .getLogger(PhraseChunker.class);

    /** base model path */
    protected static final String MODEL_PATH = "data/models/";

    /** model for opennlp sentence detection */
    protected static final String MD_SBD_ONLP = MODEL_PATH
            + "opennlp/sentdetect/EnglishSD.bin.gz";

    /** holds the model. **/
    private Object model;

    /** holds the name of the chunker. **/
    private String name;

    /** holds the sentences. **/
    private String[] sentences;

    /**
     * loads the chunker model into the chunker
     * 
     * @param configModelFilePath
     * @return
     */
    public abstract boolean loadModel(String configModelFilePath);

    /**
     * loads the default chunker model into the chunker
     * 
     * @return
     */
    public abstract boolean loadModel();

    /**
     * chunks a sentence and writes parts in @see {@link #chunks} and @see
     * {@link #tokens}
     * 
     * @param sentence
     */
    public abstract void detect(String text);

    /**
     * chunks a senntence with given model file path and writes it into @see
     * {@link #chunks} and @see {@link #tokens}
     * 
     * @param sentence
     * @param configModelFilePath
     */
    public abstract void detect(String text, String configModelFilePath);

    public Object getModel() {
        return model;
    }

    public void setModel(Object model) {
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the sentences
     */
    public String[] getSentences() {
        return sentences;
    }

    /**
     * @param sentences
     *            the sentences to set
     */
    public void setSentences(String[] sentences) {
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
