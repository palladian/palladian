package ws.palladian.classification.text;

import ws.palladian.core.ImmutableToken;
import ws.palladian.core.Token;
import ws.palladian.helper.collection.AbstractIterator2;

import java.util.Iterator;
import java.util.Objects;

public class SkipGramWrapperIterator extends AbstractIterator2<Token> {

    private static final String DEFAULT_SEPARATOR = " ";
    // private static final String SEPARATOR = " §skip§ ";

    private final Iterator<Token> tokenIterator;
    private final String separator;
    private Token currentToken;

    public SkipGramWrapperIterator(Iterator<Token> tokenIterator, String separator) {
        this.tokenIterator = Objects.requireNonNull(tokenIterator, "tokenIterator was null");
        this.separator = Objects.requireNonNull(separator, "separator was null");
    }

    public SkipGramWrapperIterator(Iterator<Token> tokenIterator) {
        this(tokenIterator, DEFAULT_SEPARATOR);
    }

    @Override
    protected Token getNext() {
        if (currentToken != null) {
            Token result = createSkipGram(currentToken);
            currentToken = null;
            if (result != null) {
                return result;
            }
        }
        if (tokenIterator.hasNext()) {
            currentToken = tokenIterator.next();
            return currentToken;
        }
        return finished();
    }

    private Token createSkipGram(Token token) {
        String tokenValue = token.getValue();
        String[] split = tokenValue.split("\\s");
        if (split.length > 2) {
            return new ImmutableToken(token.getStartPosition(), split[0] + separator + split[split.length - 1]);
            //			return new ImmutableToken(token.getStartPosition(), split[0] + StringUtils.repeat(separator, split.length - 2) + split[split.length - 1]);
        }
        return null;
    }

}
