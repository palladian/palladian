package ws.palladian.extraction.location;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.integrationtests.ITHelper;
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
        config = ITHelper.getTestConfig();
        ITHelper.assertMemory(750, SizeUnit.MEGABYTES);
        String dbUrl = config.getString("db.jdbcUrl");
        String dbUsername = config.getString("db.username");
        String dbPassword = config.getString("db.password");
        assertTrue("palladian-test.properties must provide a database URL", StringUtils.isNotBlank(dbUrl));
        assertTrue("palladian-test.properties must provide a database username", StringUtils.isNotBlank(dbUsername));
        locationSource = DatabaseManagerFactory.create(LocationDatabase.class, dbUrl, dbUsername, dbPassword);
        if (locationSource.size() != EXPECTED_DB_LOCATION_COUNT) {
            LOGGER.warn(
                    "LocationSource does not contain the expected amount of locations; make sure to use the correct database ({} instead of {}).",
                    locationSource.size(), EXPECTED_DB_LOCATION_COUNT);
        }
    }

    @Test
    public void test_Heuristic_TUD() {
        String validationPath = config.getString("dataset.tudloc2013.validation");
        ITHelper.assertDirectory(validationPath);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        ITHelper.assertMin("MUC-Pr", 0.8247, result.mucPr);
        ITHelper.assertMin("MUC-Rc", 0.7509, result.mucRc);
        ITHelper.assertMin("MUC-F1", 0.7861, result.mucF1);
        ITHelper.assertMin("Geo-Pr", 0.9369, result.geoPr);
        ITHelper.assertMin("Geo-Rc", 0.7935, result.geoRc);
        ITHelper.assertMin("Geo-F1", 0.8593, result.geoF1);
    }

    @Test
    @Ignore
    public void test_Heuristic_LGL() {
        String validationPath = config.getString("dataset.lgl.validation");
        ITHelper.assertDirectory(validationPath);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        ITHelper.assertMin("MUC-Pr", 0.7327, result.mucPr);
        ITHelper.assertMin("MUC-Rc", 0.6155, result.mucRc);
        ITHelper.assertMin("MUC-F1", 0.6690, result.mucF1);
        ITHelper.assertMin("Geo-Pr", 0.6866, result.geoPr);
        ITHelper.assertMin("Geo-Rc", 0.5697, result.geoRc);
        ITHelper.assertMin("Geo-F1", 0.6227, result.geoF1);
    }

    @Test
    @Ignore
    public void test_Heuristic_CLUST() {
        String validationPath = config.getString("dataset.clust.validation");
        ITHelper.assertDirectory(validationPath);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        ITHelper.assertMin("MUC-Pr", 0.8044, result.mucPr);
        ITHelper.assertMin("MUC-Rc", 0.6878, result.mucRc);
        ITHelper.assertMin("MUC-F1", 0.7416, result.mucF1);
        ITHelper.assertMin("Geo-Pr", 0.9027, result.geoPr);
        ITHelper.assertMin("Geo-Rc", 0.7624, result.geoRc);
        ITHelper.assertMin("Geo-F1", 0.8267, result.geoF1);
    }

    @Test
    public void test_MachineLearning_TUD() {
        String trainPath = config.getString("dataset.tudloc2013.train");
        String validationPath = config.getString("dataset.tudloc2013.validation");
        ITHelper.assertDirectory(trainPath, validationPath);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource,
                DefaultLocationTagger.INSTANCE, 100, new ConfigurableFeatureExtractor());
        QuickDtModel model = learner.learn(new File(trainPath));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        ITHelper.assertMin("MUC-Pr", 0.83, result.mucPr);
        ITHelper.assertMin("MUC-Rc", 0.73, result.mucRc);
        ITHelper.assertMin("MUC-F1", 0.78, result.mucF1);
        ITHelper.assertMin("Geo-Pr", 0.96, result.geoPr);
        ITHelper.assertMin("Geo-Rc", 0.82, result.geoRc);
        ITHelper.assertMin("Geo-F1", 0.89, result.geoF1);
    }

    @Test
    @Ignore
    public void test_MachineLearning_LGL() {
        String trainPath = config.getString("dataset.lgl.train");
        String validationPath = config.getString("dataset.lgl.validation");
        ITHelper.assertDirectory(trainPath, validationPath);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource,
                DefaultLocationTagger.INSTANCE, 100, new ConfigurableFeatureExtractor());
        QuickDtModel model = learner.learn(new File(trainPath));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        ITHelper.assertMin("MUC-Pr", 0.76, result.mucPr);
        ITHelper.assertMin("MUC-Rc", 0.60, result.mucRc);
        ITHelper.assertMin("MUC-F1", 0.67, result.mucF1);
        ITHelper.assertMin("Geo-Pr", 0.78, result.geoPr);
        ITHelper.assertMin("Geo-Rc", 0.62, result.geoRc);
        ITHelper.assertMin("Geo-F1", 0.69, result.geoF1);
    }

    @Test
    @Ignore
    public void test_MachineLearning_CLUST() {
        String trainPath = config.getString("dataset.clust.train");
        String validationPath = config.getString("dataset.clust.validation");
        ITHelper.assertDirectory(trainPath, validationPath);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource,
                DefaultLocationTagger.INSTANCE, 100, new ConfigurableFeatureExtractor());
        QuickDtModel model = learner.learn(new File(trainPath));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        ITHelper.assertMin("MUC-Pr", 0.80, result.mucPr);
        ITHelper.assertMin("MUC-Rc", 0.69, result.mucRc);
        ITHelper.assertMin("MUC-F1", 0.74, result.mucF1);
        ITHelper.assertMin("Geo-Pr", 0.93, result.geoPr);
        ITHelper.assertMin("Geo-Rc", 0.82, result.geoRc);
        ITHelper.assertMin("Geo-F1", 0.87, result.geoF1);
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
        ITHelper.assertDirectory(validationPath);
        QuickDtModel model = FileHelper.deserialize(ResourceHelper
                .getResourcePath("/model/locationDisambiguationModel_tud_1409729069110.ser.gz"));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        ITHelper.assertMin("MUC-Pr", 0.8366, result.mucPr);
        ITHelper.assertMin("MUC-Rc", 0.7542, result.mucRc);
        ITHelper.assertMin("MUC-F1", 0.7933, result.mucF1);
        ITHelper.assertMin("Geo-Pr", 0.9710, result.geoPr);
        ITHelper.assertMin("Geo-Rc", 0.8362, result.geoRc);
        ITHelper.assertMin("Geo-F1", 0.8986, result.geoF1);
    }

}
