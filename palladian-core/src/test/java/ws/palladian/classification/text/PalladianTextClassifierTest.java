package ws.palladian.classification.text;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.classification.text.evaluation.TextDatasetIterator;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.helper.math.ConfusionMatrix;

public class PalladianTextClassifierTest {

    private static final String JRC_TRAIN_FILE = "/Users/pk/Dropbox/Uni/Datasets/Wikipedia76Languages/languageDocumentIndex_random1000_train.txt";
    private static final String JRC_TEST_FILE = "/Users/pk/Dropbox/Uni/Datasets/Wikipedia76Languages/languageDocumentIndex_random1000_test.txt";
    private static final String WIKIPEDIA_TRAIN_FILE = "/Users/pk/Dropbox/Uni/Datasets/Wikipedia76Languages/languageDocumentIndex_random1000_train.txt";
    private static final String WIKIPEDIA_TEST_FILE = "/Users/pk/Dropbox/Uni/Datasets/Wikipedia76Languages/languageDocumentIndex_random1000_test.txt";
    private static final String TWENTY_NEWSGROUPS_1 = "/Users/pk/Dropbox/Uni/Datasets/20newsgroups-18828/index_split1.txt";
    private static final String TWENTY_NEWSGROUPS_2 = "/Users/pk/Dropbox/Uni/Datasets/20newsgroups-18828/index_split2.txt";

    // private static final String JRC_TRAIN_FILE =
    // "C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_train.txt";
    // private static final String JRC_TEST_FILE =
    // "C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_test.txt";
    // private static final String WIKIPEDIA_TRAIN_FILE =
    // "C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_train.txt";
    // private static final String WIKIPEDIA_TEST_FILE =
    // "C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_test.txt";
    // private static final String TWENTY_NEWSGROUPS_1 = "C:\\Workspace\\data\\20newsgroups-18828\\index_split1.txt";
    // private static final String TWENTY_NEWSGROUPS_2 = "C:\\Workspace\\data\\20newsgroups-18828\\index_split2.txt";

    @Test
    public void testDictionaryClassifierCharJrc() {

        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        featureSetting.setMaxTerms(1000);
        featureSetting.setMinNGramLength(3);
        featureSetting.setMaxNGramLength(6);
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);

        Dataset trainset = new Dataset("JRC");
        trainset.setFirstFieldLink(true);
        trainset.setSeparationString(" ");
        trainset.setPath(JRC_TRAIN_FILE);

        Dataset testset = new Dataset("JRC");
        testset.setFirstFieldLink(true);
        testset.setSeparationString(" ");
        testset.setPath(JRC_TEST_FILE);

        TextDatasetIterator trainIterator = TextDatasetIterator.createIterator(trainset);
        DictionaryModel model = classifier.train(trainIterator);
        
        TextDatasetIterator testIterator = TextDatasetIterator.createIterator(testset);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, model, testIterator);

        System.out.println("accuracy char jrc: " + evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() >= 0.983);
    }

    @Test
    public void testDictionaryClassifierWordJrc() {

        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        featureSetting.setMaxTerms(10);
        featureSetting.setMinNGramLength(1);
        featureSetting.setMaxNGramLength(3);
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);

        Dataset trainset = new Dataset("JRC");
        trainset.setFirstFieldLink(true);
        trainset.setSeparationString(" ");
        trainset.setPath(WIKIPEDIA_TRAIN_FILE);

        Dataset testset = new Dataset("JRC");
        testset.setFirstFieldLink(true);
        testset.setSeparationString(" ");
        testset.setPath(WIKIPEDIA_TEST_FILE);

        TextDatasetIterator trainIterator = TextDatasetIterator.createIterator(trainset);
        DictionaryModel model = classifier.train(trainIterator);
        
        TextDatasetIterator testIterator = TextDatasetIterator.createIterator(testset);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, model, testIterator);

        System.out.println("accuracy word jrc: " + evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() >= 0.935);
    }

    @Test
    public void testDictionaryClassifierCharNg() {

        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        featureSetting.setMaxTerms(1000);
        featureSetting.setMinNGramLength(3);
        featureSetting.setMaxNGramLength(6);
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);

        Dataset trainset = new Dataset("JRC");
        trainset.setFirstFieldLink(true);
        trainset.setSeparationString(" ");
        trainset.setPath(TWENTY_NEWSGROUPS_1);

        Dataset testset = new Dataset("JRC");
        testset.setFirstFieldLink(true);
        testset.setSeparationString(" ");
        testset.setPath(TWENTY_NEWSGROUPS_2);

        TextDatasetIterator trainIterator = TextDatasetIterator.createIterator(trainset);
        DictionaryModel model = classifier.train(trainIterator);
        
        TextDatasetIterator testIterator = TextDatasetIterator.createIterator(testset);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, model, testIterator);
        
        System.out.println("accuracy char ng: " + evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() >= 0.8893436410489617); // 0.8882825526754585
    }

    @Test
    public void testDictionaryClassifierWordNg() {

        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        featureSetting.setMaxTerms(10);
        featureSetting.setMinNGramLength(1);
        featureSetting.setMaxNGramLength(3);
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);

        Dataset trainset = new Dataset("JRC");
        trainset.setFirstFieldLink(true);
        trainset.setSeparationString(" ");
        trainset.setPath(TWENTY_NEWSGROUPS_1);

        Dataset testset = new Dataset("JRC");
        testset.setFirstFieldLink(true);
        testset.setSeparationString(" ");
        testset.setPath(TWENTY_NEWSGROUPS_2);

        TextDatasetIterator trainIterator = TextDatasetIterator.createIterator(trainset);
        DictionaryModel model = classifier.train(trainIterator);
        
        TextDatasetIterator testIterator = TextDatasetIterator.createIterator(testset);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, model, testIterator);

        System.out.println("accuracy word ng: " + evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() >= 0.6030013642564802); // 0.17735334242837653
    }


}
