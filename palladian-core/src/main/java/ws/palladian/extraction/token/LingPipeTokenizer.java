package ws.palladian.extraction.token;

import java.util.List;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * <p>
 * A {@link BaseTokenizer} implementation based on <a href="http://alias-i.com/lingpipe/">LingPipe</a>'s <a
 * href="http://alias-i.com/lingpipe/docs/api/com/aliasi/tokenizer/IndoEuropeanTokenizerFactory.html"
 * >IndoEuropeanTokenizerFactory</a>.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class LingPipeTokenizer extends BaseTokenizer {

    /** Factory for creating a LingPipe tokenizer. */
    private final TokenizerFactory tokenizerFactory;

    public LingPipeTokenizer() {
        tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
    }

    @Override
    public List<Annotation> getAnnotations(String text) {
        com.aliasi.tokenizer.Tokenizer tokenizer = tokenizerFactory.tokenizer(text.toCharArray(), 0, text.length());
        String nextToken = tokenizer.nextToken();
        List<Annotation> annotations = CollectionHelper.newArrayList();
        while (nextToken != null) {
            int startPosition = tokenizer.lastTokenStartPosition();
            annotations.add(new ImmutableAnnotation(startPosition, nextToken, PROVIDED_FEATURE));
            nextToken = tokenizer.nextToken();
        }
        return annotations;
    }

}
