package ws.palladian.extraction.token;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.ImmutableToken;
import ws.palladian.core.Token;
import ws.palladian.core.TextTokenizer;
import ws.palladian.helper.collection.AbstractIterator;

public final class WordTokenizer implements TextTokenizer {

    private static final Pattern PATTERN = Pattern.compile(Tokenizer.TOKEN_SPLIT_REGEX, DOTALL | CASE_INSENSITIVE);

    @Override
    public Iterator<Token> iterateSpans(String text) {
        Validate.notNull(text, "text must not be null");
        final Matcher matcher = PATTERN.matcher(text);
        return new AbstractIterator<Token>() {
            @Override
            protected Token getNext() throws Finished {
                if (matcher.find()) {
                    return new ImmutableToken(matcher.start(), matcher.group());
                }
                throw FINISHED;
            }
        };
    }

}
