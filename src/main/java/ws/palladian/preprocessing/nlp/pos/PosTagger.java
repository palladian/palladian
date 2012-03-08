/**
 *
 */
package ws.palladian.preprocessing.nlp.pos;

import org.apache.log4j.Logger;

import ws.palladian.preprocessing.nlp.TagAnnotation;
import ws.palladian.preprocessing.nlp.TagAnnotations;

/**
 * This is the abstract base class for all Part of Speech taggers used by <tt>Palladian</tt>.
 * 
 * @author Martin Wunderwald
 * @author David Urbansky
 */
public abstract class PosTagger {

    /** The logger for this class. Adapt <tt>src/main/resources/log4j.properties</tt>. */
    protected static final Logger LOGGER = Logger.getLogger(PosTagger.class);

    /** Model for open nlp pos-tagging. */
    private Object model = null;

    /** The model. **/
    protected transient String MODEL;

    /** Name for the POS Tagger. */
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
     * Also tags a sentence and returns the @see {@link #tags}.
     * 
     * @param sentence
     * @return the tag annotations
     */
    public TagAnnotations getTags(String sentence) {
        this.tag(sentence);
        return getTagAnnotations();
    }

    public String getTaggedString() {
        StringBuilder sb = new StringBuilder();
        for (TagAnnotation tagAnnotation : getTagAnnotations()) {
            sb.append(tagAnnotation.getChunk()).append("/").append(tagAnnotation.getTag()).append(" ");
        }

        return sb.toString();
    }

    protected String normalizeTag(String tag) {
        // return tag.replaceAll("(-|\\+).*", "");
        return tag.replaceAll("-.*", "");
    }

    /**
     * Loads a default PoS tagging model. Method returns <code>this</code> instance of {@code AbstractPOSTagger},
     * to allow convenient
     * concatenations of method invocations, like:
     * <code>new OpenNLPPOSTagger().loadDefaultModel().tag(...).getTagAnnotations();</code>
     * 
     * @return This object for method chaining.
     */
    public PosTagger loadModel() {
        return this.loadModel(MODEL);
    }


    /**
     * Loads a model trained on sample data to recognize PoS tags.
     * 
     * @param modelFilePath The path on the local file system pointing to the file containing the model to load.
     * @return This object for method chaining.
     */
    public abstract PosTagger loadModel(String modelFilePath);

    /**
     * <p>Sets the model containing information on how to tag natural language texts. Each domain might need its own model,
     * so even though there are some prepared models, like the brown corpus, available, you might need to train a new
     * model that fits your application domain before getting high quality results from this PoS tagger.</p>
     * 
     * @param model The model to use by this PoS tagger.
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
     * Tags a string and writes the tags. Method returns {@code this} instance of
     * AbstractPOSTagger, to allow convenient concatenations of method
     * invocations, like: <code>new OpenNLPPOSTagger().loadDefaultModel().tag(...).getTagAnnotations();</code>
     * 
     * @param sentence The sentence to tag.
     */
    public abstract PosTagger tag(String sentence);

    /**
     * Tags a string and writes the tags. Method returns {@code this} instance of
     * AbstractPOSTagger, to allow convenient concatenations of method
     * invocations, like: {@code new OpenNLPPOSTagger().loadDefaultModel().tag(...).getTagAnnotations();}
     * 
     * @param sentence The sentence to tag.
     * @param modelFilePath A path to a prepared model file containing information on how to tag sentences.
     */
    public abstract PosTagger tag(String sentence, String modelFilePath);

}
