package ws.palladian.extraction.location.scope.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.GeoUtils;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguation;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.evaluation.TudLoc2013DatasetIterable;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.extraction.location.scope.FeatureBasedScopeDetector;
import ws.palladian.extraction.location.scope.FirstScopeDetector;
import ws.palladian.extraction.location.scope.FrequencyScopeDetector;
import ws.palladian.extraction.location.scope.HighestPopulationScopeDetector;
import ws.palladian.extraction.location.scope.HighestTrustScopeDetector;
import ws.palladian.extraction.location.scope.LeastDistanceScopeDetector;
import ws.palladian.extraction.location.scope.MidpointScopeDetector;
import ws.palladian.extraction.location.scope.RankingScopeDetector;
import ws.palladian.extraction.location.scope.ScopeDetector;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.FatStats;
import ws.palladian.helper.math.Stats;
import ws.palladian.persistence.DatabaseManagerFactory;

public class ScopeDetectorEvaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ScopeDetectorEvaluator.class);

    /** The failure distance which we assume, in case a {@link RankingScopeDetector} detects no location. */
    private static final double MISS_DISTANCE = 0.5 * GeoUtils.EARTH_CIRCUMFERENCE_KM;

    public static final File RESULT_CSV_FILE = new File("data/_scopeDetectionResults.csv");

    private static final String RESULT_DETAILS_FILE = "data/scopeDetectionDetailedResults_%s.csv";

    private final List<Iterable<LocationDocument>> datasets = CollectionHelper.newArrayList();

    private final List<ScopeDetector> detectors = CollectionHelper.newArrayList();

    public void addDataset(Iterable<LocationDocument> dataset) {
        datasets.add(dataset);
    }

    public void addDetector(ScopeDetector detector) {
        detectors.add(detector);
    }

    public void runAll(boolean detailedResults) {
        writeHeader();

        for (Iterable<LocationDocument> dataset : datasets) {
            FileHelper.appendFile(RESULT_CSV_FILE.getPath(), "##### " + dataset.toString() + "\n");
            for (ScopeDetector detector : detectors) {
                evaluateScopeDetection(detector, dataset, detailedResults);
            }
            FileHelper.appendFile(RESULT_CSV_FILE.getPath(), "\n\n");
            FileHelper.appendFile(RESULT_CSV_FILE.getPath(), "\n\n");
        }
    }

    /**
     * Write CSV header if necessary.
     */
    private static void writeHeader() {
        if (!RESULT_CSV_FILE.isFile()) {
            String header = "detector;below1km;below10km;below100km;below1000km;meanError;medianError;minError;maxError;mse;rmse;misses\n";
            FileHelper.writeToFile(RESULT_CSV_FILE.getPath(), header);
        }
    }

    /**
     * @param scopeDetector The {@link RankingScopeDetector} to evaluate, not <code>null</code>.
     * @param locationExtractor The {@link LocationExtractor} to use for extracting the locations, not <code>null</code>
     *            .
     * @param documentIterator The {@link Iterator} over the dataset, not <code>null</code>.
     * @param detailedResults <code>true</code> to write an additional CSV file with detailed results information.
     */
    public static void evaluateScopeDetection(ScopeDetector scopeDetector,
            Iterable<LocationDocument> documentIterator, boolean detailedResults) {
        Validate.notNull(scopeDetector, "scopeDetector must not be null");
        Validate.notNull(documentIterator, "documentIterator must not be null");

        Stats distanceStats = new FatStats();
        int misses = 0;
        StringBuilder detailedResultsBuilder = new StringBuilder();
        detailedResultsBuilder.append(documentIterator).append('\n');
        detailedResultsBuilder.append(scopeDetector.getClass().getSimpleName()).append("\n\n\n");
        detailedResultsBuilder.append("document;expected;actual;error\n");

        for (LocationDocument document : documentIterator) {
            Location mainLocation = document.getMainLocation();
            if (mainLocation == null) {
                // these will simply be ignored
                LOGGER.debug("*** no reference scope provided in {}", document.getFileName());
                continue;
            }
            GeoCoordinate reference = mainLocation.getCoordinate();
            GeoCoordinate scopeCoordinate = scopeDetector.getScope(document.getText());
            double distance;
            if (scopeCoordinate!= null) {
                distance = reference.distance(scopeCoordinate);
            } else {
                LOGGER.debug("*** no scope detected for {}", document.getFileName());
                misses++;
                distance = MISS_DISTANCE;
            }
            LOGGER.trace("Actual: {}, extracted: {}, distance: {}", mainLocation.getCoordinate(), scopeCoordinate, distance);
            distanceStats.add(distance);

            // detailed results
            if (detailedResults) {
                detailedResultsBuilder.append(document.getFileName()).append(';');
                detailedResultsBuilder.append(mainLocation).append(';');
                detailedResultsBuilder.append(scopeCoordinate).append(';');
                detailedResultsBuilder.append(distance).append('\n');
            }
        }

        String line = String.format("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s\n", //
                scopeDetector.toString(), //
                distanceStats.getCumulativeProbability(1), //
                distanceStats.getCumulativeProbability(10), //
                distanceStats.getCumulativeProbability(100), //
                distanceStats.getCumulativeProbability(1000), //
                distanceStats.getMean(), //
                distanceStats.getMedian(), //
                distanceStats.getMin(), //
                distanceStats.getMax(), //
                distanceStats.getMse(), //
                distanceStats.getRmse(), //
                misses); //
        writeHeader();
        FileHelper.appendFile(RESULT_CSV_FILE.getPath(), line);

        // write detailed results
        if (detailedResults) {
            FileHelper.writeToFile(String.format(RESULT_DETAILS_FILE, System.currentTimeMillis()),
                    detailedResultsBuilder);
        }
    }

    public static void main(String[] args) throws IOException {
        ScopeDetectorEvaluator eval = new ScopeDetectorEvaluator();

        // general necessary prerequisites
        LocationDatabase source = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        QuickDtModel disambiguationModel = FileHelper
                .deserialize("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/Models/location_disambiguation_all_train_1377442726898.model");

        // the extractors
        FeatureBasedDisambiguation disambiguation = new FeatureBasedDisambiguation(disambiguationModel, 0, 1000);
        LocationExtractor extractor = new PalladianLocationExtractor(source, disambiguation);

        // the test datasets
        eval.addDataset(new TudLoc2013DatasetIterable(new File("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/3-test")));
        eval.addDataset(new WikipediaLocationScopeIterator(new File(
                "/Users/pk/Desktop/WikipediaScopeDataset-2014/split-3")));

        // the scope detectors under test
        eval.addDetector(new FirstScopeDetector(extractor));
        eval.addDetector(new HighestPopulationScopeDetector(extractor));
        eval.addDetector(new FrequencyScopeDetector(extractor));
        eval.addDetector(new MidpointScopeDetector(extractor));
        eval.addDetector(new LeastDistanceScopeDetector(extractor));
        eval.addDetector(new HighestTrustScopeDetector(extractor));

        QuickDtModel quickDtModel = FileHelper.deserialize("scopeDetection_wikipedia_quickDt.model");
        eval.addDetector(new FeatureBasedScopeDetector(extractor, quickDtModel));

        quickDtModel = FileHelper.deserialize("scopeDetection_tud-loc_quickDt.model");
        eval.addDetector(new FeatureBasedScopeDetector(extractor, quickDtModel));

        eval.runAll(true);

    }

}
