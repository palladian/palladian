package ws.palladian.extraction.sentence;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.ImmutableSpan;
import ws.palladian.core.Token;
import ws.palladian.helper.collection.AbstractIterator;

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
public final class LingPipeSentenceDetector implements SentenceDetector {

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
    public Iterator<Token> iterateSpans(final String text) {
        Validate.notNull(text, "text must not be null");
        Chunking chunking = sentenceChunker.chunk(text);
        final Iterator<Chunk> chunkIterator = chunking.chunkSet().iterator();
        return new AbstractIterator<Token>() {
            @Override
            protected Token getNext() throws Finished {
                if (chunkIterator.hasNext()){
                    Chunk chunk = chunkIterator.next();
                    int start = chunk.start();
                    int end = chunk.end();
                    String value = text.substring(start, end);
                    return new ImmutableSpan(start, value);
                }
                throw FINISHED;
            }
        };
    }
}
