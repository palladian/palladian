package ws.palladian.classification.text;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.classification.text.LingPipeTextClassifier.LingPipeTextClassifierModel;
import ws.palladian.classification.text.evaluation.TextDatasetIterator;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.integrationtests.ITHelper;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenFeatureExtractor;

/**
 * <p>
 * "Integration Test" for the {@link OpenNlpTextClassifier}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class LingPipeTextClassifierIT {

    /** The configuration with the paths to the datasets. */
    private static PropertiesConfiguration config;

    @BeforeClass
    public static void ignition() throws ConfigurationException {
        try {
            config = new PropertiesConfiguration(ResourceHelper.getResourceFile("/palladian-test.properties"));
        } catch (FileNotFoundException e) {
            fail("palladian-test.properties not found; test is skipped!");
        }
        ITHelper.assertMemory(750, SizeUnit.MEGABYTES);
    }

    @After
    public void cleanup() {
        ITHelper.forceGc();
    }

    @Test
    public void testJrc() {
        String trainFile = config.getString("dataset.jrc.train");
        String testFile = config.getString("dataset.jrc.test");
        checkExistence("JRC", testFile, trainFile);
        assertAccuracy(trainFile, testFile, 1);
    }

    @Test
    public void testWikipedia() {
        String trainFile = config.getString("dataset.wikipedia.train");
        String testFile = config.getString("dataset.wikipedia.test");
        checkExistence("Wikipedia", testFile, trainFile);
        assertAccuracy(trainFile, testFile, 0.98);
    }

    @Test
    public void test20Newsgroups() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        checkExistence("20 Newsgroups", testFile, trainFile);
        assertAccuracy(trainFile, testFile, 0.88);
    }

    @Test
    public void testSpamAssassin() {
        String trainFile = config.getString("dataset.spamassassin.train");
        String testFile = config.getString("dataset.spamassassin.test");
        checkExistence("SpamAssassin", trainFile, testFile);
        assertAccuracy(trainFile, testFile, 0.99);
    }

    @Test
    public void testImdb() {
        String trainFile = config.getString("dataset.imdb.train");
        String testFile = config.getString("dataset.imdb.test");
        checkExistence("IMDB", trainFile, testFile);
        assertAccuracy(trainFile, testFile, 0.68);
    }

    /**
     * <p>
     * Use the training set, train a classifier, check accuracy on test set.
     * </p>
     * 
     * @param trainFile The training data.
     * @param testFile The testing data.
     * @param minAccuracy The minimum expected accuracy on the test data.
     */
    private static void assertAccuracy(String trainFile, String testFile, double minAccuracy) {
        LingPipeTextClassifier classifier = new LingPipeTextClassifier(new TokenFeatureExtractor(
                IndoEuropeanTokenizerFactory.INSTANCE));
        TextDatasetIterator trainIterator = new TextDatasetIterator(trainFile, " ", true);
        LingPipeTextClassifierModel model = classifier.train(trainIterator);
        TextDatasetIterator testIterator = new TextDatasetIterator(testFile, " ", true);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, testIterator, model);
        System.out.println("accuracy on " + testFile + " : " + evaluation.getAccuracy());
        assertTrue("expected accuracy: " + minAccuracy + ", actual accuracy: " + evaluation.getAccuracy(),
                evaluation.getAccuracy() >= minAccuracy);
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
            assumeTrue("Dataset for '" + datasetName
                    + "' is missing, test is skipped. Adjust palladian-test.properties to set the correct paths.",
                    false);
        }
    }

}
