package ws.palladian.extraction.location;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguationLearner;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.disambiguation.LocationDisambiguation;
import ws.palladian.extraction.location.evaluation.LocationExtractionEvaluator;
import ws.palladian.extraction.location.evaluation.LocationExtractionEvaluator.LocationEvaluationResult;
import ws.palladian.extraction.location.sources.NewsSeecrLocationSource;
import ws.palladian.helper.io.ResourceHelper;

public class PalladianLocationExtractorIT {
    /** Mashape API key for accessing the NewsSeecr location source. */
    private static String mashapeTestKey;
    /** Path to the training data. */
    private static String trainDataSet;
    /** Path to the validation data. */
    private static String validationDataSet;

    @BeforeClass
    public static void readConfiguration() throws ConfigurationException {
        try {
            PropertiesConfiguration config = new PropertiesConfiguration(
                    ResourceHelper.getResourceFile("/palladian-test.properties"));
            trainDataSet = config.getString("dataset.tudloc2013.train");
            validationDataSet = config.getString("dataset.tudloc2013.test");
            assumeDirectory(trainDataSet, validationDataSet);
            mashapeTestKey = config.getString("api.newsseecr.mashapeKey");
            assumeTrue("api.newsseecr.mashapeKey configuration is missing", StringUtils.isNotBlank(mashapeTestKey));
        } catch (FileNotFoundException e) {
            fail("palladian-test.properties not found; test is skipped!");
        }
    }

    /**
     * Make sure, all given paths are pointing to directories.
     * 
     * @param paths The paths to check.
     */
    private static void assumeDirectory(String... paths) {
        for (String path : paths) {
            assumeTrue(path + " not present", new File(path).isDirectory());
        }
    }

    @Test
//    @Ignore
    public void testPalladianLocationExtractor_Heuristic() {
        LocationSource locationSource = new NewsSeecrLocationSource(mashapeTestKey);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, disambiguation);
//        LocationExtractor extractor = new NewsSeecrLocationExtractor(mashapeTestKey);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationDataSet), true);
        System.out.println(result);
    }
    
    @Test
    @Ignore
    public void testPalladianLocationExtractor_FeatureBased() throws IOException {
        LocationSource locationSource = new NewsSeecrLocationSource(mashapeTestKey);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource);
        learner.learn(new File(trainDataSet));
    }
    
}
