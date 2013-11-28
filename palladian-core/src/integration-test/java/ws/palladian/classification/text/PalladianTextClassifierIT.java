package ws.palladian.classification.text;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.classification.text.FeatureSetting.TextFeatureType;
import ws.palladian.classification.text.evaluation.TextDatasetIterator;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.constants.SizeUnit;
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

    /** The configuration with the paths to the datasets. */
    private static PropertiesConfiguration config;

    @BeforeClass
    public static void ignition() throws ConfigurationException {
        try {
            config = new PropertiesConfiguration(ResourceHelper.getResourceFile("/palladian-test.properties"));
        } catch (FileNotFoundException e) {
            fail("palladian-test.properties not found; test is skipped!");
        }
        // make sure, we have enough heap
        if (ProcessHelper.getFreeMemory() < SizeUnit.MEGABYTES.toBytes(750)) {
            fail("Not enough memory. This test requires at least 1 GB heap memory.");
        }
    }

    @Test
    public void testJrcChar() {
        String trainFile = config.getString("dataset.jrc.train");
        String testFile = config.getString("dataset.jrc.test");
        checkExistence("JRC", testFile, trainFile);

        FeatureSetting featureSetting = new FeatureSetting(TextFeatureType.CHAR_NGRAMS, 3, 6, 1000);
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);

        TextDatasetIterator trainIterator = new TextDatasetIterator(trainFile, " ", true);
        DictionaryModel model = classifier.train(trainIterator);

        TextDatasetIterator testIterator = new TextDatasetIterator(testFile, " ", true);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, testIterator, model);

        System.out.println("accuracy char jrc: " + evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() >= 0.99);
    }

    @Test
    public void testWikipediaWord() {
        String trainFile = config.getString("dataset.wikipedia.train");
        String testFile = config.getString("dataset.wikipedia.test");
        checkExistence("Wikipedia", testFile, trainFile);

        FeatureSetting featureSetting = new FeatureSetting(TextFeatureType.WORD_NGRAMS, 1, 3, 10);
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);

        TextDatasetIterator trainIterator = new TextDatasetIterator(trainFile, " ", true);
        DictionaryModel model = classifier.train(trainIterator);

        TextDatasetIterator testIterator = new TextDatasetIterator(testFile, " ", true);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, testIterator, model);

        System.out.println("accuracy word jrc: " + evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() >= 0.98);
    }

    @Test
    public void test20NewsgroupsChar() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        checkExistence("20 Newsgroups", testFile, trainFile);

        FeatureSetting featureSetting = new FeatureSetting(TextFeatureType.CHAR_NGRAMS, 3, 6, 1000);
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);

        TextDatasetIterator trainIterator = new TextDatasetIterator(trainFile, " ", true);
        DictionaryModel model = classifier.train(trainIterator);

        TextDatasetIterator testIterator = new TextDatasetIterator(testFile, " ", true);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, testIterator, model);

        System.out.println("accuracy char ng: " + evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() >= 0.88);
    }

    @Test
    public void test20NewsgroupsWord() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        checkExistence("20 Newsgroups", testFile, trainFile);

        FeatureSetting featureSetting = new FeatureSetting(TextFeatureType.WORD_NGRAMS, 1, 3, 10);
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);

        TextDatasetIterator trainIterator = new TextDatasetIterator(trainFile, " ", true);
        DictionaryModel model = classifier.train(trainIterator);

        TextDatasetIterator testIterator = new TextDatasetIterator(testFile, " ", true);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, testIterator, model);

        System.out.println("accuracy word ng: " + evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() >= 0.9);
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
            fail("Dataset for '" + datasetName + "' is missing, test is skipped. Adjust palladian-test.properties to set the correct paths.");
        }
    }

}
