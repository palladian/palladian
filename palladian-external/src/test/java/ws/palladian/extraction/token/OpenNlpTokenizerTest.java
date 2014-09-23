package ws.palladian.extraction.token;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

import ws.palladian.core.Token;
import ws.palladian.helper.collection.CollectionHelper;

public class OpenNlpTokenizerTest {

    private static final String TEXT = "The quick brown fox jumps over the lazy dog.";

    @Test
    public void testOpenNlpTokenizer() {
        OpenNlpTokenizer tokenizer = new OpenNlpTokenizer();
        Iterator<Token> tokens = tokenizer.iterateTokens(TEXT);
        assertEquals(10, CollectionHelper.count(tokens));
    }

}
