package ws.palladian.extraction.sentence;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;

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
 * @since 0.0.1
 */
public final class LingPipeSentenceDetector extends AbstractSentenceDetector {

    /**
     * The {@code SentenceChunker} instance used and containing the core implementation for splitting a processed text
     * into sentences.
     */
    private final SentenceChunker sentenceChunker;

    /** Create a new {@code LingPipeSentenceDetector}. */
    public LingPipeSentenceDetector() {
        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        SentenceModel sentenceModel = new IndoEuropeanSentenceModel();
        sentenceChunker = new SentenceChunker(tokenizerFactory, sentenceModel);
    }

    @Override
    public List<Annotation> getAnnotations(String text) {
        Validate.notNull(text, "text must not be null");
        Chunking chunking = sentenceChunker.chunk(text);
        List<Annotation> sentences = CollectionHelper.newArrayList();
        for (Chunk chunk : chunking.chunkSet()) {
            int start = chunk.start();
            int end = chunk.end();
            String value = text.substring(start, end);
            sentences.add(new ImmutableAnnotation(start, value, StringUtils.EMPTY));
        }
        return sentences;
    }
}
