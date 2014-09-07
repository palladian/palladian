package ws.palladian.extraction.location.scope;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.extraction.location.DefaultLocationTagger;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguation;
import ws.palladian.extraction.location.disambiguation.LocationDisambiguation;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.evaluation.TudLoc2013DatasetIterable;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.extraction.location.scope.evaluation.ScopeDetectorEvaluator;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.math.Stats;
import ws.palladian.integrationtests.ITHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

public class ScopeDetectorIT {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ScopeDetectorIT.class);

    /**
     * The expected amount of location entries in the database. Checked prior testing, to guarantee comparable testing
     * results.
     */
    private static final int EXPECTED_DB_LOCATION_COUNT = 9605378;

    /** Path to the feature based disambiguation model. */
    private static final String DISAMBIGUATION_PATH = "/model/locationDisambiguationModel_tud_1409729069110.ser.gz";

    /** The configuration. */
    private static Configuration config;

    /** The location extractor used for all tests. */
    private static LocationExtractor extractor;

    /** The validation data set. */
    private static Iterable<LocationDocument> documentIterator;

    @BeforeClass
    public static void readConfiguration() {
        config = ITHelper.getTestConfig();
        String dbUrl = config.getString("db.jdbcUrl");
        String dbUsername = config.getString("db.username");
        String dbPassword = config.getString("db.password");
        assertTrue("palladian-test.properties must provide a database URL", StringUtils.isNotBlank(dbUrl));
        assertTrue("palladian-test.properties must provide a database username", StringUtils.isNotBlank(dbUsername));

        String validationPath = config.getString("dataset.tudloc2013.validation");
        ITHelper.assertDirectory(validationPath);
        documentIterator = new TudLoc2013DatasetIterable(new File(validationPath));

        LocationSource locationSource = DatabaseManagerFactory.create(LocationDatabase.class, dbUrl, dbUsername,
                dbPassword);
        if (locationSource.size() != EXPECTED_DB_LOCATION_COUNT) {
            LOGGER.warn(
                    "LocationSource does not contain the expected amount of locations; make sure to use the correct database ({} instead of {}).",
                    locationSource.size(), EXPECTED_DB_LOCATION_COUNT);
        }
        QuickDtModel disambiguationModel;
        try {
            disambiguationModel = FileHelper.deserialize(ResourceHelper.getResourcePath(DISAMBIGUATION_PATH));
        } catch (IOException e) {
            throw new IllegalStateException("Could not deserialize disambiguation model from '" + DISAMBIGUATION_PATH
                    + "'.");
        }
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(disambiguationModel, 0);
        extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE, disambiguation);
    }
    
    @AfterClass
    public static void cleanUp() {
        config = null;
        extractor = null;
        documentIterator = null;
    }

    @Test
    public void testFirstScopeDetector() throws IOException {
        ScopeDetector detector = new FirstScopeDetector(extractor);
        Stats evaluationResult = ScopeDetectorEvaluator.evaluateScopeDetection(detector, documentIterator, false);
        // System.out.println(evaluationResult);
        ITHelper.assertMax("meanErrorDistance", 1632, evaluationResult.getMean());
        ITHelper.assertMax("medianErrorDistance", 3, evaluationResult.getMedian());
    }

    @Test
    public void testFrequencyScopeDetector() {
        ScopeDetector detector = new FrequencyScopeDetector(extractor);
        Stats evaluationResult = ScopeDetectorEvaluator.evaluateScopeDetection(detector, documentIterator, false);
        // System.out.println(evaluationResult);
        ITHelper.assertMax("meanErrorDistance", 566, evaluationResult.getMean());
        ITHelper.assertMax("medianErrorDistance", 0, evaluationResult.getMedian());
    }

    @Test
    public void testHighestPopulationScopeDetector() {
        ScopeDetector detector = new HighestPopulationScopeDetector(extractor);
        Stats evaluationResult = ScopeDetectorEvaluator.evaluateScopeDetection(detector, documentIterator, false);
        // System.out.println(evaluationResult);
        ITHelper.assertMax("meanErrorDistance", 3244, evaluationResult.getMean());
        ITHelper.assertMax("medianErrorDistance", 1284, evaluationResult.getMedian());
    }

    @Test
    public void testHighestTrustScopeDetector() {
        ScopeDetector detector = new HighestTrustScopeDetector(extractor);
        Stats evaluationResult = ScopeDetectorEvaluator.evaluateScopeDetection(detector, documentIterator, false);
        // System.out.println(evaluationResult);
        ITHelper.assertMax("meanErrorDistance", 1777, evaluationResult.getMean());
        ITHelper.assertMax("medianErrorDistance", 136, evaluationResult.getMedian());
    }

    @Test
    public void testLeastDistanceScopeDetector() {
        ScopeDetector detector = new LeastDistanceScopeDetector(extractor);
        Stats evaluationResult = ScopeDetectorEvaluator.evaluateScopeDetection(detector, documentIterator, false);
        // System.out.println(evaluationResult);
        ITHelper.assertMax("meanErrorDistance", 574, evaluationResult.getMean());
        ITHelper.assertMax("medianErrorDistance", 11, evaluationResult.getMedian());
    }

    @Test
    public void testMidpointScopeDetector() {
        ScopeDetector detector = new MidpointScopeDetector(extractor);
        Stats evaluationResult = ScopeDetectorEvaluator.evaluateScopeDetection(detector, documentIterator, false);
        // System.out.println(evaluationResult);
        ITHelper.assertMax("meanErrorDistance", 975, evaluationResult.getMean());
        ITHelper.assertMax("medianErrorDistance", 413, evaluationResult.getMedian());
    }

    @Test
    public void testFeatureBasedScopeDetector() {
        String trainPath = config.getString("dataset.tudloc2013.train");
        ITHelper.assertDirectory(trainPath);
        Iterable<LocationDocument> trainIterator = new TudLoc2013DatasetIterable(new File(trainPath));
        QuickDtModel model = FeatureBasedScopeDetector.train(trainIterator, extractor);
        FeatureBasedScopeDetector detector = new FeatureBasedScopeDetector(extractor, model);
        Stats evaluationResult = ScopeDetectorEvaluator.evaluateScopeDetection(detector, documentIterator, false);
        // System.out.println(evaluationResult);
        ITHelper.assertMax("meanErrorDistance", 405, evaluationResult.getMean());
        ITHelper.assertMax("medianErrorDistance", 0, evaluationResult.getMedian());
    }

    @Test
    public void testFeatureBasedScopeDetector_existingModel() throws IOException {
        QuickDtModel model = FileHelper.deserialize(ResourceHelper
                .getResourcePath("/model/locationScopeModel_tud_1409780094255.ser.gz"));
        FeatureBasedScopeDetector detector = new FeatureBasedScopeDetector(extractor, model);
        Stats evaluationResult = ScopeDetectorEvaluator.evaluateScopeDetection(detector, documentIterator, false);
        // System.out.println(evaluationResult);
        ITHelper.assertMax("meanErrorDistance", 401.81, evaluationResult.getMean());
        ITHelper.assertMax("medianErrorDistance", 0, evaluationResult.getMedian());
    }

}
