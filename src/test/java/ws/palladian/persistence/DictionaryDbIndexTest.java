package ws.palladian.persistence;

import junit.framework.TestCase;
import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Dictionary;

public class DictionaryDBIndexTest extends TestCase {

    public void testDBIndex() {

        DictionaryDbIndexH2 dbIndex = new DictionaryDbIndexH2("test_dictionary", "root", "", "");
        dbIndex.setDictionary(new Dictionary("test_dictionary", 1));
        dbIndex.empty();

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
        dbIndex.write("abc", ces);
        dbIndex.write("com/abc", ces);
        dbIndex.write(".com", ces);
        dbIndex.write(".com l_d", ces);

        // read from the index and check results
        ces = dbIndex.read("abc");

        // number of category entries
        assertEquals(2, ces.size());

        // absolute relevance
        assertEquals(absoluteRelevance1, ces.getCategoryEntry("test1").getAbsoluteRelevance());
        assertEquals(absoluteRelevance2, ces.getCategoryEntry("test2").getAbsoluteRelevance());

        // relative relevance
        assertEquals(1.0 / 3.0, ces.getCategoryEntry("test1").getRelevance());
        assertEquals(2.0 / 3.0, ces.getCategoryEntry("test2").getRelevance());

        // category priors
        // assertEquals(0.5,ces.getCategoryEntry("test1").getCategory().getPrior());
        // assertEquals(0.5,ces.getCategoryEntry("test2").getCategory().getPrior());

        // update index
        ces.getCategoryEntry("test1").addAbsoluteRelevance(100);
        dbIndex.update("abc", ces);

        // read updated values
        ces = dbIndex.read("abc");
        assertEquals(101.0, ces.getCategoryEntry("test1").getAbsoluteRelevance());
        assertEquals(absoluteRelevance2, ces.getCategoryEntry("test2").getAbsoluteRelevance());

        // System.out.println(ces);

        ces = dbIndex.read("com/abc");
        assertEquals(2, ces.size());
        // System.out.println(ces);

        ces = dbIndex.read(".com");
        assertEquals(2, ces.size());
        // System.out.println(ces);

        ces = dbIndex.read(".com l_d");
        // System.out.println(ces);
        assertEquals(2, ces.size());

    }

}
