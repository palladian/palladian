package ws.palladian.extraction.token;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.helper.collection.CollectionHelper;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * <p>
 * A {@link AbstractTokenizer} implementation based on <a href="http://alias-i.com/lingpipe/">LingPipe</a>'s <a
 * href="http://alias-i.com/lingpipe/docs/api/com/aliasi/tokenizer/IndoEuropeanTokenizerFactory.html"
 * >IndoEuropeanTokenizerFactory</a>.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class LingPipeTokenizer extends AbstractTokenizer {

    /** Factory for creating a LingPipe tokenizer. */
    private final TokenizerFactory tokenizerFactory;

    public LingPipeTokenizer() {
        tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
    }

    @Override
    public List<Annotation> getAnnotations(String text) {
        Tokenizer tokenizer = tokenizerFactory.tokenizer(text.toCharArray(), 0, text.length());
        List<Annotation> annotations = CollectionHelper.newArrayList();
        String nextToken = tokenizer.nextToken();
        while (nextToken != null) {
            int startPosition = tokenizer.lastTokenStartPosition();
            annotations.add(new ImmutableAnnotation(startPosition, nextToken, StringUtils.EMPTY));
            nextToken = tokenizer.nextToken();
        }
        return annotations;
    }

}
