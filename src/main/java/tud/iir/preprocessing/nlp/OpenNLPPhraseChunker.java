package tud.iir.preprocessing.nlp;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;

import org.apache.commons.configuration.PropertiesConfiguration;

import tud.iir.helper.ConfigHolder;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;

public class OpenNLPPhraseChunker extends AbstractPhraseChunker {

    /** The model path. **/
    private final String MODEL;

    /**
     * Constructor.
     */
    public OpenNLPPhraseChunker() {
        super();
        this.setName("OpenNLP Phrase Chunker");
        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config != null) {
            MODEL = config.getString("models.opennlp.en.chunker");
        } else {
            MODEL = "";
        }
    }

    /**
     * Chunks a sentence into annotations by a given list of tokens and postags.
     *
     * @param sentence
     * @param tokenList
     * @param posList
     */
    public OpenNLPPhraseChunker chunk(final String sentence,
            List<String> tokenList, List<String> posList) {

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
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * tud.iir.extraction.event.AbstractPhraseChunker#chunk(java.lang.String,
     * java.lang.String)
     */
    @Override
    public final OpenNLPPhraseChunker chunk(String sentence,
            String configModelFilePath) {
        this.loadModel(configModelFilePath);
        return this.chunk(sentence);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * tud.iir.extraction.event.AbstractPhraseChunker#chunk(java.lang.String)
     */
    @Override
    public final OpenNLPPhraseChunker chunk(String sentence) {

        final OpenNLPPOSTagger tagger = new OpenNLPPOSTagger();
        tagger.loadDefaultModel().tag(sentence);

        return chunk(sentence, tagger.getTagAnnotations().getTokenList(),
                tagger.getTagAnnotations().getTagList());

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * tud.iir.extraction.event.AbstractPhraseChunker#loadModel(java.lang.String
     * )
     */
    @Override
    public final OpenNLPPhraseChunker loadModel(String configModelFilePath) {
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

        } catch (final IOException e) {
            LOGGER.error(e);
        }

        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see tud.iir.extraction.event.AbstractPhraseChunker#loadModel()
     */
    @Override
    public final OpenNLPPhraseChunker loadDefaultModel() {
        return this.loadModel(MODEL);
    }

}
