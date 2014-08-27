package ws.palladian.extraction.token;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.core.ImmutableToken;
import ws.palladian.core.Token;
import ws.palladian.core.TextTokenizer;
import ws.palladian.helper.collection.AbstractIterator;

public final class CharacterNGramTokenizer implements TextTokenizer {

    /** Character used to fill up left/right padding. */
    private static final String PADDING_CHARACTER = "#";

    private final int minLength;
    private final int maxLength;
    private final boolean padding;

    public CharacterNGramTokenizer(int minLength, int maxLength) {
        this(minLength, maxLength, false);
    }

    public CharacterNGramTokenizer(int minLength, int maxLength, boolean padding) {
        Validate.isTrue(minLength > 0, "minLength must be greater zero");
        Validate.isTrue(maxLength >= minLength, "maxLength must be greater/equal zero");
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.padding = padding;
    }

    @Override
    public Iterator<Token> iterateTokens(String text) {
        Validate.notNull(text, "text must not be null");
        final String textForTokenization = padding ? createPadding(text) : text;
        return new AbstractIterator<Token>() {
            private int offset = 0;
            private int length = minLength;

            @Override
            protected Token getNext() throws Finished {
                for (;;) {
                    if (offset + minLength > textForTokenization.length()) {
                        throw FINISHED;
                    }
                    String nGram = textForTokenization.substring(offset, offset + length);
                    if (offset + length == textForTokenization.length() || length == maxLength) {
                        offset++;
                        length = minLength;
                    } else {
                        length++;
                    }
                    if (nGram.replace(PADDING_CHARACTER, "").length() == 0) {
                        continue; // skip tokens which only consist of padding character
                    }
                    // System.out.println("offset=" + offset + ",ngram=" + nGram);
                    return new ImmutableToken(offset, nGram);
                }
            }
        };
    }

    /**
     * Fill up a string on the left/right with padding characters (this create additional features with explicit
     * "begin of text" and "end of text" markers.
     * 
     * @param text The text.
     * @return The padded text.
     */
    private String createPadding(String text) {
        StringBuilder padding = new StringBuilder();
        padding.append(StringUtils.repeat(PADDING_CHARACTER, maxLength - 1));
        padding.append(text);
        padding.append(StringUtils.repeat(PADDING_CHARACTER, maxLength - 1));
        return padding.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CharacterNGramTokenizer [minLength=");
        builder.append(minLength);
        builder.append(", maxLength=");
        builder.append(maxLength);
        builder.append(", padding=");
        builder.append(padding);
        builder.append("]");
        return builder.toString();
    }

}
