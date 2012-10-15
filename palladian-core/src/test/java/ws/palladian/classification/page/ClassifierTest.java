package ws.palladian.classification.page;


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
    // private static final Logger LOGGER = Logger.getLogger(ClassifierTest.class);

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
     * FIXME
     * 
     * @throws FileNotFoundException
     */
    // @Test
    // public void testRegressionTextClassifier() throws FileNotFoundException {
    //
    // // specify the dataset that should be used as training data
    // Dataset dataset = new Dataset();
    //
    // // set the path to the dataset
    // dataset.setPath(ResourceHelper.getResourcePath("/classifier/index_learning.txt"));
    //
    // // tell the preprocessor that the first field in the file is a link to the actual document
    // dataset.setFirstFieldLink(true);
    //
    // // create a text classifier by giving a name and a path where it should be saved to
    // DictionaryClassifier classifier = new DictionaryClassifier();
    //
    // // specify the settings for the classification
    // ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();
    //
    // // we use only a single category per document
    // classificationTypeSetting.setClassificationType(ClassificationTypeSetting.REGRESSION);
    //
    // // we want the classifier to be serialized in the end
    // classificationTypeSetting.setSerializeClassifier(false);
    //
    // // specify feature settings that should be used by the classifier
    // FeatureSetting featureSetting = new FeatureSetting();
    //
    // // we want to create character-level n-grams
    // featureSetting.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
    //
    // // the minimum length of our n-grams should be 1
    // featureSetting.setMinNGramLength(1);
    //
    // // the maximum length of our n-grams should be 1
    // featureSetting.setMaxNGramLength(1);
    //
    // // terms can be one char
    // featureSetting.setMinimumTermLength(1);
    //
    // // we assign the settings to our classifier
    // classifier.setClassificationTypeSetting(classificationTypeSetting);
    // classifier.setFeatureSetting(featureSetting);
    //
    // // now we can train the classifier using the given dataset
    // classifierManager.trainClassifier(dataset, classifier);
    //
    // // test different documents
    // TextInstance classifiedDocument;
    //
    // classifiedDocument = classifier.classify("a");
    // assertEquals("1.0", classifiedDocument.getMainCategoryEntry().getCategory().getName());
    //
    // classifiedDocument = classifier.classify("b");
    // assertEquals("5.5", classifiedDocument.getMainCategoryEntry().getCategory().getName());
    //
    // // 1/3 * 1 + 1/3 * 4 + 1/3 * 10 = 5
    // classifiedDocument = classifier.classify("c");
    // assertEquals("5.0", classifiedDocument.getMainCategoryEntry().getCategory().getName());
    //
    // // that is kind of experimental since the calculation uses squared relevances that might not apply for
    // // regression
    // classifiedDocument = classifier.classify("a c");
    // assertEquals("1.9999999999999996", classifiedDocument.getMainCategoryEntry().getCategory().getName());
    // }

}