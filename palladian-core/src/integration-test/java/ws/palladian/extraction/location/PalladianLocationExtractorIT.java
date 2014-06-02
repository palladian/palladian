package ws.palladian.extraction.location;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguation;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguationLearner;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.disambiguation.LocationDisambiguation;
import ws.palladian.extraction.location.evaluation.LocationExtractionEvaluator;
import ws.palladian.extraction.location.evaluation.LocationExtractionEvaluator.LocationEvaluationResult;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.extraction.location.sources.NewsSeecrLocationSource;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

public class PalladianLocationExtractorIT {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianLocationExtractorIT.class);

    /** The gazetteer. */
    private static LocationSource locationSource;
    /** Path to the training data. */
    private static File trainDataSet;
    /** Path to the validation data. */
    private static File validationDataSet;

    @SuppressWarnings("deprecation")
    @BeforeClass
    public static void readConfiguration() throws ConfigurationException {
        try {
            PropertiesConfiguration config = new PropertiesConfiguration(
                    ResourceHelper.getResourceFile("/palladian-test.properties"));
            String trainDataPath = config.getString("dataset.tudloc2013.train");
            String validationDataPath = config.getString("dataset.tudloc2013.test");
            assumeDirectory(trainDataPath, validationDataPath);
            trainDataSet = new File(trainDataPath);
            validationDataSet = new File(validationDataPath);

            String dbUrl = config.getString("db.jdbcUrl");
            String dbUsername = config.getString("db.username");
            String dbPassword = config.getString("db.password");
            String mashapeTestKey = config.getString("api.newsseecr.mashapeKey");
            if (StringUtils.isNotBlank(dbUrl) && StringUtils.isNotBlank(dbPassword)) {
                locationSource = DatabaseManagerFactory.create(LocationDatabase.class, dbUrl, dbUsername, dbPassword);
                LOGGER.info("Using local DB ({}) for testing", dbUrl);
            } else if (StringUtils.isNotBlank(mashapeTestKey)) {
                locationSource = new NewsSeecrLocationSource(mashapeTestKey);
                LOGGER.info("Using NewsSeecr DB for testing");
            } else {
                assumeTrue(
                        "palladian-test.properties must either provide a database configuration, or a Mashape API key",
                        false);
            }
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
    public void testPalladianLocationExtractor_Heuristic() {
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, validationDataSet, true);
        // System.out.println(result);
        assertGreater("MUC-Pr", result.mucPr, 0.8163);
        assertGreater("MUC-Rc", result.mucRc, 0.7287);
        assertGreater("MUC-F1", result.mucF1, 0.7700);
        assertGreater("Geo-Pr", result.geoPr, 0.9214);
        assertGreater("Geo-Rc", result.geoRc, 0.7825);
        assertGreater("Geo-F1", result.geoF1, 0.8463);
    }

    @Test
    public void testPalladianLocationExtractor_MachineLearning() throws IOException {
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource, 100);
        QuickDtModel model = learner.learn(trainDataSet);
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, validationDataSet, true);
        System.out.println(result);
        // assertGreater("MUC-Pr", result.mucPr, 0.81);
        // assertGreater("MUC-Rc", result.mucRc, 0.75);
        assertGreater("MUC-F1", result.mucF1, 0.78);
        // assertGreater("Geo-Pr", result.geoPr, 0.92);
        // assertGreater("Geo-Rc", result.geoRc, 0.79);
        assertGreater("Geo-F1", result.geoF1, 0.84);
    }

    private static void assertGreater(String valueName, double value, double minimumExpected) {
        String msg = valueName + " must be greater/equal " + minimumExpected + ", but was " + value;
        assertTrue(msg, value >= minimumExpected);
    }

}
