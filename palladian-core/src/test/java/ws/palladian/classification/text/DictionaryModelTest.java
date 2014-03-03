package ws.palladian.classification.text;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DictionaryModelTest {

    @Test
    public void testDictionaryModel() {
        String c1 = "category1";
        String c2 = "category2";

        String word1 = "word1";
        String word2 = "word2";
        String word3 = "word3";
        String word4 = "word4";
        String word5 = "word5";
        /**
         * <pre>
         * word  | c1 | c2 
         * ------+----+----
         * word1 |  2 |  
         * word2 |    |  1
         * word3 |  1 |  1
         * word4 |    |  3
         * word5 |    |
         * 
         * rel(word1,c1) = 100%
         * rel(word2,c2) = 100%
         * rel(word3,c1) = 75%, rel(word3,c2) = 25%
         * rel(word4,c2) = 100%
         * </pre>
         */
        DictionaryModel model = new DictionaryModel(null);
        model.updateTerm(word1, c1);
        model.updateTerm(word1, c1);

        model.updateTerm(word2, c2);

        model.updateTerm(word3, c1);
        model.updateTerm(word3, c1);
        model.updateTerm(word3, c1);
        model.updateTerm(word3, c2);

        model.updateTerm(word4, c2);
        model.updateTerm(word4, c2);
        model.updateTerm(word4, c2);

        // check dictionary
        assertEquals(1., model.getCategoryEntries(word1).getProbability(c1), 0);
        assertEquals(1., model.getCategoryEntries(word2).getProbability(c2), 0);
        assertEquals(0.75, model.getCategoryEntries(word3).getProbability(c1), 0);
        assertEquals(0.25, model.getCategoryEntries(word3).getProbability(c2), 0);
        assertEquals(1., model.getCategoryEntries(word4).getProbability(c2), 0);
        assertEquals(0., model.getCategoryEntries(word5).getProbability(c1), 0);
        assertEquals(0., model.getCategoryEntries(word5).getProbability(c2), 0);

        // assertEquals(2, model.getCategories().size());
        assertEquals(4, model.getNumTerms());
    }

}
