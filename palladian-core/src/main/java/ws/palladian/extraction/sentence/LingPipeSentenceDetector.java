/**
 *
 */
package ws.palladian.extraction.sentence;

import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.ProcessingPipeline;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.PositionAnnotation;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * <p>
 * A sentence detector based on the implementation provided by the <a href="http://alias-i.com/lingpipe">Lingpipe</a>
 * framework.
 * </p>
 * 
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.0
 */
public final class LingPipeSentenceDetector extends AbstractSentenceDetector {

    /**
     * <p>
     * Unique identifier to serialize and deserialize objects of this type to and from a file.
     * </p>
     */
    private static final long serialVersionUID = 4827188441005628492L;
    
    private final SentenceChunker sentenceChunker;

    /**
     * <p>
     * Creates a new completely initialized sentence detector without any parameters. The state of the new object is set
     * to default values. If used as a {@code PipelineProcessor} the new sentence detector process only the
     * "originalContent" view (see {@link ProcessingPipeline}).
     * </p>
     */
    public LingPipeSentenceDetector() {
        super();
        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        SentenceModel sentenceModel = new IndoEuropeanSentenceModel();
        sentenceChunker = new SentenceChunker(tokenizerFactory, sentenceModel);
    }

    @Override
    public LingPipeSentenceDetector detect(String text) {
        Chunking chunking = sentenceChunker.chunk(text);
        Annotation[] sentences = new Annotation[chunking.chunkSet().size()];
        PipelineDocument<String> document = new PipelineDocument<String>(text);
        int ite = 0;
        for (final Chunk chunk : chunking.chunkSet()) {
            sentences[ite] = new PositionAnnotation(document, chunk.start(), chunk.end());
            ite++;
        }
        setSentences(sentences);
        return this;
    }
}
