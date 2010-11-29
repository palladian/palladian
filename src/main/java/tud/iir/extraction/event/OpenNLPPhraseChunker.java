package tud.iir.extraction.event;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;

public class OpenNLPPhraseChunker extends AbstractPhraseChunker {

    private final String MODEL;

    public OpenNLPPhraseChunker() {
        this.setName("OpenNLP Phrase Chunker");
        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/models.conf");
        } catch (ConfigurationException e) {
            LOGGER.error("could not get model path from config/models.conf, "
                    + e.getMessage());
        }

        if (config != null) {
            MODEL = config.getString("models.opennlp.en.chunker");
        } else {
            MODEL = "";
        }
    }

    public void chunk(String sentence, List<String> tokenList,
            List<String> posList) {

        final List<String> chunkList = ((ChunkerME) getModel()).chunk(
                tokenList, posList);

        String tag = "";
        String token = "";

        final TagAnnotations tagAnnotations = new TagAnnotations();

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

                tagAnnotations.add(new TagAnnotation(sentence.indexOf(token),
                        tag, token));
            }
        }
        this.setTagAnnotations(tagAnnotations);
    }

    @Override
    public void chunk(String sentence, String configModelFilePath) {
        this.loadModel(configModelFilePath);
        this.chunk(sentence);
    }

    @Override
    public void chunk(String sentence) {

        final OpenNLPPOSTagger tagger = new OpenNLPPOSTagger();
        tagger.loadModel();
        tagger.tag(sentence);

        chunk(sentence, tagger.getTagAnnotations().getTokenList(), tagger
                .getTagAnnotations().getTagList());

    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        try {

            ChunkerME tbc = null;
            if (DataHolder.getInstance()
                    .containsDataObject(configModelFilePath)) {

                tbc = (ChunkerME) DataHolder.getInstance().getDataObject(
                        configModelFilePath);

            } else {
                final StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                tbc = new ChunkerME(new ChunkerModel(new FileInputStream(
                        configModelFilePath)));
                DataHolder.getInstance()
                        .putDataObject(configModelFilePath, tbc);

                stopWatch.stop();
                LOGGER.info("Reading " + this.getName() + " from file "
                        + configModelFilePath + " in "
                        + stopWatch.getElapsedTimeString());
            }

            setModel(tbc);

            return true;
        } catch (final IOException e) {
            LOGGER.error(e);
            return false;
        }

    }

    @Override
    public boolean loadModel() {
        return this.loadModel(MODEL);
    }

}
