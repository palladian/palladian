package ws.palladian.preprocessing.nlp;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;

import org.apache.commons.configuration.PropertiesConfiguration;

import ws.palladian.helper.Cache;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.StopWatch;

public class OpenNLPPhraseChunker extends AbstractPhraseChunker {

    /** The model path. **/
    private final transient String MODEL;

    /**
     * Constructor.
     */
    public OpenNLPPhraseChunker() {
        super();
        setName("OpenNLP Phrase Chunker");
        final PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();

        MODEL = config.getString("models.root") + config.getString("models.opennlp.en.chunker");
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractPhraseChunker#chunk(java.lang.String)
     */
    @Override
    public final OpenNLPPhraseChunker chunk(String sentence) {

        final OpenNLPPOSTagger tagger = new OpenNLPPOSTagger();
        tagger.loadModel().tag(sentence);

        return chunk(sentence, tagger.getTagAnnotations().getTokenList(), tagger.getTagAnnotations().getTagList());

    }

    /**
     * Chunks a sentence into annotations by a given list of tokens and postags.
     * 
     * @param sentence
     * @param tokenList
     * @param posList
     */
    public OpenNLPPhraseChunker chunk(final String sentence, List<String> tokenList, List<String> posList) {

        final List<String> chunkList = ((ChunkerME) getModel()).chunk(tokenList, posList);

        String tag = "";
        final StringBuffer token = new StringBuffer();

        final TagAnnotations tagAnnotations = new TagAnnotations();

        // joining Tags
        for (int i = 0; i < chunkList.size(); i++) {

            if (chunkList.get(i).contains("B-")) {
                tag = chunkList.get(i).substring(2);
                token.replace(0, token.length(), tokenList.get(i));

            } else if (chunkList.get(i).contains("I-")) {
                token.append(' ').append(tokenList.get(i));
                tag = chunkList.get(i).substring(2);

            }
            if (i + 1 < chunkList.size() && chunkList.get(i + 1).contains("B-") || i == chunkList.size() - 1) {

                tagAnnotations.add(new TagAnnotation(sentence.indexOf(token.toString()), tag, token.toString()));
            }
        }
        setTagAnnotations(tagAnnotations);
        return this;
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractPhraseChunker#chunk(java.lang.String,
     * java.lang.String)
     */
    @Override
    public final OpenNLPPhraseChunker chunk(String sentence, String modelFilePath) {
        loadModel(modelFilePath);
        return this.chunk(sentence);
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractPhraseChunker#loadModel()
     */
    @Override
    public final OpenNLPPhraseChunker loadDefaultModel() {
        return loadModel(MODEL);
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractPhraseChunker#loadModel(java.lang.String
     * )
     */
    @Override
    public final OpenNLPPhraseChunker loadModel(String modelFilePath) {
        try {

            ChunkerME tbc = null;
            if (Cache.getInstance().containsDataObject(modelFilePath)) {

                tbc = (ChunkerME) Cache.getInstance().getDataObject(modelFilePath);

            } else {
                final StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                tbc = new ChunkerME(new ChunkerModel(new FileInputStream(modelFilePath)));
                Cache.getInstance().putDataObject(modelFilePath, tbc);

                stopWatch.stop();
                LOGGER.info("Reading " + getName() + " from file " + modelFilePath + " in "
                        + stopWatch.getElapsedTimeString());
            }

            setModel(tbc);

        } catch (final IOException e) {
            LOGGER.error(e);
        }

        return this;
    }

}
