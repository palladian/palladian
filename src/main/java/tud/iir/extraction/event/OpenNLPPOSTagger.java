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

import opennlp.tools.postag.POSDictionary;
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

    private POSDictionary dictionary;

    private final String MODEL;
    private final String MODEL_TOK;
    private final String MODEL_DICT;

    public OpenNLPPOSTagger() {
        setName("OpenNLP POS-Tagger");
        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/models.conf");
        } catch (ConfigurationException e) {
            LOGGER.error("could not get model path from config/models.conf, "
                    + e.getMessage());
        }

        if (config != null) {
            MODEL = config.getString("models.opennlp.en.postag");
            MODEL_TOK = config.getString("models.opennlp.en.tokenize");
            MODEL_DICT = config.getString("models.opennlp.en.postag.dict");
        } else {
            MODEL = "";
            MODEL_TOK = "";
            MODEL_DICT = "";
        }
    }

    public boolean loadTagDictionary(String dictionaryFilePath) {

        try {
            dictionary = POSDictionary.create(new FileInputStream(
                    dictionaryFilePath));
            setDictionary(dictionary);
            return true;
        } catch (final IOException e) {
            LOGGER.error(e);
            return false;
        }

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
                POSModel model = new POSModel(new FileInputStream(
                        configModelFilePath));

                tagger = new POSTaggerME(model);
                DataHolder.getInstance().putDataObject(configModelFilePath,
                        tagger);

                stopWatch.stop();
                LOGGER.info("Reading " + this.getName() + " from file "
                        + configModelFilePath + " in "
                        + stopWatch.getElapsedTimeString());

            } catch (InvalidFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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
        try {
            // SentenceDetectorME sdetector = new
            // SentenceDetector(POSTagger.MODEL_SBD_OPENNLP);

            InputStream modelIn = new FileInputStream(MODEL_TOK);
            try {
                TokenizerModel model = new TokenizerModel(modelIn);
                Tokenizer tokenizer = new TokenizerME(model);
                final String[] tokens = tokenizer.tokenize(sentence);

                final List<String> tokenList = new ArrayList<String>();
                for (int j = 0; j < tokens.length; j++) {
                    tokenList.add(tokens[j]);
                }

                final List<String> tagList = ((POSTagger) getModel())
                        .tag(tokenList);

                final TagAnnotations tagAnnotations = new TagAnnotations();
                for (int i = 0; i < tagList.size(); i++) {
                    final TagAnnotation tagAnnotation = new TagAnnotation(
                            sentence.indexOf(tokenList.get(i)), tagList.get(i),
                            tokenList.get(i));
                    tagAnnotations.add(tagAnnotation);
                }

                this.setTagAnnotations(tagAnnotations);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (modelIn != null) {
                    try {
                        modelIn.close();
                    } catch (IOException e) {
                    }
                }
            }

        } catch (final IOException e) {
            LOGGER.error(e);
        }

    }

    @Override
    public void tag(String sentence, String configModelFilePath) {
        this.loadModel(configModelFilePath);
        this.tag(sentence);
    }

    @Override
    public boolean loadModel() {
        this.loadModel(MODEL);
        this.loadTagDictionary(MODEL_DICT);
        return false;
    }

    public POSDictionary getDictionary() {
        return dictionary;
    }

    public void setDictionary(POSDictionary dictionary) {
        this.dictionary = dictionary;
    }

}
