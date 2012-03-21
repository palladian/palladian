package ws.palladian.classification.persistence;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Dictionary;
import ws.palladian.classification.persistence.DictionaryDbIndexH2;
import ws.palladian.classification.persistence.DictionaryFileIndex;
import ws.palladian.classification.persistence.DictionaryIndex;

/**
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
@RunWith(Parameterized.class)
public class DictionaryIndexTest {

    private final DictionaryIndex index;

    public DictionaryIndexTest(DictionaryIndex index) {
        this.index = index;
    }

    /**
     * <p>
     * Get the {@link DictionaryIndex} implementations to be tested.
     * </p>
     * 
     * @return The classes under tests.
     */
    @Parameters
    public static Collection<Object[]> testData() {
        DictionaryFileIndex fileIndex = new DictionaryFileIndex("data/temp/testIndexFile");
        DictionaryDbIndexH2 h2Index = new DictionaryDbIndexH2("data/temp/testIndexH2");
        Object[][] data = new Object[][] { {fileIndex}, {h2Index}};
        return Arrays.asList(data);
    }

    @Before
    public void before() {
        index.setDictionary(new Dictionary("test_dictionary", 1));
        index.empty();
        index.openWriter();
    }

    @Test
    public void testIndex() {

        // create the categories and category entries
        CategoryEntries ces = new CategoryEntries();
        Categories cats = new Categories();
        Category test = new Category("test1");
        Category test2 = new Category("test2");
        cats.add(test);
        cats.add(test2);
        double absoluteRelevance1 = 1.0;
        double absoluteRelevance2 = 2.0;

        // add category entries and increase frequency for categories
        ces.add(new CategoryEntry(ces, test, absoluteRelevance1));
        test.increaseFrequency();
        ces.add(new CategoryEntry(ces, test2, absoluteRelevance2));
        test2.increaseFrequency();
        cats.calculatePriors();

        // write to the index and close it
        index.write("abc", ces);
        index.write("com/abc", ces);
        index.write(".com", ces);
        index.write(".com l_d", ces);
        index.close();

        // read from the index and check results
        index.openReader();
        ces = index.read("abc");

        // number of category entries
        assertEquals(2, ces.size());

        // absolute relevance
        assertEquals(absoluteRelevance1, ces.getCategoryEntry("test1").getAbsoluteRelevance(), 0);
        assertEquals(absoluteRelevance2, ces.getCategoryEntry("test2").getAbsoluteRelevance(), 0);

        // relative relevance
        assertEquals(1.0 / 3.0, ces.getCategoryEntry("test1").getRelevance(), 0);
        assertEquals(2.0 / 3.0, ces.getCategoryEntry("test2").getRelevance(), 0);

        // category priors
        // assertEquals(0.5,ces.getCategoryEntry("test1").getCategory().getPrior());
        // assertEquals(0.5,ces.getCategoryEntry("test2").getCategory().getPrior());

        // update index
        index.openWriter();
        ces.getCategoryEntry("test1").addAbsoluteRelevance(100);
        index.update("abc", ces);
        index.close();

        // read updated values
        index.openReader();
        ces = index.read("abc");
        assertEquals(101.0, ces.getCategoryEntry("test1").getAbsoluteRelevance(), 0);
        assertEquals(absoluteRelevance2, ces.getCategoryEntry("test2").getAbsoluteRelevance(), 0);

        // System.out.println(ces);

        ces = index.read("com/abc");
        assertEquals(2, ces.size());
        // System.out.println(ces);

        ces = index.read(".Com");
        assertEquals(2, ces.size());
        // System.out.println(ces);

        ces = index.read(".Com L_d");
        // System.out.println(ces);
        assertEquals(2, ces.size());

    }

    @After
    public void after() {
        index.close();
    }

}
