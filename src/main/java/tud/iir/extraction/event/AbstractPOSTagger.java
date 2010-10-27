/**
 * 
 */
package tud.iir.extraction.event;

import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.helper.StopWatch;

/**
 * @author Martin Wunderwald
 * 
 */
public abstract class AbstractPOSTagger {

    /** the logger for this class */
    protected static final Logger LOGGER = Logger
            .getLogger(AbstractPOSTagger.class);

    /** base model path */
    protected static final String MODEL_PATH = "data/models/";

    /** model for open nlp pos-tagging */
    private Object model;

    /** name for the POS Tagger */
    private String name = "unknown";

    /** dict file for opennlp pos tagging */
    protected static final String MODEL_POS_OPENNLP_DICT = MODEL_PATH
            + "opennlp/postag/tagdict.txt";

    /** dict file for opennlp pos tagging */
    protected static final String MODEL_POS_OPENNLP = MODEL_PATH
            + "opennlp/postag/tag.bin.gz";

    /** model for opennlp tokenization */
    protected static final String MODEL_TOK_OPENNLP = MODEL_PATH
            + "opennlp/tokenize/EnglishTok.bin.gz";

    /** brown hidden markov model for lingpipe chunker */
    protected static final String MODEL_LINGPIPE_BROWN_HMM = MODEL_PATH
            + "lingpipe/pos-en-general-brown.HiddenMarkovModel";

    private List<String> tags;
    private List<String> tokens;
    private String taggedString;

    public abstract void tag(String sentence);

    public abstract void tag(String sentence, String configModelFilePath);

    public abstract boolean loadModel(String configModelFilePath);

    public abstract boolean loadModel();

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

    public List<String> getTags(String sentence) {
        this.tag(sentence);
        return this.getTags();
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    public String getTaggedString() {
        return taggedString;
    }

    public void setTaggedString(String taggedString) {
        this.taggedString = taggedString;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        OpenNLPPOSTagger lppt = new OpenNLPPOSTagger();
        lppt.loadModel();

        StopWatch sw = new StopWatch();
        sw.start();

        lppt.tag("Death toll rises after Indonesia tsunami.");
        LOGGER.info(lppt.getTaggedString());

        sw.stop();
        LOGGER.info("time elapsed: " + sw.getElapsedTimeString());

    }

}
