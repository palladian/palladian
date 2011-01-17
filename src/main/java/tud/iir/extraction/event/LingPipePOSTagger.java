package tud.iir.extraction.event;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.helper.ConfigHolder;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.FastCache;

/**
 * @author Martin Wunderwald
 */
public class LingPipePOSTagger extends AbstractPOSTagger {

    /** the logger for this class. */
    private static final Logger LOGGER = Logger
            .getLogger(LingPipePOSTagger.class);

    /** the model. **/
    private final String MODEL;

    /**
     * Constructor.
     */
    public LingPipePOSTagger() {
        super();
        setName("LingPipe POS-Tagger");
        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config != null) {
            MODEL = config.getString("models.lingpipe.en.postag");
        } else {
            MODEL = "";
        }
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractPOSTagger#loadModel()
     */
    @Override
    public LingPipePOSTagger loadDefaultModel() {
        return this.loadModel(MODEL);
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractPOSTagger#loadModel(java.lang.String)
     */
    @Override
    public LingPipePOSTagger loadModel(final String configModelFilePath) {

        ObjectInputStream oi = null;

        try {
            HiddenMarkovModel hmm = null;

            if (DataHolder.getInstance()
                    .containsDataObject(configModelFilePath)) {
                hmm = (HiddenMarkovModel) DataHolder.getInstance()
                        .getDataObject(configModelFilePath);
            } else {

                final StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                oi = new ObjectInputStream(new FileInputStream(
                        configModelFilePath));
                hmm = (HiddenMarkovModel) oi.readObject();
                DataHolder.getInstance()
                        .putDataObject(configModelFilePath, hmm);

                stopWatch.stop();
                LOGGER.info("Reading " + this.getName() + " from file "
                        + configModelFilePath + " in "
                        + stopWatch.getElapsedTimeString());
            }

            setModel(hmm);

        } catch (final IOException ie) {
            LOGGER.error("IO Error: " + ie.getMessage());

        } catch (final ClassNotFoundException ce) {
            LOGGER.error("Class error: " + ce.getMessage());

        } finally {
            if (oi != null) {
                try {
                    oi.close();
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
        final FastCache<String, double[]> cache = new FastCache<String, double[]>(
                cacheSize);

        // read HMM for pos tagging

        // construct chunker
        final HmmDecoder posTagger = new HmmDecoder(
                (HiddenMarkovModel) getModel(), null, cache);
        final TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;

        // apply pos tagger
        final String[] tokens = tokenizerFactory.tokenizer(
                sentence.toCharArray(), 0, sentence.length()).tokenize();
        final List<String> tokenList = Arrays.asList(tokens);
        final Tagging<String> tagging = posTagger.tag(tokenList);

        final TagAnnotations tagAnnotations = new TagAnnotations();
        for (int i = 0; i < tagging.size(); i++) {

            final TagAnnotation tagAnnotation = new TagAnnotation(sentence
                    .indexOf(tagging.token(i)), tagging.tag(i).toUpperCase(
                    new Locale("en")), tagging.token(i));
            tagAnnotations.add(tagAnnotation);

        }
        this.setTagAnnotations(tagAnnotations);
        return this;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractPOSTagger#tag(java.lang.String,
     * java.lang.String)
     */
    @Override
    public LingPipePOSTagger tag(String sentence, String configModelFilePath) {
        return this.loadModel(configModelFilePath).tag(sentence);
    }

}
