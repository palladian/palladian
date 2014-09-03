package ws.palladian.extraction.location.scope;

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
import ws.palladian.extraction.location.DefaultLocationTagger;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.PalladianLocationExtractorIT;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianLocationExtractorIT.class);

    /**
     * The expected amount of location entries in the database. Checked prior testing, to guarantee comparable testing
     * results.
     */
    private static final int EXPECTED_DB_LOCATION_COUNT = 9605378;

    /** Path to the feature based disambiguation model. */
    private static final String DISAMBIGUATION_PATH = "/model/locationDisambiguationModel_tud_1409729069110.ser.gz";

    /** The gazetteer. */
    private static LocationSource locationSource;

    private static Configuration config;

    private static QuickDtModel disambiguationModel;

    @BeforeClass
    public static void readConfiguration() throws ConfigurationException {
        try {
            config = new PropertiesConfiguration(ResourceHelper.getResourceFile("/palladian-test.properties"));
            String dbUrl = config.getString("db.jdbcUrl");
            String dbUsername = config.getString("db.username");
            String dbPassword = config.getString("db.password");
            if (StringUtils.isNotBlank(dbUrl) && StringUtils.isNotBlank(dbUsername)) {
                locationSource = DatabaseManagerFactory.create(LocationDatabase.class, dbUrl, dbUsername, dbPassword);
                LOGGER.info("Using local DB ({}) for testing", dbUrl);
                if (locationSource.size() != EXPECTED_DB_LOCATION_COUNT) {
                    LOGGER.warn(
                            "LocationSource does not contain the expected amount of locations; make sure to use the correct database ({} instead of {}).",
                            locationSource.size(), EXPECTED_DB_LOCATION_COUNT);
                }
            } else {
                assumeTrue("palladian-test.properties must provide a database configuration", false);
            }
        } catch (FileNotFoundException e) {
            assumeTrue("palladian-test.properties not found; test is skipped!", false);
        }
        try {
            disambiguationModel = FileHelper.deserialize(ResourceHelper.getResourcePath(DISAMBIGUATION_PATH));
        } catch (IOException e) {
            throw new IllegalStateException("Could not load disambiguation model from resource path '"
                    + DISAMBIGUATION_PATH + ".", e);
        }
    }

    @Test
    public void testFirstScopeDetector() throws IOException {
        String validationPath = config.getString("dataset.tudloc2013.validation");
        ITHelper.assumeDirectory(validationPath);
        LocationDisambiguation disambiguation = new FeatureBasedDisambiguation(disambiguationModel, 0);
        LocationExtractor extractor = new PalladianLocationExtractor(locationSource, DefaultLocationTagger.INSTANCE,
                disambiguation);
        FirstScopeDetector detector = new FirstScopeDetector(extractor);
        Iterable<LocationDocument> documentIterator = new TudLoc2013DatasetIterable(new File(validationPath));
        Stats evaluationResult = ScopeDetectorEvaluator.evaluateScopeDetection(detector, documentIterator, false);
        ITHelper.assertGreater("meanErrorDistance", 1631.17, evaluationResult.getMean());
        ITHelper.assertGreater("medianErrorDistance", 2.83, evaluationResult.getMedian());
    }

}
