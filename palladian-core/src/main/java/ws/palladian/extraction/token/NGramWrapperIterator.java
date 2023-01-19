package ws.palladian.extraction.token;

import org.apache.commons.lang3.Validate;
import ws.palladian.classification.text.Preprocessor;
import ws.palladian.core.ImmutableToken;
import ws.palladian.core.Token;
import ws.palladian.helper.collection.AbstractIterator2;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public final class NGramWrapperIterator extends AbstractIterator2<Token> {

    private static final char SPACE = ' ';

    private final Iterator<Token> wrapped;
    private final int minLength;
    private final int maxLength;
    private final Queue<Token> tokenQueue;

    private int currentLength;

    public NGramWrapperIterator(Iterator<Token> wrapped, int minLength, int maxLength) {
        Validate.notNull(wrapped, "wrapped must not be null");
        Validate.isTrue(minLength > 0, "minLength must be greater zero");
        Validate.isTrue(maxLength >= minLength, "maxLength must be greater/equal zero");
        this.wrapped = wrapped;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.tokenQueue = new LinkedList<>();
        this.currentLength = minLength;
    }

    @Override
    protected Token getNext() {
        while (tokenQueue.size() < maxLength && wrapped.hasNext()) {
            tokenQueue.add(wrapped.next());
        }
        if (currentLength <= maxLength && currentLength <= tokenQueue.size()) {
            Optional<Token> nGram = createNGram();
            if (nGram.isPresent()) {
                return nGram.get();
            }
        }
        while (tokenQueue.size() >= minLength) {
            currentLength = minLength;
            tokenQueue.poll();
            if (wrapped.hasNext()) {
                tokenQueue.add(wrapped.next());
            }
            if (tokenQueue.size() >= minLength) {
                Optional<Token> nGram = createNGram();
                if (nGram.isPresent()) {
                    return nGram.get();
                }
            }
        }
        return finished();
    }

    private Optional<Token> createNGram() {
        int start = 0;
        StringBuilder builder = new StringBuilder();
        Iterator<Token> queueIterator = tokenQueue.iterator();
        int length = 0;
        while (queueIterator.hasNext() && length++ < currentLength) {
            Token current = queueIterator.next();
            if (current == Preprocessor.REMOVED_TOKEN) {
                return Optional.empty(); // ignore
            }
            if (length == 1) {
                start = current.getStartPosition();
            } else {
                builder.append(SPACE);
            }
            builder.append(current.getValue());
        }
        currentLength++;
        return Optional.of(new ImmutableToken(start, builder.toString()));
    }

}
