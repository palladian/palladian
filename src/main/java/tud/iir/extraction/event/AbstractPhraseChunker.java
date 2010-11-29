/**
 * 
 */
package tud.iir.extraction.event;

import org.apache.log4j.Logger;

import tud.iir.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public abstract class AbstractPhraseChunker {

    /** the logger for this class */
    protected static final Logger LOGGER = Logger
            .getLogger(AbstractPhraseChunker.class);

    protected static final String MD_POS_STANFORD = "data/models/stanford/postag/left3words-wsj-0-18.tagger";

    /** holds the model **/
    private Object model;

    /** holds the name of the chunker **/
    private String name;

    /** holds the tag Annotations. **/
    private TagAnnotations tagAnnotations;

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
    public abstract void chunk(String sentence);

    /**
     * chunks a senntence with given model file path and writes it into @see
     * {@link #chunks} and @see {@link #tokens}
     * 
     * @param sentence
     * @param configModelFilePath
     */
    public abstract void chunk(String sentence, String configModelFilePath);

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
     * @return the tagAnnotations
     */
    public TagAnnotations getTagAnnotations() {
        return tagAnnotations;
    }

    /**
     * @param tagAnnotations
     *            the tagAnnotations to set
     */
    public void setTagAnnotations(TagAnnotations tagAnnotations) {
        this.tagAnnotations = tagAnnotations;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        final OpenNLPPhraseChunker onlppc = new OpenNLPPhraseChunker();
        onlppc.loadModel();

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        onlppc.chunk("Death toll rises after Indonesia tsunami.");
        LOGGER.info(onlppc.getTagAnnotations().getTaggedString());

        stopWatch.stop();
        LOGGER.info("time elapsed: " + stopWatch.getElapsedTimeString());

    }

}
