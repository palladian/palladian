package ws.palladian.extraction.phrase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import ws.palladian.extraction.TagAnnotation;
import ws.palladian.extraction.TagAnnotations;
import ws.palladian.extraction.pos.OpenNlpPosTagger;
import ws.palladian.helper.Cache;

public final class OpenNlpPhraseChunker implements PhraseChunker {
    
    private static final String CHUNKER_NAME = "OpenNLP Phrase Chunker";
    
    private final ChunkerME model;
    
    private final OpenNlpPosTagger tagger;


    public OpenNlpPhraseChunker(File chunkerModelFile, File posTaggerModelFile) {
        model = loadModel(chunkerModelFile);
        tagger = new OpenNlpPosTagger(posTaggerModelFile);
    }
    

//    /*
//     * (non-Javadoc)
//     * @see
//     * tud.iir.extraction.event.AbstractPhraseChunker#chunk(java.lang.String)
//     */
//    @Override
//    public final OpenNlpPhraseChunker chunk(String sentence) {
//
//        final OpenNlpPosTagger tagger = new OpenNlpPosTagger();
//        TagAnnotations tagAnnotations = tagger.tag(sentence);
//
//        return chunk(sentence, tagAnnotations.getTokenList(), tagAnnotations.getTagList());
//
//    }
    
    @Override
    public TagAnnotations chunk(String sentence) {
        TagAnnotations tagAnnotations = tagger.tag(sentence);
        return chunk(sentence, tagAnnotations.getTokenList(), tagAnnotations.getTagList());
    }

    @Override
    public String getName() {
        return CHUNKER_NAME;
    }

    /**
     * <p>Chunks a sentence into annotations by a given list of tokens and postags.</p>
     * 
     * @param sentence
     * @param tokenList
     * @param posList
     */
    private TagAnnotations chunk(final String sentence, List<String> tokenList, List<String> posList) {

        // List<String> chunkList = model.chunk(tokenList, posList);
        String[] toks = tokenList.toArray(new String[tokenList.size()]);
        String[] tags = posList.toArray(new String[posList.size()]);
        String[] chunks = model.chunk(toks, tags);

        String tag = "";
        StringBuilder token = new StringBuilder();

        TagAnnotations tagAnnotations = new TagAnnotations();

        // joining Tags
        for (int i = 0; i < chunks.length; i++) {
            
            String chunk = chunks[i];

            if (chunk.contains("B-")) {
                tag = chunk.substring(2);
                token.replace(0, token.length(), tokenList.get(i));

            } else if (chunk.contains("I-")) {
                token.append(' ').append(tokenList.get(i));
                tag = chunk.substring(2);

            }
            if (i + 1 < chunks.length && chunks[i + 1].contains("B-") || i == chunks.length - 1) {

                tagAnnotations.add(new TagAnnotation(sentence.indexOf(token.toString()), tag, token.toString()));
            }
        }
        return tagAnnotations;
    }

//    /*
//     * (non-Javadoc)
//     * @see
//     * tud.iir.extraction.event.AbstractPhraseChunker#chunk(java.lang.String,
//     * java.lang.String)
//     */
//    @Override
//    public final OpenNlpPhraseChunker chunk(String sentence, String modelFilePath) {
//        loadModel(modelFilePath);
//        return this.chunk(sentence);
//    }

//    /*
//     * (non-Javadoc)
//     * @see tud.iir.extraction.event.AbstractPhraseChunker#loadModel()
//     */
//    @Override
//    public final OpenNlpPhraseChunker loadDefaultModel() {
//        return loadModel(MODEL);
//    }

//    /*
//     * (non-Javadoc)
//     * @see
//     * tud.iir.extraction.event.AbstractPhraseChunker#loadModel(java.lang.String
//     * )
//     */
//    @Override
    private final ChunkerME loadModel(File modelFile) {
        String modelFilePath = modelFile.getAbsolutePath();
        ChunkerME tbc = (ChunkerME) Cache.getInstance().getDataObject(modelFilePath);
        if (tbc == null) {
            try {
                tbc = new ChunkerME(new ChunkerModel(new FileInputStream(modelFilePath)));
                Cache.getInstance().putDataObject(modelFilePath, tbc);
            } catch (final IOException e) {
                throw new IllegalStateException("Error while loading model file \"" + modelFilePath + "\": "
                        + e.getMessage());
            }
        }
        return tbc;
    }



}
