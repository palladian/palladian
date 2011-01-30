/**
 * 
 */
package tud.iir.preprocessing.nlp;

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

import tud.iir.helper.ConfigHolder;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public class OpenNLPPOSTagger extends AbstractPOSTagger {

    /** The tokenizer. **/
    private Tokenizer tokenizer;

    /** model file path. **/
    private final String MODEL;
    /** tokenizer model file path. **/
    private final String MODEL_TOK;

    public OpenNLPPOSTagger() {
        super();
        setName("OpenNLP POS-Tagger");
        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config != null) {
            MODEL = config.getString("models.root") + config.getString("models.opennlp.en.postag");
            MODEL_TOK = config.getString("models.root") + config.getString("models.opennlp.en.tokenize");
        } else {
            MODEL = "";
            MODEL_TOK = "";
        }
    }

    /**
     * Loads the Tokenizer.
     * 
     * @param configModelFilePath
     * @return
     */
    public OpenNLPPOSTagger loadTokenizer(String configModelFilePath) {

        InputStream modelIn;

        if (DataHolder.getInstance().containsDataObject(configModelFilePath)) {

            setTokenizer((Tokenizer) DataHolder.getInstance().getDataObject(
                    configModelFilePath));

        } else {

            try {
                modelIn = new FileInputStream(configModelFilePath);

                try {
                    final TokenizerModel model = new TokenizerModel(modelIn);
                    final Tokenizer tokenizer = new TokenizerME(model);

                    DataHolder.getInstance().putDataObject(configModelFilePath,
                            tokenizer);
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

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.POSTagger#loadModel(java.lang.String)
     */
    @Override
    public OpenNLPPOSTagger loadModel(final String configModelFilePath) {

        POSTaggerME tagger = null;

        if (DataHolder.getInstance().containsDataObject(configModelFilePath)) {

            tagger = (POSTaggerME) DataHolder.getInstance().getDataObject(
                    configModelFilePath);

        } else {
            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            try {
                final POSModel model = new POSModel(new FileInputStream(
                        configModelFilePath));

                tagger = new POSTaggerME(model);
                DataHolder.getInstance().putDataObject(configModelFilePath,
                        tagger);

                stopWatch.stop();
                LOGGER.info("Reading " + getName() + " from file "
                        + configModelFilePath + " in "
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
            final TagAnnotation tagAnnotation = new TagAnnotation(sentence
                    .indexOf(tokenList.get(i)), tagList.get(i), tokenList
                    .get(i));
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
    public OpenNLPPOSTagger tag(final String sentence,
            final String configModelFilePath) {
        return this.loadModel(configModelFilePath).tag(sentence);
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractPOSTagger#loadModel()
     */
    @Override
    public OpenNLPPOSTagger loadDefaultModel() {
        return loadModel(MODEL).loadTokenizer(MODEL_TOK);
    }

    /**
     * @return the tokenizer
     */
    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    /**
     * @param tokenizer
     *            the tokenizer to set
     */
    public void setTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

}
