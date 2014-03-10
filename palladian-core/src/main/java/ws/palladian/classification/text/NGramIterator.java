package ws.palladian.classification.text;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;

public final class NGramIterator extends AbstractIterator<String> {

    private final String string;
    private final int minLength;
    private final int maxLength;
    private int offset;
    private int length;

    public NGramIterator(String string, int minLength, int maxLength) {
        Validate.isTrue(minLength > 0, "minLength must be greater zero");
        Validate.isTrue(maxLength >= minLength, "maxLength must be greater/equal zero");
        this.string = string;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.offset = 0;
        this.length = minLength;
    }

    @Override
    protected String getNext() throws Finished {
        if (offset + length > string.length()) {
            throw FINISHED;
        }
        String nGram = string.substring(offset, offset + length);
        if (length == maxLength) {
            offset++;
            length = minLength;
        } else {
            length++;
        }
        return nGram;
    }

    public static void main(String[] args) {
        NGramIterator iterator = new NGramIterator("the quick brown fox", 19, 20);
        CollectionHelper.print(iterator);
    }

}
