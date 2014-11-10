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
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultCandidateExtractor.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        ITHelper.assertMin("MUC-Pr", 0.8257, result.mucPr);
        ITHelper.assertMin("MUC-Rc", 0.7757, result.mucRc);
        ITHelper.assertMin("MUC-F1", 0.8, result.mucF1);
        ITHelper.assertMin("Geo-Pr", 0.9409, result.geoPr);
        ITHelper.assertMin("Geo-Rc", 0.8475, result.geoRc);
        ITHelper.assertMin("Geo-F1", 0.8917, result.geoF1);
    }

    @Test
    public void test_Heuristic_LGL() {
        String validationPath = config.getString("dataset.lgl.validation");
        ITHelper.assertDirectory(validationPath);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultCandidateExtractor.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        ITHelper.assertMin("MUC-Pr", 0.7376, result.mucPr);
        ITHelper.assertMin("MUC-Rc", 0.6204, result.mucRc);
        ITHelper.assertMin("MUC-F1", 0.6740, result.mucF1);
        ITHelper.assertMin("Geo-Pr", 0.7033, result.geoPr);
        ITHelper.assertMin("Geo-Rc", 0.5900, result.geoRc);
        ITHelper.assertMin("Geo-F1", 0.6417, result.geoF1);
    }

    @Test
    public void test_Heuristic_CLUST() {
        String validationPath = config.getString("dataset.clust.validation");
        ITHelper.assertDirectory(validationPath);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultCandidateExtractor.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        ITHelper.assertMin("MUC-Pr", 0.7886, result.mucPr);
        ITHelper.assertMin("MUC-Rc", 0.6901, result.mucRc);
        ITHelper.assertMin("MUC-F1", 0.7361, result.mucF1);
        ITHelper.assertMin("Geo-Pr", 0.9033, result.geoPr);
        ITHelper.assertMin("Geo-Rc", 0.7680, result.geoRc);
        ITHelper.assertMin("Geo-F1", 0.8302, result.geoF1);
    }

    @Test
    public void test_MachineLearning_TUD() {
        String trainPath = config.getString("dataset.tudloc2013.train");
        String validationPath = config.getString("dataset.tudloc2013.validation");
        ITHelper.assertDirectory(trainPath, validationPath);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource,
                DefaultCandidateExtractor.INSTANCE, 100, new ConfigurableFeatureExtractor());
        QuickDtModel model = learner.learn(new File(trainPath));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultCandidateExtractor.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        ITHelper.assertMin("MUC-Pr", 0.83, result.mucPr);
        ITHelper.assertMin("MUC-Rc", 0.73, result.mucRc);
        ITHelper.assertMin("MUC-F1", 0.78, result.mucF1);
        ITHelper.assertMin("Geo-Pr", 0.96, result.geoPr);
        ITHelper.assertMin("Geo-Rc", 0.82, result.geoRc);
        ITHelper.assertMin("Geo-F1", 0.88, result.geoF1);
    }

    @Test
    @Ignore
    public void test_MachineLearning_LGL() {
        String trainPath = config.getString("dataset.lgl.train");
        String validationPath = config.getString("dataset.lgl.validation");
        ITHelper.assertDirectory(trainPath, validationPath);
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource,
                DefaultCandidateExtractor.INSTANCE, 100, new ConfigurableFeatureExtractor());
        QuickDtModel model = learner.learn(new File(trainPath));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultCandidateExtractor.INSTANCE,
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
                DefaultCandidateExtractor.INSTANCE, 100, new ConfigurableFeatureExtractor());
        QuickDtModel model = learner.learn(new File(trainPath));
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultCandidateExtractor.INSTANCE,
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
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultCandidateExtractor.INSTANCE,
                disambiguation);
        LocationEvaluationResult result = LocationExtractionEvaluator.run(extractor, new File(validationPath), true);
        // System.out.println(result);
        ITHelper.assertMin("MUC-Pr", 0.8449, result.mucPr);
        ITHelper.assertMin("MUC-Rc", 0.7654, result.mucRc);
        ITHelper.assertMin("MUC-F1", 0.8032, result.mucF1);
        ITHelper.assertMin("Geo-Pr", 0.9716, result.geoPr);
        ITHelper.assertMin("Geo-Rc", 0.8510, result.geoRc);
        ITHelper.assertMin("Geo-F1", 0.9073, result.geoF1);
    }

}
