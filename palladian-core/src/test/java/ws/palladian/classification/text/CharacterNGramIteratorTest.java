package ws.palladian.classification.text;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class CharacterNGramIteratorTest {

    private static final String STRING = "palladian";

    @Test
    public void testCharacterNGramIterator() {
        CharacterNGramIterator nGrams;
        nGrams = new CharacterNGramIterator(STRING, 3, 3);
        assertEquals(7, CollectionHelper.count(nGrams));

        nGrams = new CharacterNGramIterator(STRING, 15, 15);
        assertEquals(0, CollectionHelper.count(nGrams));

        nGrams = new CharacterNGramIterator(STRING, 9, 9);
        assertEquals(1, CollectionHelper.count(nGrams));

        nGrams = new CharacterNGramIterator(STRING, 3, 8);
        // CollectionHelper.print(nGrams);
        assertEquals(27, CollectionHelper.count(nGrams));
    }

}
