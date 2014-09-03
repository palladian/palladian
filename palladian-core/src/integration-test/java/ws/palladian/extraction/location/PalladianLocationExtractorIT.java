package ws.palladian.extraction.location;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.extraction.location.disambiguation.ConfigurableFeatureExtractor;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguation;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguationLearner;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.disambiguation.LocationDisambiguation;
import ws.palladian.extraction.location.evaluation.LocationExtractionEvaluator;
import ws.palladian.extraction.location.evaluation.LocationExtractionEvaluator.LocationEvaluationResult;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

/**
 * <p>
 * Integration test for the {@link PalladianLocationExtractor} and its {@link LocationDisambiguation} strategies. The
 * test must be run with a local database and the following database dump, in order to produce meaningful results:
 * <code>locations_2013-08-05.sql.gz</code>.
 * 
 * @author pk
 */
public class PalladianLocationExtractorIT {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianLocationExtractorIT.class);

    /**
     * The expected amount of location entries in the database. Checked prior testing, to guarantee comparable testing
     * results.
     */
    private static final int EXPECTED_DB_LOCATION_COUNT = 9605378;

    /** The gazetteer. */
    private static LocationSource locationSource;

    private static Configuration config;

    @BeforeClass
    public static void readConfiguration() throws ConfigurationException {
        if (ProcessHelper.getFreeMemory() < SizeUnit.MEGABYTES.toBytes(750)) {
            fail("Not enough memory. This test requires at least 1 GB heap memory.");
        }
        try {
            config = new PropertiesConfiguration(ResourceHelper.getResourceFile("/palladian-test.properties"));
            String dbUrl = config.getString("db.jdbcUrl");
            String dbUsername = config.getString("db.username");
            String dbPassword = config.getString("db.password");
            // String mashapeTestKey = config.getString("api.newsseecr.mashapeKey");
            if (StringUtils.isNotBlank(dbUrl) && StringUtils.isNotBlank(dbUsername)) {
                locationSource = DatabaseManagerFactory.create(LocationDatabase.class, dbUrl, dbUsername, dbPassword);
                LOGGER.info("Using local DB ({}) for testing", dbUrl);
                // } else if (StringUtils.isNotBlank(mashapeTestKey)) {
                // locationSource = new NewsSeecrLocationSource(mashapeTestKey);
                // LOGGER.info("Using NewsSeecr DB for testing");
                if (locationSource.size() != EXPECTED_DB_LOCATION_COUNT) {
                    LOGGER.warn(
                            "LocationSource does not contain the expected amount of locations; make sure to use the correct database ({} instead of {}).",
                            locationSource.size(), EXPECTED_DB_LOCATION_COUNT);
                }
            } else {
                assumeTrue(
                // "palladian-test.properties must either provide a database configuration, or a Mashape API key",
                        "palladian-test.properties must provide a database configuration", false);
            }
        } catch (FileNotFoundException e) {
            assumeTrue("palladian-test.properties not found; test is skipped!", false);
        }
    }

    @Test
    public void test_Heuristic_TUD() {
        String validationPath = config.getString("dataset.tudloc2013.validation");
        LocationTestHelper.assumeDirectory(validationPath);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        LocationTestHelper.assertGreater("MUC-Pr", result.mucPr, 0.8247);
        LocationTestHelper.assertGreater("MUC-Rc", result.mucRc, 0.7509);
        LocationTestHelper.assertGreater("MUC-F1", result.mucF1, 0.7861);
        LocationTestHelper.assertGreater("Geo-Pr", result.geoPr, 0.9369);
        LocationTestHelper.assertGreater("Geo-Rc", result.geoRc, 0.7935);
        LocationTestHelper.assertGreater("Geo-F1", result.geoF1, 0.8593);
    }

    @Test
    public void test_Heuristic_LGL() {
        String validationPath = config.getString("dataset.lgl.validation");
        LocationTestHelper.assumeDirectory(validationPath);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        LocationTestHelper.assertGreater("MUC-Pr", result.mucPr, 0.7327);
        LocationTestHelper.assertGreater("MUC-Rc", result.mucRc, 0.6155);
        LocationTestHelper.assertGreater("MUC-F1", result.mucF1, 0.6690);
        LocationTestHelper.assertGreater("Geo-Pr", result.geoPr, 0.6866);
        LocationTestHelper.assertGreater("Geo-Rc", result.geoRc, 0.5697);
        LocationTestHelper.assertGreater("Geo-F1", result.geoF1, 0.6227);
    }

    @Test
    public void test_Heuristic_CLUST() {
        String validationPath = config.getString("dataset.clust.validation");
        LocationTestHelper.assumeDirectory(validationPath);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        LocationTestHelper.assertGreater("MUC-Pr", result.mucPr, 0.8044);
        LocationTestHelper.assertGreater("MUC-Rc", result.mucRc, 0.6878);
        LocationTestHelper.assertGreater("MUC-F1", result.mucF1, 0.7416);
        LocationTestHelper.assertGreater("Geo-Pr", result.geoPr, 0.9027);
        LocationTestHelper.assertGreater("Geo-Rc", result.geoRc, 0.7624);
        LocationTestHelper.assertGreater("Geo-F1", result.geoF1, 0.8267);
    }

    @Test
    public void test_MachineLearning_TUD() {
        String trainPath = config.getString("dataset.tudloc2013.train");
        String validationPath = config.getString("dataset.tudloc2013.validation");
        LocationTestHelper.assumeDirectory(trainPath, validationPath);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource,
                DefaultLocationTagger.INSTANCE, 100, new ConfigurableFeatureExtractor());
        QuickDtModel model = learner.learn(new File(trainPath));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        LocationTestHelper.assertGreater("MUC-Pr", result.mucPr, 0.83);
        LocationTestHelper.assertGreater("MUC-Rc", result.mucRc, 0.73);
        LocationTestHelper.assertGreater("MUC-F1", result.mucF1, 0.78);
        LocationTestHelper.assertGreater("Geo-Pr", result.geoPr, 0.96);
        LocationTestHelper.assertGreater("Geo-Rc", result.geoRc, 0.82);
        LocationTestHelper.assertGreater("Geo-F1", result.geoF1, 0.89);
    }

    @Test
    public void test_MachineLearning_LGL() {
        String trainPath = config.getString("dataset.lgl.train");
        String validationPath = config.getString("dataset.lgl.validation");
        LocationTestHelper.assumeDirectory(trainPath, validationPath);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource,
                DefaultLocationTagger.INSTANCE, 100, new ConfigurableFeatureExtractor());
        QuickDtModel model = learner.learn(new File(trainPath));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        LocationTestHelper.assertGreater("MUC-Pr", result.mucPr, 0.77);
        LocationTestHelper.assertGreater("MUC-Rc", result.mucRc, 0.60);
        LocationTestHelper.assertGreater("MUC-F1", result.mucF1, 0.67);
        LocationTestHelper.assertGreater("Geo-Pr", result.geoPr, 0.78);
        LocationTestHelper.assertGreater("Geo-Rc", result.geoRc, 0.62);
        LocationTestHelper.assertGreater("Geo-F1", result.geoF1, 0.69);
    }

    @Test
    public void test_MachineLearning_CLUST() {
        String trainPath = config.getString("dataset.clust.train");
        String validationPath = config.getString("dataset.clust.validation");
        LocationTestHelper.assumeDirectory(trainPath, validationPath);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource,
                DefaultLocationTagger.INSTANCE, 100, new ConfigurableFeatureExtractor());
        QuickDtModel model = learner.learn(new File(trainPath));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        LocationTestHelper.assertGreater("MUC-Pr", result.mucPr, 0.80);
        LocationTestHelper.assertGreater("MUC-Rc", result.mucRc, 0.69);
        LocationTestHelper.assertGreater("MUC-F1", result.mucF1, 0.74);
        LocationTestHelper.assertGreater("Geo-Pr", result.geoPr, 0.93);
        LocationTestHelper.assertGreater("Geo-Rc", result.geoRc, 0.82);
        LocationTestHelper.assertGreater("Geo-F1", result.geoF1, 0.87);
    }

    /**
     * Run the test of the {@link FeatureBasedDisambiguation} with a pre-learned model. In contrast to
     * {@link #test_MachineLearning_TUD()}, we can make exact assertions about results here.
     * 
     * @throws IOException In case the model cannot be loaded (should not happen).
     */
    @Test
    public void test_MachineLearning_TUD_existingModel() throws IOException {
        String validationPath = config.getString("dataset.tudloc2013.validation");
        LocationTestHelper.assumeDirectory(validationPath);
        QuickDtModel model = FileHelper.deserialize(ResourceHelper
                .getResourcePath("/model/locationDisambiguationModel_tud_1409729069110.ser.gz"));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        LocationTestHelper.assertGreater("MUC-Pr", result.mucPr, 0.8366);
        LocationTestHelper.assertGreater("MUC-Rc", result.mucRc, 0.7542);
        LocationTestHelper.assertGreater("MUC-F1", result.mucF1, 0.7933);
        LocationTestHelper.assertGreater("Geo-Pr", result.geoPr, 0.9710);
        LocationTestHelper.assertGreater("Geo-Rc", result.geoRc, 0.8362);
        LocationTestHelper.assertGreater("Geo-F1", result.geoF1, 0.8986);
    }

}
