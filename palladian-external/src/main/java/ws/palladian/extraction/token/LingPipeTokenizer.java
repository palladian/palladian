package ws.palladian.extraction.token;

import java.util.Iterator;

import ws.palladian.core.ImmutableToken;
import ws.palladian.core.Token;
import ws.palladian.core.TextTokenizer;
import ws.palladian.helper.collection.AbstractIterator;

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
public final class LingPipeTokenizer implements TextTokenizer {

    /** Factory for creating a LingPipe tokenizer. */
    private final TokenizerFactory tokenizerFactory;

    public LingPipeTokenizer() {
        tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
    }

    @Override
    public Iterator<Token> iterateTokens(String text) {
        final Tokenizer tokenizer = tokenizerFactory.tokenizer(text.toCharArray(), 0, text.length());
        return new AbstractIterator<Token>() {
            @Override
            protected Token getNext() throws Finished {
                String nextToken = tokenizer.nextToken();
                if (nextToken == null) {
                    throw FINISHED;
                }
                int startPosition = tokenizer.lastTokenStartPosition();
                return new ImmutableToken(startPosition, nextToken);
            }
        };
    }

}
