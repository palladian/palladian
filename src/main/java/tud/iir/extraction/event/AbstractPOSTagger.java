/**
 * 
 */
package tud.iir.extraction.event;

import org.apache.log4j.Logger;

import tud.iir.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public abstract class AbstractPOSTagger {

    /** the logger for this class. */
    protected static final Logger LOGGER = Logger
            .getLogger(AbstractPOSTagger.class);

    /** model for open nlp pos-tagging. */
    private Object model;

    /** name for the POS Tagger. */
    private String name = "unknown";

    /** The Annotations. */
    private TagAnnotations tagAnnotations;

    /**
     * tags a string and writes the tags into @see {@link #tags} and @see
     * {@link #tokens}.
     * 
     * @param sentence
     */
    public abstract void tag(String sentence);

    /**
     * tags a string and writes the tags into @see {@link #tags} and @see
     * {@link #tokens}.
     * 
     * @param sentence
     * @param configModelFilePath
     */
    public abstract void tag(String sentence, String configModelFilePath);

    /**
     * loads model into @see {@link #model}.
     * 
     * @param configModelFilePath
     * @return
     */
    public abstract boolean loadModel(String configModelFilePath);

    /**
     * loads the default model into @see {@link #model}.
     * 
     * @param configModelFilePath
     * @return
     */
    public abstract boolean loadModel();

    /**
     * Getter for model.
     * 
     * @return the model
     */
    public Object getModel() {
        return model;
    }

    /**
     * Settermethod for the model.
     * 
     * @param model
     */
    public void setModel(Object model) {
        this.model = model;
    }

    /**
     * Getter for the name.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for the name.
     * 
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * also tags a sentence and returns the @see {@link #tags}
     * 
     * @param sentence
     * @return the tag annotations
     */
    public TagAnnotations getTags(String sentence) {
        this.tag(sentence);
        return this.getTagAnnotations();
    }

    /**
     * Getter for the tagAnnotations.
     * 
     * @return the tagAnnotations
     */
    public TagAnnotations getTagAnnotations() {
        return tagAnnotations;
    }

    /**
     * Settermethod for tagAnnotations.
     * 
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

        final OpenNLPPOSTagger lppt = new OpenNLPPOSTagger();
        lppt.loadModel();

        final StopWatch sw = new StopWatch();
        sw.start();

        lppt.tag("Death toll rises after Indonesia tsunami.");
        LOGGER.info(lppt.getTagAnnotations().getTaggedString());

        sw.stop();
        LOGGER.info("time elapsed: " + sw.getElapsedTimeString());

    }

}
