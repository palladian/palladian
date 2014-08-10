package ws.palladian.extraction.token;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

import ws.palladian.core.Token;
import ws.palladian.extraction.token.CharacterNGramTokenizer;
import ws.palladian.helper.collection.CollectionHelper;

public class CharacterNGramIteratorTest {

    private static final String STRING = "palladian";

    @Test
    public void testCharacterNGramIterator() {
        Iterator<Token> nGrams;
        nGrams = new CharacterNGramTokenizer(3, 3).iterateSpans(STRING);
        assertEquals(7, CollectionHelper.count(nGrams));

        nGrams = new CharacterNGramTokenizer(15, 15).iterateSpans(STRING);
        assertEquals(0, CollectionHelper.count(nGrams));

        nGrams = new CharacterNGramTokenizer(9, 9).iterateSpans(STRING);
        assertEquals(1, CollectionHelper.count(nGrams));

        nGrams = new CharacterNGramTokenizer(3, 8).iterateSpans(STRING);
        // CollectionHelper.print(nGrams);
        assertEquals(27, CollectionHelper.count(nGrams));
    }

}
