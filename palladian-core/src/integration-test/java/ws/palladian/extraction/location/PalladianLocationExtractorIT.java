package ws.palladian.extraction.location;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguationLearner;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.disambiguation.LocationDisambiguation;
import ws.palladian.extraction.location.evaluation.LocationExtractionEvaluator;
import ws.palladian.extraction.location.sources.NewsSeecrLocationSource;
import ws.palladian.helper.io.ResourceHelper;

public class PalladianLocationExtractorIT {
    // XXX move to properties
    private static final String MASHAPE_TEST_KEY = "tr1dn3mc0bdhzzjngkvzahqloxph0e";
    /** Path to the training data. */
    private static String trainDataSet;
    /** Path to the test data. */
    private static String testDataSet;

    @BeforeClass
    public static void readConfiguration() throws ConfigurationException {
        try {
            PropertiesConfiguration config = new PropertiesConfiguration(
                    ResourceHelper.getResourceFile("/palladian-test.properties"));
            trainDataSet = config.getString("dataset.tudloc2013.train");
            testDataSet = config.getString("dataset.tudloc2013.test");
            assumeDirectory(trainDataSet, testDataSet);
        } catch (FileNotFoundException e) {
            fail("palladian-test.properties not found; test is skipped!");
        }
    }

    /**
     * Make sure, all given paths are pointing to directories.
     * 
     * @param paths
     */
    private static void assumeDirectory(String... paths) {
        for (String path : paths) {
            assumeTrue(path + " not present", new File(path).isDirectory());
        }
    }

    @Test
    @Ignore
    public void testPalladianLocationExtractor_Heuristic() {
        LocationSource locationSource = new NewsSeecrLocationSource(MASHAPE_TEST_KEY);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, disambiguation);
//        LocationExtractor extractor = new NewsSeecrLocationExtractor(MASHAPE_TEST_KEY);
        LocationExtractionEvaluator.run(extractor, new File(testDataSet), true);
    }
    
    @Test
    public void testPalladianLocationExtractor_FeatureBased() throws IOException {
        LocationSource locationSource = new NewsSeecrLocationSource(MASHAPE_TEST_KEY);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource);
        learner.learn(new File(trainDataSet));
    }
}
