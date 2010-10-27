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
public abstract class AbstractPhraseChunker {

    /** the logger for this class */
    protected static final Logger LOGGER = Logger
            .getLogger(PhraseChunker.class);

    /** base model path */
    protected static final String MODEL_PATH = "data/models/";

    /** model for open nlp pos-tagging */
    protected static final String MODEL_POS_OPENNLP = MODEL_PATH
            + "opennlp/postag/tag.bin.gz";

    /** dict file for opennlp pos tagging */
    protected static final String MODEL_POS_OPENNLP_DICT = MODEL_PATH
            + "opennlp/postag/tagdict.txt";

    /** model for opennlp sentence detection */
    protected static final String MODEL_SBD_OPENNLP = MODEL_PATH
            + "opennlp/sentdetect/EnglishSD.bin.gz";

    /** model for opennlp phrase chunking */
    protected static final String MODEL_CHUNK_OPENNLP = MODEL_PATH
            + "opennlp/chunker/EnglishChunk.bin.gz";

    /** model for opennlp tokenization */
    protected static final String MODEL_TOK_OPENNLP = MODEL_PATH
            + "opennlp/tokenize/EnglishTok.bin.gz";

    /** brown hidden markov model for lingpipe chunker */
    protected static final String MODEL_LINGPIPE_BROWN_HMM = MODEL_PATH
            + "pos-en-general-brown.HiddenMarkovModel";

    protected static final String MODEL_POS_STANFORD = MODEL_PATH
            + "stanford/postag/left3words-wsj-0-18.tagger";

    private Object model;
    private String name;

    private List<String> tokens;
    private List<String> chunks;
    private String taggedString;

    public abstract boolean loadModel(String configModelFilePath);

    public abstract boolean loadModel();

    public abstract void chunk(String sentence);

    public abstract void chunk(String sentence, String configModelFilePath);

    public List<String> getTokens() {
        return tokens;
    }

    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    public List<String> getChunks(String sentence) {
        this.chunk(sentence);
        return this.getChunks();
    }

    public List<String> getChunks() {
        return chunks;
    }

    public void setChunks(List<String> chunks) {
        this.chunks = chunks;
    }

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

        OpenNLPPhraseChunker pc = new OpenNLPPhraseChunker();
        pc.loadModel();

        StopWatch sw = new StopWatch();
        sw.start();

        pc.chunk("Death toll rises after Indonesia tsunami.");
        LOGGER.info(pc.getTaggedString());

        sw.stop();
        LOGGER.info("time elapsed: " + sw.getElapsedTimeString());

    }

}
