/**
 *
 */
package tud.iir.preprocessing.nlp;

import org.apache.log4j.Logger;

/**
 * @author Martin Wunderwald
 */
public abstract class AbstractPOSTagger {

    /** the logger for this class. */
    protected static final Logger LOGGER = Logger.getLogger(AbstractPOSTagger.class);

    /** model for open nlp pos-tagging. */
    private Object model = null;

    /** name for the POS Tagger. */
    private String name = "unknown";

    /** The Annotations. */
    private TagAnnotations tagAnnotations;

    /**
     * Getter for model.
     * 
     * @return the model
     */
    public Object getModel() {
        return model;
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
     * Getter for the tagAnnotations.
     * 
     * @return the tagAnnotations
     */
    public TagAnnotations getTagAnnotations() {
        return tagAnnotations;
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
     * loads the default model into @see {@link #model}. Method returns <code>this</code> instance of AbstractPOSTagger,
     * to allow convenient
     * concatenations of method invocations, like:
     * <code>new OpenNLPPOSTagger().loadDefaultModel().tag(...).getTagAnnotations();</code>
     * 
     * @param configModelFilePath
     * @return
     */
    public abstract AbstractPOSTagger loadDefaultModel();

    /**
     * loads the default model into @see {@link #model}. Method returns <code>this</code> instance of AbstractPOSTagger,
     * to allow convenient
     * concatenations of method invocations, like:
     * <code>new OpenNLPPOSTagger().loadDefaultModel().tag(...).getTagAnnotations();</code>
     * 
     * @param configModelFilePath
     * @return
     */
    public AbstractPOSTagger loadModel() {
        return this.loadDefaultModel();
    }

    /**
     * Loads a model trained on sample data to recognize PoS tags.
     * 
     * @param modelFilePath
     * @return This object for method chaining.
     */
    public abstract AbstractPOSTagger loadModel(String modelFilePath);

    /**
     * Settermethod for the model.
     * 
     * @param model
     */
    public void setModel(Object model) {
        this.model = model;
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
     * Settermethod for tagAnnotations.
     * 
     * @param tagAnnotations
     *            the tagAnnotations to set
     */
    public void setTagAnnotations(TagAnnotations tagAnnotations) {
        this.tagAnnotations = tagAnnotations;
    }

    /**
     * Tags a string and writes the tags into @see {@link #tags} and @see {@link #tokens}. Method returns
     * <code>this</code> instance of
     * AbstractPOSTagger, to allow convenient concatenations of method
     * invocations, like: <code>new OpenNLPPOSTagger().loadDefaultModel().tag(...).getTagAnnotations();</code>
     * 
     * @param sentence
     */
    public abstract AbstractPOSTagger tag(String sentence);

    /**
     * tags a string and writes the tags into @see {@link #tags} and @see {@link #tokens}. Method returns
     * <code>this</code> instance of
     * AbstractPOSTagger, to allow convenient concatenations of method
     * invocations, like: <code>new OpenNLPPOSTagger().loadDefaultModel().tag(...).getTagAnnotations();</code>
     * 
     * @param sentence
     * @param modelFilePath
     */
    public abstract AbstractPOSTagger tag(String sentence, String modelFilePath);

}
