/**
 * 
 */
package ws.palladian.preprocessing.nlp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

import org.apache.commons.configuration.PropertiesConfiguration;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.DataHolder;
import ws.palladian.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public class OpenNLPPOSTagger extends AbstractPOSTagger {

    /** The tokenizer. **/
    private Tokenizer tokenizer;

    /** model file path. **/
    private final transient String MODEL;
    /** tokenizer model file path. **/
    private final transient String MODEL_TOK;

    public OpenNLPPOSTagger() {
        super();
        setName("OpenNLP POS-Tagger");
        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config == null) {
            MODEL = "data/models/opennlp/postag/en-pos-maxent.bin";
            MODEL_TOK = "data/models/opennlp/tokenize/en-token.bin";
            LOGGER.warn("could not load configuration, use default location: " + MODEL + " and " + MODEL_TOK);
        } else {
            MODEL = config.getString("models.root") + config.getString("models.opennlp.en.postag");
            MODEL_TOK = config.getString("models.root") + config.getString("models.opennlp.en.tokenize");
        }
    }

    /**
     * @return the tokenizer
     */
    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractPOSTagger#loadModel()
     */
    @Override
    public OpenNLPPOSTagger loadModel() {
        return loadModel(MODEL).loadTokenizer(MODEL_TOK);
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.POSTagger#loadModel(java.lang.String)
     */
    @Override
    public OpenNLPPOSTagger loadModel(final String modelFilePath) {

        POSTaggerME tagger = null;

        if (DataHolder.getInstance().containsDataObject(modelFilePath)) {

            tagger = (POSTaggerME) DataHolder.getInstance().getDataObject(modelFilePath);

        } else {
            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            try {
                final POSModel model = new POSModel(new FileInputStream(modelFilePath));

                tagger = new POSTaggerME(model);
                DataHolder.getInstance().putDataObject(modelFilePath, tagger);

                stopWatch.stop();
                LOGGER.info("Reading " + getName() + " from file " + modelFilePath + " in "
                        + stopWatch.getElapsedTimeString());

            } catch (final InvalidFormatException e) {
                LOGGER.error(e);
            } catch (final FileNotFoundException e) {
                LOGGER.error(e);
            } catch (final IOException e) {
                LOGGER.error(e);
            }

        }

        setModel(tagger);

        return this;
    }

    /**
     * Loads the Tokenizer.
     * 
     * @param modelFilePath
     * @return
     */
    public OpenNLPPOSTagger loadTokenizer(String modelFilePath) {

        InputStream modelIn;

        if (DataHolder.getInstance().containsDataObject(modelFilePath)) {

            setTokenizer((Tokenizer) DataHolder.getInstance().getDataObject(modelFilePath));

        } else {

            try {
                modelIn = new FileInputStream(modelFilePath);

                try {
                    final TokenizerModel model = new TokenizerModel(modelIn);
                    final Tokenizer tokenizer = new TokenizerME(model);

                    DataHolder.getInstance().putDataObject(modelFilePath, tokenizer);
                    setTokenizer(tokenizer);

                } catch (final IOException e) {
                    LOGGER.error(e);
                } finally {
                    if (modelIn != null) {
                        try {
                            modelIn.close();
                        } catch (final IOException e) {
                            LOGGER.error(e);
                        }
                    }
                }
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        }
        return this;
    }

    /**
     * @param tokenizer
     *            the tokenizer to set
     */
    public void setTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.POSTagger#tag(java.lang.String)
     */
    @Override
    public OpenNLPPOSTagger tag(final String sentence) {

        final String[] tokens = getTokenizer().tokenize(sentence);

        final List<String> tokenList = new ArrayList<String>();
        for (final String token : tokens) {
            tokenList.add(token);
        }

        final List<String> tagList = ((POSTagger) getModel()).tag(tokenList);

        final TagAnnotations tagAnnotations = new TagAnnotations();
        for (int i = 0; i < tagList.size(); i++) {
            final TagAnnotation tagAnnotation = new TagAnnotation(sentence.indexOf(tokenList.get(i)), tagList.get(i),
                    tokenList.get(i));
            tagAnnotations.add(tagAnnotation);
        }

        setTagAnnotations(tagAnnotations);
        return this;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractPOSTagger#tag(java.lang.String,
     * java.lang.String)
     */
    @Override
    public OpenNLPPOSTagger tag(final String sentence, final String modelFilePath) {
        return this.loadModel(modelFilePath).tag(sentence);
    }

}
