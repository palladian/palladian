package ws.palladian.classification.text;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static ws.palladian.classification.text.BayesScorer.Options.COMPLEMENT;
import static ws.palladian.classification.text.BayesScorer.Options.LAPLACE;
import static ws.palladian.classification.text.BayesScorer.Options.PRIORS;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.classification.text.PalladianTextClassifier.DefaultScorer;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
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
    
//    static {
//        PalladianTextClassifier.learnCounts = true;
//    }

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
    
    @After
    public void cleanup() {
        // make sure, garbage collector runs
        Object obj = new Object();
        WeakReference<Object> ref = new WeakReference<Object>(obj);
        obj = null;
        while (ref.get() != null) {
            System.gc();
        }
    }

    @Test
    public void testJrcChar() {
        String trainFile = config.getString("dataset.jrc.train");
        String testFile = config.getString("dataset.jrc.test");
        checkExistence("JRC", testFile, trainFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(3, 6).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.99, new DefaultScorer());
    }

    @Test
    public void testWikipediaWord() {
        String trainFile = config.getString("dataset.wikipedia.train");
        String testFile = config.getString("dataset.wikipedia.test");
        checkExistence("Wikipedia", testFile, trainFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).maxTerms(10).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.99, new DefaultScorer());
    }

    @Test
    public void test20NewsgroupsChar() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        checkExistence("20 Newsgroups", testFile, trainFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(3, 6).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.89, new DefaultScorer());
    }

    @Test
    public void test20NewsgroupsChar_Bayes() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        checkExistence("20 Newsgroups", testFile, trainFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(3, 6).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.90, new BayesScorer(LAPLACE, PRIORS, COMPLEMENT));
    }

    @Test
    public void test20NewsgroupsWord() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        checkExistence("20 Newsgroups", testFile, trainFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).maxTerms(10).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.81, new DefaultScorer());
    }
    
    @Test
    public void test20NewsgroupsWord_Bayes() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        checkExistence("20 Newsgroups", testFile, trainFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).maxTerms(10).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.81, new BayesScorer(LAPLACE, PRIORS, COMPLEMENT));
    }

    @Test
    public void testSpamAssassinChar() {
        String trainFile = config.getString("dataset.spamassassin.train");
        String testFile = config.getString("dataset.spamassassin.test");
        checkExistence("SpamAssassin", trainFile, testFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(6).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.87, new DefaultScorer());
    }
    
//    @Test
//    public void testSpamAssassinChar_categoryEqualization() {
//        String trainFile = config.getString("dataset.spamassassin.train");
//        String testFile = config.getString("dataset.spamassassin.test");
//        checkExistence("SpamAssassin", trainFile, testFile);
//        FeatureSetting featureSetting = FeatureSettingBuilder.chars(6).maxTerms(1000).create();
//        assertAccuracy(trainFile, testFile, featureSetting, 0.98, new PalladianTextClassifier.CategoryEqualizationScorer());
//    }
    
    @Test
    public void testSpamAssassinChar_BayesScorer() {
        String trainFile = config.getString("dataset.spamassassin.train");
        String testFile = config.getString("dataset.spamassassin.test");
        checkExistence("SpamAssassin", trainFile, testFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(6).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.97, new BayesScorer(LAPLACE, PRIORS, COMPLEMENT));
    }
    
    @Test
    public void testImdbWord_PalladianScorer() {
        String trainFile = config.getString("dataset.imdb.train");
        String testFile = config.getString("dataset.imdb.test");
        checkExistence("IMDB", trainFile, testFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.74, new DefaultScorer());
    }
    
    @Test
    public void testImdbWord_BayesScorer() {
        String trainFile = config.getString("dataset.imdb.train");
        String testFile = config.getString("dataset.imdb.test");
        checkExistence("IMDB", trainFile, testFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.76, new BayesScorer(LAPLACE, PRIORS, COMPLEMENT));
    }

    /**
     * <p>
     * Use the training set, train a classifier, check accuracy on test set.
     * </p>
     * 
     * @param trainFile The training data.
     * @param testFile The testing data.
     * @param featureSetting The feature setting for the classifier.
     * @param scorer The scorer to use, <code>null</code> means {@link DefaultScorer}.
     * @param minAccuracy The minimum expected accuracy on the test data.
     */
    private static void assertAccuracy(String trainFile, String testFile, FeatureSetting featureSetting,
            double minAccuracy, Scorer scorer) {
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting, scorer);
        TextDatasetIterator trainIterator = new TextDatasetIterator(trainFile, " ", true);
        DictionaryModel model = classifier.train(trainIterator);
        TextDatasetIterator testIterator = new TextDatasetIterator(testFile, " ", true);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(classifier, testIterator, model);
        System.out.println("accuracy with " + featureSetting + " and " + scorer + " on " + testFile + " : "
                + evaluation.getAccuracy());
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
