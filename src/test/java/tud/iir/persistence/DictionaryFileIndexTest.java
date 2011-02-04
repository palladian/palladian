package tud.iir.persistence;

import java.io.File;

import org.apache.commons.io.FileUtils;

import junit.framework.TestCase;
import tud.iir.classification.Categories;
import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;

public class DictionaryFileIndexTest extends TestCase {

    public DictionaryFileIndexTest(String name) {
        super(name);
    }

    public void testFileIndex() throws Exception {

        // create the dictionary index and empty the existing one
        File dictionaryFile = new File("testIndex");
        if(!dictionaryFile.exists()) {
            dictionaryFile.mkdir();
        }
        DictionaryFileIndex di = new DictionaryFileIndex(dictionaryFile.getAbsolutePath());
        di.empty();
        di.openWriter();

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
        di.write("abc", ces);
        di.write("com/abc", ces);
        di.write(".com", ces);
        di.write(".com l_d", ces);
        di.close();

        // read from the index and check results
        di.openReader();
        ces = di.read("abc");

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
        di.openWriter();
        ces.getCategoryEntry("test1").addAbsoluteRelevance(100);
        di.update("abc", ces);
        di.close();

        // read updated values
        di.openReader();
        ces = di.read("abc");
        assertEquals(101.0, ces.getCategoryEntry("test1").getAbsoluteRelevance());
        assertEquals(absoluteRelevance2, ces.getCategoryEntry("test2").getAbsoluteRelevance());

        // System.out.println(ces);

        ces = di.read("com/abc");
        assertEquals(2, ces.size());
        ;
        // System.out.println(ces);

        ces = di.read(".Com");
        assertEquals(2, ces.size());
        // System.out.println(ces);

        ces = di.read(".Com L_d");
        // System.out.println(ces);
        assertEquals(2, ces.size());
        
        FileUtils.deleteDirectory(dictionaryFile);
    }

}