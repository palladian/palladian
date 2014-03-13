package ws.palladian.classification.text;

import java.util.regex.Matcher;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.AbstractIterator;

public final class TokenIterator extends AbstractIterator<String> {

    private final Matcher matcher;

    public TokenIterator(String string) {
        Validate.notNull(string, "string must not be null");
        this.matcher = Tokenizer.SPLIT_PATTERN.matcher(string);
    }

    @Override
    protected String getNext() throws Finished {
        if (matcher.find()) {
            return matcher.group();
        }
        throw FINISHED;
    }

}
