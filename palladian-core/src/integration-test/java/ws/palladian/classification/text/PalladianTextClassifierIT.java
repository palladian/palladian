package ws.palladian.classification.text;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.evaluation.TextDatasetIterator;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * <p>
 * "Integration Test" for the {@link PalladianTextClassifier}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class PalladianTextClassifierIT {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianTextClassifierIT.class);

    /** The configuration with the paths to the datasets. */
    private static PropertiesConfiguration config;

    @BeforeClass
    public static void loadConfig() throws ConfigurationException {
        try {
            config = new PropertiesConfiguration(ResourceHelper.getResourceFile("/palladian-test.properties"));
        } catch (FileNotFoundException e) {
            LOGGER.warn("palladian-test.properties not found; test is skipped!");
            assumeTrue(false);
        }
    }

    @Test
    public void testJrcChar() {
        String trainFile = config.getString("dataset.jrc.train");
        String testFile = config.getString("dataset.jrc.test");
        checkExistence("JRC", testFile, trainFile);

        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        featureSetting.setMaxTerms(1000);
        featureSetting.setMinNGramLength(3);
        featureSetting.setMaxNGramLength(6);
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);

        TextDatasetIterator trainIterator = TextDatasetIterator.createIterator(trainFile, " ", true);
        DictionaryModel model = classifier.train(trainIterator);

        TextDatasetIterator testIterator = TextDatasetIterator.createIterator(testFile, " ", true);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, model, testIterator);

        System.out.println("accuracy char jrc: " + evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() >= 0.983);
    }

    @Test
    public void testWikipediaWord() {
        String trainFile = config.getString("dataset.wikipedia.train");
        String testFile = config.getString("dataset.wikipedia.test");
        checkExistence("Wikipedia", testFile, trainFile);

        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        featureSetting.setMaxTerms(10);
        featureSetting.setMinNGramLength(1);
        featureSetting.setMaxNGramLength(3);
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);

        TextDatasetIterator trainIterator = TextDatasetIterator.createIterator(trainFile, " ", true);
        DictionaryModel model = classifier.train(trainIterator);

        TextDatasetIterator testIterator = TextDatasetIterator.createIterator(testFile, " ", true);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, model, testIterator);

        System.out.println("accuracy word jrc: " + evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() >= 0.935);
    }

    @Test
    public void test20NewsgroupsChar() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        checkExistence("20 Newsgroups", testFile, trainFile);

        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        featureSetting.setMaxTerms(1000);
        featureSetting.setMinNGramLength(3);
        featureSetting.setMaxNGramLength(6);
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);

        TextDatasetIterator trainIterator = TextDatasetIterator.createIterator(trainFile, " ", true);
        DictionaryModel model = classifier.train(trainIterator);

        TextDatasetIterator testIterator = TextDatasetIterator.createIterator(testFile, " ", true);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, model, testIterator);

        System.out.println("accuracy char ng: " + evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() >= 0.8893436410489617); // 0.8882825526754585
    }

    @Test
    public void test20NewsgroupsWord() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        checkExistence("20 Newsgroups", testFile, trainFile);

        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        featureSetting.setMaxTerms(10);
        featureSetting.setMinNGramLength(1);
        featureSetting.setMaxNGramLength(3);
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);

        TextDatasetIterator trainIterator = TextDatasetIterator.createIterator(trainFile, " ", true);
        DictionaryModel model = classifier.train(trainIterator);

        TextDatasetIterator testIterator = TextDatasetIterator.createIterator(testFile, " ", true);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, model, testIterator);

        System.out.println("accuracy word ng: " + evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() >= 0.6030013642564802); // 0.17735334242837653
    }

    /**
     * <p>
     * Verify that the given files with datasets exist. If not, output a warning message and skip the test.
     * </p>
     * 
     * @param datasetName The name of the dataset, used for log output in case the files do not exist.
     * @param filePaths The paths whose existence to verify.
     */
    private static void checkExistence(String datasetName, String... filePaths) {
        boolean runTest = true;
        for (String filePath : filePaths) {
            if (filePath == null || !new File(filePath).isFile()) {
                runTest = false;
                break;
            }
        }
        if (!runTest) {
            LOGGER.warn(
                    "Dataset for {} is missing, test is skipped. Adjust palladian-test.properties to set the correct paths.",
                    datasetName);
        }
        assumeTrue(runTest);
    }

}
