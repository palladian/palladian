package ws.palladian.classification.text;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.AbstractIterator;

public final class TokenIterator extends AbstractIterator<String> {

    private static final Pattern PATTERN = Pattern.compile(Tokenizer.TOKEN_SPLIT_REGEX, DOTALL | CASE_INSENSITIVE);

    private final Matcher matcher;

    public TokenIterator(String string) {
        Validate.notNull(string, "string must not be null");
        this.matcher = PATTERN.matcher(string);
    }

    @Override
    protected String getNext() throws Finished {
        if (matcher.find()) {
            return matcher.group();
        }
        throw FINISHED;
    }

}
