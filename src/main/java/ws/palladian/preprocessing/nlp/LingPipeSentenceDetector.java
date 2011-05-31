/**
 *
 */
package ws.palladian.preprocessing.nlp;

import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.featureextraction.Token;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 */
public class LingPipeSentenceDetector extends AbstractSentenceDetector {

    /**
     * Constructor.
     */
    public LingPipeSentenceDetector() {
        super();
        setName("LingPipe MaximumEntropy SentenceDetector");
        final TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        final SentenceModel sentenceModel = new IndoEuropeanSentenceModel();

        final SentenceChunker sentenceChunker = new SentenceChunker(tokenizerFactory, sentenceModel);
        setModel(sentenceChunker);
    }

    @Override
    public LingPipeSentenceDetector detect(final String text) {
        final Chunking chunking = ((SentenceChunker) getModel()).chunk(text);
        final Token[] sentences = new Token[chunking.chunkSet().size()];
        PipelineDocument document = new PipelineDocument(text);
        int ite = 0;
        for (final Chunk chunk : chunking.chunkSet()) {
            sentences[ite] = new Token(document,chunk.start(),chunk.end());
            ite++;
        }
        setSentences(sentences);
        return this;
    }
}
