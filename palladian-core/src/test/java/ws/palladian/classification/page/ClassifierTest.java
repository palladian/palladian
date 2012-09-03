package ws.palladian.classification.page;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Dictionary;
import ws.palladian.classification.Term;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.classification.page.evaluation.FeatureSetting;
import ws.palladian.helper.io.ResourceHelper;

/**
 * <p>
 * Tests the correct functionality of the classifiers provided by Palladian.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
public class ClassifierTest {

    /**
     * <p>
     * The logger for objects of this class.
     * </p>
     */
    private static final Logger LOGGER = Logger.getLogger(ClassifierTest.class);

    /**
     * <p>
     * Build a simple dictionary of 4 documents and test regression:
     * </p>
     * 
     * <pre>
     * document (class/value)    | words
     * 1 (1)                     | a b c
     * 2 (4)                     |     c d e
     * 3 (5)                     |          f g h
     * 4 (10)                    |   b c          i
     * </pre>
     * 
     * @throws FileNotFoundException
     */
    @Test
    public void testRegressionTextClassifier() throws FileNotFoundException {

        // create a classifier mananger object
        ClassifierManager classifierManager = new ClassifierManager();

        // specify the dataset that should be used as training data
        Dataset dataset = new Dataset();

        // set the path to the dataset
        dataset.setPath(ResourceHelper.getResourcePath("/classifier/index_learning.txt"));

        // tell the preprocessor that the first field in the file is a link to the actual document
        dataset.setFirstFieldLink(true);

        // create a text classifier by giving a name and a path where it should be saved to
        TextClassifier classifier = new DictionaryClassifier();

        // specify the settings for the classification
        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();

        // we use only a single category per document
        classificationTypeSetting.setClassificationType(ClassificationTypeSetting.REGRESSION);

        // we want the classifier to be serialized in the end
        classificationTypeSetting.setSerializeClassifier(false);

        // specify feature settings that should be used by the classifier
        FeatureSetting featureSetting = new FeatureSetting();

        // we want to create character-level n-grams
        featureSetting.setTextFeatureType(FeatureSetting.WORD_NGRAMS);

        // the minimum length of our n-grams should be 1
        featureSetting.setMinNGramLength(1);

        // the maximum length of our n-grams should be 1
        featureSetting.setMaxNGramLength(1);

        // terms can be one char
        featureSetting.setMinimumTermLength(1);

        // we assign the settings to our classifier
        classifier.setClassificationTypeSetting(classificationTypeSetting);
        classifier.setFeatureSetting(featureSetting);

        // now we can train the classifier using the given dataset
        classifierManager.trainClassifier(dataset, classifier);

        // test different documents
        TextInstance classifiedDocument;

        classifiedDocument = classifier.classify("a");
        Assert.assertEquals("1.0", classifiedDocument.getMainCategoryEntry().getCategory().getName());

        classifiedDocument = classifier.classify("b");
        Assert.assertEquals("5.5", classifiedDocument.getMainCategoryEntry().getCategory().getName());

        // 1/3 * 1 + 1/3 * 4 + 1/3 * 10 = 5
        classifiedDocument = classifier.classify("c");
        Assert.assertEquals("5.0", classifiedDocument.getMainCategoryEntry().getCategory().getName());

        // that is kind of experimental since the calculation uses squared relevances that might not apply for
        // regression
        classifiedDocument = classifier.classify("a c");
        Assert.assertEquals("1.9999999999999996", classifiedDocument.getMainCategoryEntry().getCategory().getName());
    }

    @Test
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
        assertEquals(0.75, c1.getPrior(), 0);
        assertEquals(0.25, c2.getPrior(), 0);

        LOGGER.info(categories);

        Term word1 = new Term("word1");
        Term word2 = new Term("word2");
        Term word3 = new Term("word3");
        Term word4 = new Term("word4");

        CategoryEntries ces1 = new CategoryEntries();

        CategoryEntry ce1 = new CategoryEntry(ces1, c1, 443);
        CategoryEntry ce2 = new CategoryEntry(ces1, c2, 100);

        ces1.add(ce1);
        ces1.add(ce2);

        LOGGER.info(ces1);

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
        assertEquals(0.4, dictionary.get(word1).getCategoryEntry("category1").getCategory().getPrior(), 0);
        assertEquals(0.6, dictionary.get(word4).getCategoryEntry("category2").getCategory().getPrior(), 0);

        // check dictionary
        assertEquals(1.0, dictionary.get(word1).getCategoryEntry("category1").getRelevance(), 0);
        assertEquals(66.0, dictionary.get(word1).getCategoryEntry("category1").getAbsoluteRelevance(), 0);
        assertEquals(1.0, dictionary.get(word2).getCategoryEntry("category2").getRelevance(), 0);
        assertEquals(0.75, dictionary.get(word3).getCategoryEntry("category1").getRelevance(), 0);
        assertEquals(0.25, dictionary.get(word3).getCategoryEntry("category2").getRelevance(), 0);
        assertEquals(1.0, dictionary.get(word4).getCategoryEntry("category2").getRelevance(), 0);

        // check term weights
        CategoryEntries ces = new CategoryEntries();
        ces.add(dictionary.get(word1).getCategoryEntry("category1"));
        ces.add(dictionary.get(word2).getCategoryEntry("category1"));
        assertEquals(66.0 / 84.0, ces.getTermWeight(dictionary.get(word1).getCategoryEntry("category1").getCategory()),
                0);

        ces = new CategoryEntries();
        ces.add(dictionary.get(word1).getCategoryEntry("category1"));
        ces.add(dictionary.get(word3).getCategoryEntry("category1"));
        assertEquals(1.0, ces.getTermWeight(dictionary.get(word1).getCategoryEntry("category1").getCategory()), 0);

        assertEquals(1.0, dictionary.get(word1).getCategoryEntry("category1").getRelevance(), 0);
        assertEquals(66.0, dictionary.get(word1).getCategoryEntry("category1").getAbsoluteRelevance(), 0);

        LOGGER.info(dictionary);

    }
}