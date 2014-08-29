package ws.palladian.extraction.location;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;

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
import ws.palladian.extraction.location.sources.NewsSeecrLocationSource;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

public class PalladianLocationExtractorIT {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianLocationExtractorIT.class);

    /** The gazetteer. */
    private static LocationSource locationSource;

    private static Configuration config;

    @SuppressWarnings("deprecation")
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
            String mashapeTestKey = config.getString("api.newsseecr.mashapeKey");
            if (StringUtils.isNotBlank(dbUrl) && StringUtils.isNotBlank(dbUsername)) {
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
            assumeTrue("palladian-test.properties not found; test is skipped!", false);
        }
    }

    @Test
    public void test_Heuristic_TUD() {
        String validationPath = config.getString("dataset.tudloc2013.validation");
        assumeDirectory(validationPath);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        assertGreater("MUC-Pr", result.mucPr, 0.8163);
        assertGreater("MUC-Rc", result.mucRc, 0.7287);
        assertGreater("MUC-F1", result.mucF1, 0.7700);
        assertGreater("Geo-Pr", result.geoPr, 0.9214);
        assertGreater("Geo-Rc", result.geoRc, 0.7825);
        assertGreater("Geo-F1", result.geoF1, 0.8463);
    }

    @Test
    public void test_Heuristic_LGL() {
        String validationPath = config.getString("dataset.lgl.validation");
        assumeDirectory(validationPath);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        assertGreater("MUC-Pr", result.mucPr, 0.7339);
        assertGreater("MUC-Rc", result.mucRc, 0.5755);
        assertGreater("MUC-F1", result.mucF1, 0.6451);
        assertGreater("Geo-Pr", result.geoPr, 0.6746);
        assertGreater("Geo-Rc", result.geoRc, 0.5330);
        assertGreater("Geo-F1", result.geoF1, 0.5955);
    }

    @Test
    public void test_Heuristic_CLUST() {
        String validationPath = config.getString("dataset.clust.validation");
        assumeDirectory(validationPath);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        assertGreater("MUC-Pr", result.mucPr, 0.8068);
        assertGreater("MUC-Rc", result.mucRc, 0.6730);
        assertGreater("MUC-F1", result.mucF1, 0.7338);
        assertGreater("Geo-Pr", result.geoPr, 0.9018);
        assertGreater("Geo-Rc", result.geoRc, 0.7447);
        assertGreater("Geo-F1", result.geoF1, 0.8158);
    }

    @Test
    public void test_MachineLearning_TUD() {
        String trainPath = config.getString("dataset.tudloc2013.train");
        String validationPath = config.getString("dataset.tudloc2013.validation");
        assumeDirectory(trainPath, validationPath);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource,
                DefaultLocationTagger.INSTANCE, 100, new ConfigurableFeatureExtractor());
        QuickDtModel model = learner.learn(new File(trainPath));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        assertGreater("MUC-Pr", result.mucPr, 0.84);
        assertGreater("MUC-Rc", result.mucRc, 0.73);
        assertGreater("MUC-F1", result.mucF1, 0.78);
        assertGreater("Geo-Pr", result.geoPr, 0.96);
        assertGreater("Geo-Rc", result.geoRc, 0.82);
        assertGreater("Geo-F1", result.geoF1, 0.89);
    }

    @Test
    public void test_MachineLearning_LGL() {
        String trainPath = config.getString("dataset.lgl.train");
        String validationPath = config.getString("dataset.lgl.validation");
        assumeDirectory(trainPath, validationPath);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource,
                DefaultLocationTagger.INSTANCE, 100, new ConfigurableFeatureExtractor());
        QuickDtModel model = learner.learn(new File(trainPath));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        assertGreater("MUC-Pr", result.mucPr, 0.77);
        assertGreater("MUC-Rc", result.mucRc, 0.60);
        assertGreater("MUC-F1", result.mucF1, 0.68);
        assertGreater("Geo-Pr", result.geoPr, 0.80);
        assertGreater("Geo-Rc", result.geoRc, 0.64);
        assertGreater("Geo-F1", result.geoF1, 0.71);
    }

    @Test
    public void test_MachineLearning_CLUST() {
        String trainPath = config.getString("dataset.clust.train");
        String validationPath = config.getString("dataset.clust.validation");
        assumeDirectory(trainPath, validationPath);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource,
                DefaultLocationTagger.INSTANCE, 100, new ConfigurableFeatureExtractor());
        QuickDtModel model = learner.learn(new File(trainPath));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        assertGreater("MUC-Pr", result.mucPr, 0.80);
        assertGreater("MUC-Rc", result.mucRc, 0.69);
        assertGreater("MUC-F1", result.mucF1, 0.74);
        assertGreater("Geo-Pr", result.geoPr, 0.93);
        assertGreater("Geo-Rc", result.geoRc, 0.82);
        assertGreater("Geo-F1", result.geoF1, 0.87);
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

    private static void assertGreater(String valueName, double value, double minimumExpected) {
        String msg = valueName + " must be greater/equal " + minimumExpected + ", but was " + value;
        assertTrue(msg, value >= minimumExpected);
    }

}
