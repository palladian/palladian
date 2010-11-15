package tud.iir.extraction.event;

import java.util.List;

public class OpenNLPPhraseChunker extends AbstractPhraseChunker {

    public OpenNLPPhraseChunker() {
        setName("OpenNLP Phrase Chunker");
    }

    @SuppressWarnings("unchecked")
    public void chunk(String sentence, List<String> tokenList,
            List<String> posList) {

        final List<String> chunkList = null; // FIXME:((TreebankChunker) getModel()).chunk(tokenList, posList);

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
            if (i + 1 < chunkList.size() && chunkList.get(i + 1).contains(
                    "B-")
                    || i == chunkList.size() - 1) {

                tagAnnotations.add(new TagAnnotation(sentence.indexOf(token),
                        tag, token));
            }
        }
        setTagAnnotations(tagAnnotations);
    }

    @Override
    public void chunk(String sentence, String configModelFilePath) {
        this.loadModel(configModelFilePath);
        this.chunk(sentence);
    }

    @Override
    public void chunk(String sentence) {

        final OpenNLPPOSTagger tagger = new OpenNLPPOSTagger();
        tagger.loadModel(MD_POS_ONLP);
        tagger.tag(sentence);

        chunk(sentence, tagger.getTagAnnotations().getTokenList(), tagger
                .getTagAnnotations().getTagList());

    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        try {

            /*
             * FIXME:
             * TreebankChunker tbc = null;
             * if (DataHolder.getInstance()
             * .containsDataObject(configModelFilePath)) {
             * tbc = (TreebankChunker) DataHolder.getInstance().getDataObject(
             * configModelFilePath);
             * } else {
             * final StopWatch stopWatch = new StopWatch();
             * stopWatch.start();
             * tbc = new TreebankChunker(configModelFilePath);
             * DataHolder.getInstance()
             * .putDataObject(configModelFilePath, tbc);
             * stopWatch.stop();
             * LOGGER.info("Reading " + getName() + " from file "
             * + configModelFilePath + " in "
             * + stopWatch.getElapsedTimeString());
             * }
             * setModel(tbc);
             */

            return true;
        } catch (Exception e) {
            LOGGER.error(e);
            return false;
        }

    }

    @Override
    public boolean loadModel() {
        return this.loadModel(MD_CHUNK_ONLP);
    }

}
