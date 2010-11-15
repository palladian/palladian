/**
 * 
 */
package tud.iir.extraction.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.postag.POSDictionary;
import opennlp.tools.postag.POSTagger;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;

import com.wcohen.secondstring.tokens.SimpleTokenizer;
import com.wcohen.secondstring.tokens.Token;

/**
 * @author Martin Wunderwald
 */
public class OpenNLPPOSTagger extends AbstractPOSTagger {

    private POSDictionary dictionary;

    public OpenNLPPOSTagger() {
        setName("OpenNLP POS-Tagger");
    }

    public boolean loadTagDictionary(String dictionaryFilePath) {

        try {
            dictionary = new POSDictionary(dictionaryFilePath);
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

        POSTagger tagger = null;

        if (DataHolder.getInstance().containsDataObject(configModelFilePath)) {

            tagger = (POSTagger) DataHolder.getInstance().getDataObject(
                    configModelFilePath);

        } else {
            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            tagger = null;// FIXME: new POSTagger(configModelFilePath, dictionary);
            DataHolder.getInstance().putDataObject(configModelFilePath, tagger);

            stopWatch.stop();
            LOGGER.info("Reading " + getName() + " from file "
                    + configModelFilePath + " in "
                    + stopWatch.getElapsedTimeString());
        }

        setModel(tagger);

        return true;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.POSTagger#tag(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void tag(String sentence) {
        try {
            // SentenceDetectorME sdetector = new
            // SentenceDetector(POSTagger.MODEL_SBD_OPENNLP);

            // final Tokenizer tokenizer = new Tokenizer(AbstractPOSTagger.MODEL_TOK_OPENNLP);
            final SimpleTokenizer tokenizer = new SimpleTokenizer(true, true);

            // final String[] tokens = tokenizer.tokenize(sentence);
            final Token[] tokens = tokenizer.tokenize(sentence);

            final List<Token> tokenList = new ArrayList<Token>();
            for (int j = 0; j < tokens.length; j++) {
                tokenList.add(tokens[j]);
            }

            final List<String> tagList = null; // FIXME:((POSTagger) getModel()).tag(tokenList);

            final TagAnnotations tagAnnotations = new TagAnnotations();
            for (int i = 0; i < tagList.size(); i++) {
                final TagAnnotation tagAnnotation = null; // FIXME: new
                                                          // TagAnnotation(sentence.indexOf(tokenList.get(i)),
                                                          // tagList.get(i), tokenList.get(i));
                tagAnnotations.add(tagAnnotation);
            }

            setTagAnnotations(tagAnnotations);

        } catch (Exception e) {
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
        this.loadModel(MODEL_POS_OPENNLP);
        loadTagDictionary(MODEL_POS_OPENNLP_DICT);
        return false;
    }

    public POSDictionary getDictionary() {
        return dictionary;
    }

    public void setDictionary(POSDictionary dictionary) {
        this.dictionary = dictionary;
    }

}
