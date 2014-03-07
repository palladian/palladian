package ws.palladian.classification.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

public class DictionaryModelTest {

    private static final String CATEGORY_1 = "category1";
    private static final String CATEGORY_2 = "category2";

    private static final String WORD_1 = "word1";
    private static final String WORD_2 = "word2";
    private static final String WORD_3 = "word3";
    private static final String WORD_4 = "word4";
    private static final String WORD_5 = "word5";

    private DictionaryModel model;

    @Before
    public void setUp() {
        /**
         * <pre>
         * d1 = { c1, [word1, word3] }
         * d2 = { c2, [word2, word4] }
         * d3 = { c2, [word3, word4] }
         * d4 = { c1, [word1, word3] }
         * d5 = { c2, [word4] }
         * d6 = { c1, [word3] }
         * 
         * word  | c1 | c2 
         * ------+----+----
         * word1 |  2 |  
         * word2 |    |  1
         * word3 |  3 |  1
         * word4 |    |  3
         * word5 |    |
         * 
         * rel(word1,c1) = 100%
         * rel(word2,c2) = 100%
         * rel(word3,c1) = 75%, rel(word3,c2) = 25%
         * rel(word4,c2) = 100%
         * </pre>
         */
        model = new DictionaryModel(null);
        model.addDocument(CollectionHelper.newHashSet(WORD_1, WORD_3), CATEGORY_1);
        model.addDocument(CollectionHelper.newHashSet(WORD_2, WORD_4), CATEGORY_2);
        model.addDocument(CollectionHelper.newHashSet(WORD_3, WORD_4), CATEGORY_2);
        model.addDocument(CollectionHelper.newHashSet(WORD_1, WORD_3), CATEGORY_1);
        model.addDocument(CollectionHelper.newHashSet(WORD_4), CATEGORY_2);
        model.addDocument(CollectionHelper.newHashSet(WORD_3), CATEGORY_1);
    }

    @Test
    public void testDictionaryModel() {
        // check dictionary
        assertEquals(1., model.getCategoryEntries(WORD_1).getProbability(CATEGORY_1), 0);
        assertEquals(2, model.getCategoryEntries(WORD_1).getCount(CATEGORY_1));
        assertEquals(1., model.getCategoryEntries(WORD_2).getProbability(CATEGORY_2), 0);
        assertEquals(0, model.getCategoryEntries(WORD_1).getCount(CATEGORY_2));
        assertEquals(0.75, model.getCategoryEntries(WORD_3).getProbability(CATEGORY_1), 0);
        assertEquals(0.25, model.getCategoryEntries(WORD_3).getProbability(CATEGORY_2), 0);
        assertEquals(1., model.getCategoryEntries(WORD_4).getProbability(CATEGORY_2), 0);
        assertEquals(0., model.getCategoryEntries(WORD_5).getProbability(CATEGORY_1), 0);
        assertEquals(0., model.getCategoryEntries(WORD_5).getProbability(CATEGORY_2), 0);

        assertEquals(2, model.getCategories().size());
        assertEquals(4, model.getNumTerms());
    }

    @Test
    public void testSerialization() throws IOException {
        File tempDir = FileHelper.getTempDir();
        String tempFile = new File(tempDir, "dictionaryModel.ser").getPath();
        FileHelper.serialize(model, tempFile);
        Serializable deserializedModel = FileHelper.deserialize(tempFile);
        assertTrue(deserializedModel.equals(model));
    }

}
