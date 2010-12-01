/**
 * 
 */
package tud.iir.extraction.event;

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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public class OpenNLPPOSTagger extends AbstractPOSTagger {

    private Tokenizer tokenizer;

    private final String MODEL;
    private final String MODEL_TOK;

    public OpenNLPPOSTagger() {
        setName("OpenNLP POS-Tagger");
        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/models.conf");
        } catch (final ConfigurationException e) {
            LOGGER.error("could not get model path from config/models.conf, "
                    + e.getMessage());
        }

        if (config != null) {
            MODEL = config.getString("models.opennlp.en.postag");
            MODEL_TOK = config.getString("models.opennlp.en.tokenize");
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
    public boolean loadTokenizer(String configModelFilePath) {

        InputStream modelIn;

        if (DataHolder.getInstance().containsDataObject(configModelFilePath)) {

            setTokenizer((Tokenizer) DataHolder.getInstance().getDataObject(
                    configModelFilePath));
            return true;
        } else {

            try {
                modelIn = new FileInputStream(configModelFilePath);

                try {
                    final TokenizerModel model = new TokenizerModel(modelIn);
                    final Tokenizer tokenizer = new TokenizerME(model);

                    DataHolder.getInstance().putDataObject(configModelFilePath,
                            tokenizer);
                    setTokenizer(tokenizer);

                    return true;
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
        return false;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.POSTagger#loadModel(java.lang.String)
     */
    @Override
    public boolean loadModel(String configModelFilePath) {

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
                LOGGER.info("Reading " + this.getName() + " from file "
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

        return true;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.POSTagger#tag(java.lang.String)
     */
    @Override
    public void tag(String sentence) {

        final String[] tokens = getTokenizer().tokenize(sentence);

        final List<String> tokenList = new ArrayList<String>();
        for (int j = 0; j < tokens.length; j++) {
            tokenList.add(tokens[j]);
        }

        final List<String> tagList = ((POSTagger) getModel()).tag(tokenList);

        final TagAnnotations tagAnnotations = new TagAnnotations();
        for (int i = 0; i < tagList.size(); i++) {
            final TagAnnotation tagAnnotation = new TagAnnotation(sentence
                    .indexOf(tokenList.get(i)), tagList.get(i), tokenList
                    .get(i));
            tagAnnotations.add(tagAnnotation);
        }

        this.setTagAnnotations(tagAnnotations);

    }

    @Override
    public void tag(String sentence, String configModelFilePath) {
        this.loadModel(configModelFilePath);
        this.tag(sentence);
    }

    @Override
    public boolean loadModel() {
        this.loadModel(MODEL);
        this.loadTokenizer(MODEL_TOK);
        return false;
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
