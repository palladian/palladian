package ws.palladian.classification.text;

import java.util.Iterator;
import java.util.Objects;

import ws.palladian.core.ImmutableToken;
import ws.palladian.core.Token;
import ws.palladian.helper.collection.AbstractIterator;

public class SkipGramWrapperIterator extends AbstractIterator<Token> {

	private static final String SEPARATOR = " ";
	// private static final String SEPARATOR = " §skip§ ";
	
	private final Iterator<Token> tokenIterator;
	private Token currentToken;

	public SkipGramWrapperIterator(Iterator<Token> tokenIterator) {
		this.tokenIterator = Objects.requireNonNull(tokenIterator, "tokenIterator was null");
	}

	@Override
	protected Token getNext() throws Finished {
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
		throw FINISHED;
	}

	private static Token createSkipGram(Token token) {
		String tokenValue = token.getValue();
		String[] split = tokenValue.split("\\s");
		if (split.length > 2) {
			return new ImmutableToken(token.getStartPosition(), split[0] + SEPARATOR + split[split.length - 1]);
		}
		return null;
	}

}
