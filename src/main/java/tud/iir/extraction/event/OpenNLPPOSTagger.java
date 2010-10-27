/**
 * 
 */
package tud.iir.extraction.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.lang.english.PosTagger;
import opennlp.tools.lang.english.Tokenizer;
import opennlp.tools.postag.POSDictionary;
import tud.iir.helper.DataHolder;

/**
 * @author Martin Wunderwald
 * 
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
        } catch (IOException e) {
            LOGGER.error(e);
            return false;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see tud.iir.extraction.event.POSTagger#loadModel(java.lang.String)
     */
    @Override
    public boolean loadModel(String configModelFilePath) {

        PosTagger tagger = null;

        if (DataHolder.getInstance().containsDataObject(configModelFilePath)) {
            tagger = (PosTagger) DataHolder.getInstance().getDataObject(
                    configModelFilePath);
        } else {

            tagger = new PosTagger(configModelFilePath, dictionary);
            DataHolder.getInstance().putDataObject(configModelFilePath, tagger);
        }

        setModel(tagger);

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see tud.iir.extraction.event.POSTagger#tag(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void tag(String sentence) {
        try {
            // SentenceDetectorME sdetector = new
            // SentenceDetector(POSTagger.MODEL_SBD_OPENNLP);

            final Tokenizer tokenizer = new Tokenizer(
                    AbstractPOSTagger.MODEL_TOK_OPENNLP);

            final String[] tokens = tokenizer.tokenize(sentence);

            final List<String> tokenList = new ArrayList<String>();
            for (int j = 0; j < tokens.length; j++) {
                tokenList.add(tokens[j]);
            }

            final List<String> tagList = (List<String>) ((PosTagger) getModel())
                    .tag(tokenList);

            this.setTokens(tokenList);
            this.setTags(tagList);

            String out = "";
            for (int j = 0; j < tokenList.size(); ++j) {
                out += (tokens[j] + "/" + tagList.get(j) + " ");
            }

            this.setTaggedString(out);

        } catch (IOException e) {
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
        this.loadTagDictionary(MODEL_POS_OPENNLP_DICT);
        return false;
    }

    public POSDictionary getDictionary() {
        return dictionary;
    }

    public void setDictionary(POSDictionary dictionary) {
        this.dictionary = dictionary;
    }

}
