/**
 * 
 */
package tud.iir.preprocessing.nlp;

import org.apache.log4j.Logger;

import tud.iir.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public abstract class AbstractPhraseChunker {

    /** The logger for this class. */
    protected static final Logger LOGGER = Logger
            .getLogger(AbstractPhraseChunker.class);

    /** Holds the model. **/
    private Object model = null;

    /** Holds the name of the chunker. **/
    private String name = null;

    /** holds the tag Annotations. **/
    private TagAnnotations tagAnnotations;

    /**
     * Loads the chunker model into the chunker. Method returns
     * <code>this</code> instance of AbstractPhraseChunker, to allow convenient
     * concatenations of method invocations, like:
     * <code>new OpenNLPPhraseChunker().loadDefaultModel().chunk(...).getTagAnnotations();</code>
     * 
     * @param configModelFilePath
     * @return
     */
    public abstract AbstractPhraseChunker loadModel(String configModelFilePath);

    /**
     * Loads the default chunker model into the chunker.
     * 
     * @return
     */
    public abstract AbstractPhraseChunker loadDefaultModel();

    /**
     * Chunks a sentence and writes parts in @see {@link #chunks} and @see
     * {@link #tokens}.
     * 
     * @param sentence
     */
    public abstract AbstractPhraseChunker chunk(String sentence);

    /**
     * Chunks a senntence with given model file path and writes it into @see
     * {@link #chunks} and @see {@link #tokens}.
     * 
     * @param sentence
     * @param configModelFilePath
     */
    public abstract AbstractPhraseChunker chunk(String sentence,
            String configModelFilePath);

    /**
     * Getter for the Chunker Model.
     * 
     * @return
     */
    public final Object getModel() {
        return model;
    }

    /**
     * Setter for the chunker model.
     * 
     * @param model
     */
    public final void setModel(Object model) {
        this.model = model;
    }

    /**
     * Getter for the name.
     * 
     * @return
     */
    public final String getName() {
        return name;
    }

    /**
     * Setter for name.
     * 
     * @param name
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * @return the tagAnnotations
     */
    public final TagAnnotations getTagAnnotations() {
        return tagAnnotations;
    }

    /**
     * @param tagAnnotations
     *            the tagAnnotations to set
     */
    public final void setTagAnnotations(TagAnnotations tagAnnotations) {
        this.tagAnnotations = tagAnnotations;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        final OpenNLPPhraseChunker onlppc = new OpenNLPPhraseChunker();
        onlppc.loadDefaultModel();

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        onlppc.chunk("Death toll rises after Indonesia tsunami.");
        LOGGER.info(onlppc.getTagAnnotations().getTaggedString());

        stopWatch.stop();
        LOGGER.info("time elapsed: " + stopWatch.getElapsedTimeString());

    }

}
