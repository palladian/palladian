/**
 *
 */
package tud.iir.extraction.event;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * @author Martin Wunderwald
 */
public class LingPipeSentenceDetector extends AbstractSentenceDetector {

    /**
     * Constructor.
     */
    public LingPipeSentenceDetector() {
        super();
        setName("LingPipe MaximumEntropy SentenceDetector");
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractSentenceDetector#detect(java.lang.String
     * )
     */
    @Override
    public void detect(final String text) {
        final Chunking chunking = ((SentenceChunker) getModel()).chunk(text);
        final String[] sentences = new String[chunking.chunkSet().size()];
        int ite = 0;
        for (final Chunk chunk : chunking.chunkSet()) {
            sentences[ite] = text.substring(chunk.start(), chunk.end());
            ite++;
        }
        setSentences(sentences);
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractSentenceDetector#detect(java.lang.String
     * , java.lang.String)
     */
    @Override
    public void detect(final String text, final String configModelFilePath) {
        detect(text);
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractSentenceDetector#loadModel(java.lang
     * .String)
     */
    @Override
    public boolean loadModel(final String configModelFilePath) {
        final TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        final SentenceModel sentenceModel = new IndoEuropeanSentenceModel();

        final SentenceChunker sentenceChunker = new SentenceChunker(
                tokenizerFactory, sentenceModel);
        setModel(sentenceChunker);
        return false;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractSentenceDetector#loadModel()
     */
    @Override
    public boolean loadModel() {
        return loadModel(null);
    }

}
