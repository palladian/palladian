package ws.palladian.classification.text;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import ws.palladian.classification.text.PruningStrategies.InformationGainPruningStrategy;

public class PruningStrategiesTest {

    private static final double DELTA = 0.01;

    /**
     * <pre>
     *            A     B     C     sum
     * term1      5     5    10      20
     * ¬ term1    0     0     0       0
     * term2      1     4     0       5
     * ¬ term2    4     1    10      15
     * term3      5     5     0      10
     * ¬ term3    0     0    10      10
     * term4      0     1     0       1
     * ¬ term4    5     4    10      19
     * term5      0     5     0       5
     * ¬ term5    5     0    10      15
     * term6      0     0    10      10
     * ¬ term6    5     5     0      10
     * 
     * numDocs    5     5    10      20
     */
    private static final String TERM_1 = "term1";
    private static final String TERM_2 = "term2";
    private static final String TERM_3 = "term3";
    private static final String TERM_4 = "term4";
    private static final String TERM_5 = "term5";
    private static final String TERM_6 = "term6";
    private static final String CATEGORY_A = "categoryA";
    private static final String CATEGORY_B = "categoryB";
    private static final String CATEGORY_C = "categoryC";

    @Test
    public void testInfoGainPruningStrategy() {
        DictionaryTrieModel.Builder builder = new DictionaryTrieModel.Builder();
        addTestData(builder);

        DictionaryModel dictionary = builder.create();
        assertEquals(20, dictionary.getNumDocuments());

        InformationGainPruningStrategy pruningStrategy = new InformationGainPruningStrategy(dictionary, 0);
        assertEquals(0, pruningStrategy.getInformationGain(dictionary.getCategoryEntries(TERM_1)), DELTA);
        assertEquals(0.45, pruningStrategy.getInformationGain(dictionary.getCategoryEntries(TERM_2)), DELTA);
        assertEquals(1, pruningStrategy.getInformationGain(dictionary.getCategoryEntries(TERM_3)), DELTA);
        assertEquals(0.10, pruningStrategy.getInformationGain(dictionary.getCategoryEntries(TERM_4)), DELTA);
        assertEquals(0.81, pruningStrategy.getInformationGain(dictionary.getCategoryEntries(TERM_5)), DELTA);
        assertEquals(1, pruningStrategy.getInformationGain(dictionary.getCategoryEntries(TERM_6)), DELTA);
    }

    @Test
    public void testTermCountPruningStrategy() {
        DictionaryTrieModel.Builder builder = new DictionaryTrieModel.Builder();
        builder.addPruningStrategy(new PruningStrategies.TermCountPruningStrategy(5));
        addTestData(builder);
        DictionaryModel dictionary = builder.create();
        assertEquals(5, dictionary.getNumUniqTerms());
    }

    private void addTestData(DictionaryTrieModel.Builder builder) {
        add(builder, 1, CATEGORY_A, TERM_1, TERM_2, TERM_3);
        add(builder, 4, CATEGORY_A, TERM_1, TERM_3);
        add(builder, 1, CATEGORY_B, TERM_1, TERM_2, TERM_3, TERM_4, TERM_5);
        add(builder, 3, CATEGORY_B, TERM_1, TERM_2, TERM_3, TERM_5);
        add(builder, 1, CATEGORY_B, TERM_1, TERM_3, TERM_5);
        add(builder, 10, CATEGORY_C, TERM_1, TERM_6);
    }

    private static void add(DictionaryBuilder builder, int nCopies, String category, String... terms) {
        for (int i = 0; i < nCopies; i++) {
            builder.addDocument(Arrays.asList(terms), category);
        }
    }
    
//    @Test
//    public void testEntropyPruningStrategy() {
//        PruningStrategy pruningStrategy = new PruningStrategies.EntropyPruningStrategy(3, .95);
//        TermCategoryEntries entries = new ImmutableTermCategoryEntries("test", new CategoryEntriesBuilder()
//                .set("one", 2).set("two", 8).set("three", 5).create());
//        assertFalse(pruningStrategy.remove(entries));
//
//        entries = new ImmutableTermCategoryEntries("test", new CategoryEntriesBuilder().set("one", 5).set("two", 5)
//                .set("three", 5).create());
//        assertTrue(pruningStrategy.remove(entries));
//
//        pruningStrategy = new PruningStrategies.EntropyPruningStrategy(3, 1);
//        assertTrue(pruningStrategy.remove(entries));
//    }
}
