package ws.palladian.classification.text;

import static org.junit.Assert.assertTrue;
import static ws.palladian.classification.text.BayesScorer.Options.COMPLEMENT;
import static ws.palladian.classification.text.BayesScorer.Options.LAPLACE;
import static ws.palladian.classification.text.BayesScorer.Options.PRIORS;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.classification.text.PalladianTextClassifier.DefaultScorer;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.classification.text.evaluation.TextDatasetIterator;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.integrationtests.ITHelper;

/**
 * <p>
 * "Integration Test" for the {@link PalladianTextClassifier}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class PalladianTextClassifierIT {

    /** The configuration with the paths to the datasets. */
    private static Configuration config;

    @BeforeClass
    public static void ignition() throws ConfigurationException {
        config = ITHelper.getTestConfig();
        ITHelper.assertMemory(750, SizeUnit.MEGABYTES);
    }

    @After
    public void cleanup() {
        ITHelper.forceGc();
    }

    @Test
    public void testJrcChar() {
        String trainFile = config.getString("dataset.jrc.train");
        String testFile = config.getString("dataset.jrc.test");
        ITHelper.assumeExistence("JRC", testFile, trainFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(3, 6).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.99, new DefaultScorer());
    }

    @Test
    public void testWikipediaWord() {
        String trainFile = config.getString("dataset.wikipedia.train");
        String testFile = config.getString("dataset.wikipedia.test");
        ITHelper.assumeExistence("Wikipedia", testFile, trainFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).maxTerms(10).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.99, new DefaultScorer());
    }

    @Test
    public void test20NewsgroupsChar() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        ITHelper.assumeExistence("20 Newsgroups", testFile, trainFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(3, 6).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.89, new DefaultScorer());
    }

    @Test
    public void test20NewsgroupsChar_Bayes() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        ITHelper.assumeExistence("20 Newsgroups", testFile, trainFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(3, 6).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.90, new BayesScorer(LAPLACE, PRIORS, COMPLEMENT));
    }

    @Test
    public void test20NewsgroupsWord() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        ITHelper.assumeExistence("20 Newsgroups", testFile, trainFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).maxTerms(10).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.81, new DefaultScorer());
    }

    @Test
    public void test20NewsgroupsWord_Bayes() {
        String trainFile = config.getString("dataset.20newsgroups.split1");
        String testFile = config.getString("dataset.20newsgroups.split2");
        ITHelper.assumeExistence("20 Newsgroups", testFile, trainFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).maxTerms(10).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.81, new BayesScorer(LAPLACE, PRIORS, COMPLEMENT));
    }

    @Test
    public void testSpamAssassinChar() {
        String trainFile = config.getString("dataset.spamassassin.train");
        String testFile = config.getString("dataset.spamassassin.test");
        ITHelper.assumeExistence("SpamAssassin", trainFile, testFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(6).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.87, new DefaultScorer());
    }

    @Test
    public void testSpamAssassinChar_categoryEqualization() {
        String trainFile = config.getString("dataset.spamassassin.train");
        String testFile = config.getString("dataset.spamassassin.test");
        ITHelper.assumeExistence("SpamAssassin", trainFile, testFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(6).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.98, new ExperimentalScorers.CategoryEqualizationScorer());
    }

    @Test
    public void testSpamAssassinChar_BayesScorer() {
        String trainFile = config.getString("dataset.spamassassin.train");
        String testFile = config.getString("dataset.spamassassin.test");
        ITHelper.assumeExistence("SpamAssassin", trainFile, testFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(6).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.97, new BayesScorer(LAPLACE, PRIORS, COMPLEMENT));
    }

    @Test
    public void testImdbWord_PalladianScorer() {
        String trainFile = config.getString("dataset.imdb.train");
        String testFile = config.getString("dataset.imdb.test");
        ITHelper.assumeExistence("IMDB", trainFile, testFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).maxTerms(1000).create();
        assertAccuracy(trainFile, testFile, featureSetting, 0.74, new DefaultScorer());
    }

    @Test
    public void testImdbWord_BayesScorer() {
        String trainFile = config.getString("dataset.imdb.train");
        String testFile = config.getString("dataset.imdb.test");
        ITHelper.assumeExistence("IMDB", trainFile, testFile);
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
    public static void assertAccuracy(String trainFile, String testFile, FeatureSetting featureSetting,
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

}
