package ws.palladian.preprocessing.nlp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.Cache;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.StopWatch;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.FastCache;

/**
 * @author Martin Wunderwald
 */
public class LingPipePOSTagger extends PosTagger {

    /** the logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(LingPipePOSTagger.class);

    /** the model. **/
    private final transient String MODEL;

    /**
     * Constructor.
     */
    public LingPipePOSTagger() {
        super();
        setName("LingPipe POS-Tagger");
        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config == null) {
            MODEL = "data/models/lingpipe/pos-en-general-brown.HiddenMarkovModel";
            LOGGER.warn("could not load configuration, use default location: " + MODEL);
        } else {
            MODEL = config.getString("models.root") + config.getString("models.lingpipe.en.postag");
        }
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractPOSTagger#loadModel()
     */
    @Override
    public LingPipePOSTagger loadModel() {
        return this.loadModel(MODEL);
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractPOSTagger#loadModel(java.lang.String)
     */
    @Override
    public LingPipePOSTagger loadModel(final String modelFilePath) {

        ObjectInputStream inputStream = null;

        try {
            HiddenMarkovModel hmm = null;

            if (Cache.getInstance().containsDataObject(modelFilePath)) {
                hmm = (HiddenMarkovModel) Cache.getInstance().getDataObject(modelFilePath);
            } else {

                final StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                inputStream = new ObjectInputStream(new FileInputStream(modelFilePath));
                hmm = (HiddenMarkovModel) inputStream.readObject();
                Cache.getInstance().putDataObject(modelFilePath, hmm);

                stopWatch.stop();
                LOGGER.info("Reading " + getName() + " from file " + modelFilePath + " in "
                        + stopWatch.getElapsedTimeString());
            }

            setModel(hmm);

        } catch (final IOException ie) {
            LOGGER.error("IO Error: " + ie.getMessage());

        } catch (final ClassNotFoundException ce) {
            LOGGER.error("Class error: " + ce.getMessage());

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException ie) {
                    LOGGER.error(ie.getMessage());
                }
            }
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractPOSTagger#tag(java.lang.String)
     */
    @Override
    public LingPipePOSTagger tag(final String sentence) {

        final int cacheSize = Integer.valueOf(100);
        final FastCache<String, double[]> cache = new FastCache<String, double[]>(cacheSize);

        // read HMM for pos tagging

        // construct chunker
        final HmmDecoder posTagger = new HmmDecoder((HiddenMarkovModel) getModel(), null, cache);
        final TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;

        // apply pos tagger
        String[] tokens = tokenizerFactory.tokenizer(sentence.toCharArray(), 0, sentence.length()).tokenize();
        List<String> tokenList = Arrays.asList(tokens);
        Tagging<String> tagging = posTagger.tag(tokenList);

        TagAnnotations tagAnnotations = new TagAnnotations();
        for (int i = 0; i < tagging.size(); i++) {

            final TagAnnotation tagAnnotation = new TagAnnotation(sentence.indexOf(tagging.token(i)), tagging.tag(i)
                    .toUpperCase(new Locale("en")), tagging.token(i));
            tagAnnotations.add(tagAnnotation);

        }
        setTagAnnotations(tagAnnotations);
        return this;
    }

    @Override
    public LingPipePOSTagger tag(String sentence, String modelFilePath) {
        return this.loadModel(modelFilePath).tag(sentence);
    }

}
