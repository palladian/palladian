package tud.iir.extraction.event;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tud.iir.helper.DataHolder;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.FastCache;

public class LingPipePOSTagger extends AbstractPOSTagger {

    public LingPipePOSTagger() {
        setName("LingPipe POS-Tagger");
    }

    @Override
    public boolean loadModel() {
        this.loadModel(MODEL_LINGPIPE_BROWN_HMM);
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {

        ObjectInputStream oi = null;

        try {
            HiddenMarkovModel hmm = null;

            if (DataHolder.getInstance()
                    .containsDataObject(configModelFilePath)) {
                hmm = (HiddenMarkovModel) DataHolder.getInstance()
                        .getDataObject(configModelFilePath);
            } else {
                oi = new ObjectInputStream(new FileInputStream(
                        configModelFilePath));
                hmm = (HiddenMarkovModel) oi.readObject();
                DataHolder.getInstance()
                        .putDataObject(configModelFilePath, hmm);
            }

            setModel(hmm);
            return true;

        } catch (IOException ie) {
            LOGGER.error("IO Error: " + ie.getMessage());
            return false;
        } catch (ClassNotFoundException ce) {
            LOGGER.error("Class error: " + ce.getMessage());
            return false;
        } finally {
            if (oi != null) {
                try {
                    oi.close();
                } catch (IOException ie) {
                    LOGGER.error(ie.getMessage());
                }
            }
        }

    }

    public void tag(String sentence) {

        final int cacheSize = Integer.valueOf(100);
        FastCache<String, double[]> cache = new FastCache<String, double[]>(
                cacheSize);

        // read HMM for pos tagging

        // construct chunker
        HmmDecoder posTagger = new HmmDecoder((HiddenMarkovModel) getModel(),
                null, cache);
        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;

        // apply pos tagger
        String[] tokens = tokenizerFactory.tokenizer(sentence.toCharArray(), 0,
                sentence.length()).tokenize();
        List<String> tokenList = Arrays.asList(tokens);
        Tagging<String> tagging = posTagger.tag(tokenList);
        List<String> tagList = new ArrayList<String>();
        for (String tag : tagging.tags()) {
            tagList.add(tag.toUpperCase());
        }

        this.setTokens(tokenList);
        this.setTags(tagList);

        String out = "";
        for (int j = 0; j < tokenList.size(); ++j) {
            out += (tokens[j] + "/" + tagging.tag(j).toUpperCase() + " ");
        }

        this.setTaggedString(out);

    }

    @Override
    public void tag(String sentence, String configModelFilePath) {
        this.loadModel(configModelFilePath);
        this.tag(sentence);
    }

}
