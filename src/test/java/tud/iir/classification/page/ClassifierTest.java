package tud.iir.classification.page;

import junit.framework.TestCase;
import tud.iir.classification.Categories;
import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.classification.Dictionary;
import tud.iir.classification.Term;
import tud.iir.classification.page.evaluation.ClassificationTypeSetting;

public class ClassifierTest extends TestCase {

    public ClassifierTest(String name) {
        super(name);
    }

    public void testClassifier() {

        Category c1 = new Category("category1");
        Category c2 = new Category("category2");
        c1.increaseFrequency();
        c1.increaseFrequency();
        c1.increaseFrequency();
        c2.increaseFrequency();

        Categories categories = new Categories();
        categories.add(c1);
        categories.add(c2);

        categories.calculatePriors();
        categories.calculatePriors();

        // check priors
        assertEquals(0.75, c1.getPrior());
        assertEquals(0.25, c2.getPrior());

        System.out.println(categories);

        Term word1 = new Term("word1");
        Term word2 = new Term("word2");
        Term word3 = new Term("word3");
        Term word4 = new Term("word4");

        CategoryEntries ces1 = new CategoryEntries();

        CategoryEntry ce1 = new CategoryEntry(ces1, c1, 443);
        CategoryEntry ce2 = new CategoryEntry(ces1, c2, 100);

        ces1.add(ce1);
        ces1.add(ce2);

        System.out.println(ces1);

        // test a dictionary
        // word c1 c2
        // word1 66 => rel(word1,c1) = 100%
        // word2 2 => rel(word2,c2) = 100%
        // word3 18 6 => rel(word3,c1) = 75%, rel(word3,c2) = 25%
        // word4 11 => rel(word4,c2) = 100%
        // ------------------------------
        // documents 2 3 5 => prior(c1) = 2/5, prior(c2) = 3/5
        // weights 84 19 103 => e.g. cweight(word1,word2,c1) = 66/84, cweight(word1,word3,c1) = 84/84
        Dictionary dictionary = new Dictionary("testDictionary", ClassificationTypeSetting.SINGLE);
        dictionary.updateWord(word1, c1, 12);
        dictionary.updateWord(word2, c2, 2);
        dictionary.updateWord(word1, c1, 54);
        dictionary.updateWord(word3, c1, 18);
        dictionary.updateWord(word3, c2, 6);
        dictionary.updateWord(word4, c2, 8);
        dictionary.updateWord(word4, c2, 2);
        dictionary.updateWord(word4, c2, 1);
        dictionary.calculateCategoryPriors();

        // check priors
        assertEquals(0.4, dictionary.get(word1).getCategoryEntry("category1").getCategory().getPrior());
        assertEquals(0.6, dictionary.get(word4).getCategoryEntry("category2").getCategory().getPrior());

        // check dictionary
        assertEquals(1.0, dictionary.get(word1).getCategoryEntry("category1").getRelevance());
        assertEquals(66.0, dictionary.get(word1).getCategoryEntry("category1").getAbsoluteRelevance());
        assertEquals(1.0, dictionary.get(word2).getCategoryEntry("category2").getRelevance());
        assertEquals(0.75, dictionary.get(word3).getCategoryEntry("category1").getRelevance());
        assertEquals(0.25, dictionary.get(word3).getCategoryEntry("category2").getRelevance());
        assertEquals(1.0, dictionary.get(word4).getCategoryEntry("category2").getRelevance());

        // check term weights
        CategoryEntries ces = new CategoryEntries();
        ces.add(dictionary.get(word1).getCategoryEntry("category1"));
        ces.add(dictionary.get(word2).getCategoryEntry("category1"));
        assertEquals(66.0 / 84.0, ces.getTermWeight(dictionary.get(word1).getCategoryEntry("category1").getCategory()));

        ces = new CategoryEntries();
        ces.add(dictionary.get(word1).getCategoryEntry("category1"));
        ces.add(dictionary.get(word3).getCategoryEntry("category1"));
        assertEquals(1.0, ces.getTermWeight(dictionary.get(word1).getCategoryEntry("category1").getCategory()));

        assertEquals(1.0, dictionary.get(word1).getCategoryEntry("category1").getRelevance());
        assertEquals(66.0, dictionary.get(word1).getCategoryEntry("category1").getAbsoluteRelevance());

        System.out.println(dictionary);

    }
}