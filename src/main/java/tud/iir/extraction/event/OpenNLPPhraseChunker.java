package tud.iir.extraction.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opennlp.tools.lang.english.Tokenizer;
import opennlp.tools.lang.english.TreebankChunker;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;

public class OpenNLPPhraseChunker extends AbstractPhraseChunker {

    public OpenNLPPhraseChunker() {
        this.setName("OpenNLP Phrase Chunker");
    }

    @SuppressWarnings("unchecked")
    public void chunk(String sentence, List<String> tokenList,
            List<String> posList) {

        final List<String> chunkList = ((TreebankChunker) getModel()).chunk(
                tokenList, posList);

        String tag = "";
        String token = "";
        String tagged = "";
        final List<String> chunks = new ArrayList<String>();
        final List<String> ftokens = new ArrayList<String>();

        // joining Tags
        for (int i = 0; i < chunkList.size(); i++) {

            if (chunkList.get(i).contains("B-")) {
                tag = chunkList.get(i).substring(2);
                token = tokenList.get(i);

            } else if (chunkList.get(i).contains("I-")) {
                token += " " + tokenList.get(i);
                tag = chunkList.get(i).substring(2);

            }
            if (((i + 1) < chunkList.size() && chunkList.get(i + 1).contains(
                    "B-"))
                    || i == chunkList.size() - 1) {

                tagged += "[" + token + "]/" + tag + " ";

                chunks.add(tag);
                ftokens.add(token);
            }
        }
        setTaggedString(tagged);
        setChunks(chunks);
        setTokens(ftokens);
    }

    @Override
    public void chunk(String sentence, String configModelFilePath) {
        this.loadModel(configModelFilePath);
        this.chunk(sentence);
    }

    @Override
    public void chunk(String sentence) {

        Tokenizer tokenizer;
        try {
            tokenizer = new Tokenizer(MODEL_TOK_OPENNLP);

            final String[] tokens = tokenizer.tokenize(sentence);
            final List<String> tokenList = Arrays.asList(tokens);

            final OpenNLPPOSTagger tagger = new OpenNLPPOSTagger();
            tagger.loadModel(MODEL_POS_OPENNLP);
            tagger.tag(sentence);

            this.chunk(sentence, tokenList, tagger.getTags());

        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        try {
            StopWatch sw = new StopWatch();
            sw.start();

            TreebankChunker tbc = null;
            if (DataHolder.getInstance()
                    .containsDataObject(configModelFilePath)) {
                tbc = (TreebankChunker) DataHolder.getInstance().getDataObject(
                        configModelFilePath);
            } else {

                tbc = new TreebankChunker(configModelFilePath);
                DataHolder.getInstance()
                        .putDataObject(configModelFilePath, tbc);
            }

            setModel(tbc);
            sw.stop();
            LOGGER.info("loaded model in " + sw.getElapsedTimeString());

            return true;
        } catch (IOException e) {
            LOGGER.error(e);
            return false;
        }

    }

    @Override
    public boolean loadModel() {
        return this.loadModel(MODEL_CHUNK_OPENNLP);
    }

}
