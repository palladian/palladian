package ws.palladian.extraction.token;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.ImmutableSpan;
import ws.palladian.core.Token;
import ws.palladian.core.TextTokenizer;
import ws.palladian.helper.collection.AbstractIterator;

public final class CharacterNGramTokenizer implements TextTokenizer {

    private final int minLength;
    private final int maxLength;

    public CharacterNGramTokenizer(int minLength, int maxLength) {
        Validate.isTrue(minLength > 0, "minLength must be greater zero");
        Validate.isTrue(maxLength >= minLength, "maxLength must be greater/equal zero");
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public Iterator<Token> iterateSpans(final String text) {
        Validate.notNull(text, "text must not be null");
        return new AbstractIterator<Token>() {
            private int offset = 0;
            private int length = minLength;

            @Override
            protected Token getNext() throws Finished {
                if (offset + minLength > text.length()) {
                    throw FINISHED;
                }
                String nGram = text.substring(offset, offset + length);
                if (offset + length == text.length() || length == maxLength) {
                    offset++;
                    length = minLength;
                } else {
                    length++;
                }
                // System.out.println("offset=" + offset + ",ngram=" + nGram);
                return new ImmutableSpan(offset, nGram);
            }
        };
    }

}
