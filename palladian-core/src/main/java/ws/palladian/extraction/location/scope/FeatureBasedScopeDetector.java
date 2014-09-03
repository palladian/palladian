package ws.palladian.extraction.location.scope;

import static ws.palladian.extraction.location.LocationExtractorUtils.ANNOTATION_LOCATION_FUNCTION;
import static ws.palladian.extraction.location.LocationFilters.ancestorOf;
import static ws.palladian.extraction.location.LocationFilters.coordinate;
import static ws.palladian.extraction.location.LocationFilters.descendantOf;
import static ws.palladian.helper.collection.CollectionHelper.coalesce;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.dt.QuickDtClassifier;
import ws.palladian.classification.dt.QuickDtLearner;
import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.classification.featureselection.BackwardFeatureElimination;
import ws.palladian.classification.featureselection.BackwardFeatureElimination.FMeasureScorer;
import ws.palladian.classification.featureselection.FeatureRanking;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.LocationSet;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.ClassifiableLocation;
import ws.palladian.extraction.location.disambiguation.ConfigurableFeatureExtractor;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguation;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.evaluation.TudLoc2013DatasetIterable;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.Stats;
import ws.palladian.persistence.DatabaseManagerFactory;

/**
 * <p>
 * A {@link RankingScopeDetector} which uses various features to train a model, which is then used for predicting the
 * scope. The features are mainly influenced from the rule-based {@link RankingScopeDetector}s (see implementations).
 * </p>
 * 
 * @author pk
 */
public final class FeatureBasedScopeDetector extends AbstractRankingScopeDetector {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureBasedScopeDetector.class);

    /** Threshold between candidate and actual scope, under which a training instance is considered positive. */
    private static final int POSITIVE_DISTANCE_THRESHOLD = 50;

    private static final String NAME = "FeatureBased";

    private final QuickDtModel scopeModel;

    private final QuickDtClassifier classifier = new QuickDtClassifier();

    public FeatureBasedScopeDetector(LocationExtractor extractor, QuickDtModel scopeModel) {
        super(extractor);
        Validate.notNull(scopeModel, "scopeModel must not be null");
        this.scopeModel = scopeModel;
    }

    @Override
    public Location getScope(Collection<LocationAnnotation> annotations) {
        Validate.notNull(annotations, "annotations must not be null");
        if (annotations.isEmpty()) {
            return null;
        }

        Set<ClassifiableLocation> classifiableLocations = extractFeatures(annotations);
        double maximumScore = Double.MIN_VALUE;
        Location selectedLocation = null;

        for (ClassifiableLocation location : classifiableLocations) {
            CategoryEntries classificationResult = classifier.classify(location.getFeatureVector(), scopeModel);
            double score = classificationResult.getProbability("true");
            LOGGER.trace("{} : {}", location.getLocation().getPrimaryName(), score);
            if (selectedLocation == null || score > maximumScore) {
                maximumScore = score;
                selectedLocation = location.getLocation();
            }
        }

        return selectedLocation;
    }

    private static Set<ClassifiableLocation> extractFeatures(Collection<LocationAnnotation> annotations) {

        List<Location> locationList = CollectionHelper.convertList(annotations, ANNOTATION_LOCATION_FUNCTION);
        LocationSet allStats = new LocationSet(locationList);
        LocationSet coordinateStats = allStats.where(coordinate());
        if (coordinateStats.isEmpty()) {
            return Collections.emptySet();
        }

        GeoCoordinate midpoint = coordinateStats.midpoint();
        GeoCoordinate centerpoint = coordinateStats.center();
        int maxOffset = 1;
        for (LocationAnnotation annotation : annotations) {
            maxOffset = Math.max(maxOffset, annotation.getStartPosition());
        }
        int numLocations = annotations.size();

        Set<ClassifiableLocation> instances = CollectionHelper.newHashSet();
        for (Location location : allStats) {
            GeoCoordinate coordinate = CollectionHelper.coalesce(location.getCoordinate(), GeoCoordinate.NULL);
            double maxDisambiguationTrust = 0;
            int firstPosition = Integer.MAX_VALUE;
            int lastPosition = Integer.MIN_VALUE;
            for (LocationAnnotation annotation : annotations) {
                if (annotation.getLocation().equals(location)) {
                    maxDisambiguationTrust = Math.max(maxDisambiguationTrust, annotation.getTrust());
                    firstPosition = Math.min(firstPosition, annotation.getStartPosition());
                    lastPosition = Math.max(lastPosition, annotation.getStartPosition());
                }
            }
            Stats distances = coordinateStats.distanceStats(location);

            InstanceBuilder builder = new InstanceBuilder();
            builder.set("midpointDistance", midpoint.distance(coordinate));
            builder.set("centerpointDistance", centerpoint.distance(coordinate));
            builder.set("occurrenceFrequency", (double)Collections.frequency(locationList, location) / numLocations);
            builder.set("descendantPercentage", (double)allStats.where(descendantOf(location)).size() / numLocations);
            builder.set("ancestorPercentage", (double)allStats.where(ancestorOf(location)).size() / numLocations);
            builder.set("hierarchyDepth", location.getAncestorIds().size());
            builder.set("population", CollectionHelper.coalesce(location.getPopulation(), 0l));
            builder.set("locationType", location.getType().toString());
            builder.set("disambiguationTrust", maxDisambiguationTrust);
            builder.set("offsetFirst", (double)firstPosition / maxOffset);
            builder.set("offsetLast", (double)lastPosition / maxOffset);
            builder.set("offsetSpread", (double)(lastPosition - firstPosition) / maxOffset);
            builder.set("minDistanceToOthers", Double.isNaN(distances.getMin()) ? 0 : distances.getMin());
            builder.set("maxDistanceToOthers", Double.isNaN(distances.getMax()) ? 0 : distances.getMax());
            builder.set("meanDistanceToOthers", Double.isNaN(distances.getMean()) ? 0 : distances.getMean());
            builder.set("medianDistanceToOthers", Double.isNaN(distances.getMedian()) ? 0 : distances.getMedian());

            instances.add(new ClassifiableLocation(location, builder.create()));
        }
        return instances;
    }

    @Override
    public String toString() {
        return NAME + ":" + classifier.getClass().getSimpleName();
    }

    /**
     * <p>
     * Train a new model for location scope detection. The dataset is represented by the {@link Iterator}.
     * </p>
     * 
     * @param documentIterator The iterator representing the dataset, not <code>null</code>.
     * @param extractor The {@link LocationExtractor}, not <code>null</code>.
     * @return The trained model.
     */
    public static QuickDtModel train(Iterable<LocationDocument> documentIterator, LocationExtractor extractor) {
        Validate.notNull(documentIterator, "documentIterator must not be null");
        Validate.notNull(extractor, "extractor must not be null");
        Collection<Instance> instances = createInstances(documentIterator, extractor);
        StopWatch stopWatch = new StopWatch();
        QuickDtModel scopeModel = QuickDtLearner.randomForest(100).train(instances);
        LOGGER.info("Trained model in {}", stopWatch.getElapsedTimeString());
        return scopeModel;
    }

    public static Collection<Instance> createInstances(Iterable<LocationDocument> documents, LocationExtractor extractor) {
        Validate.notNull(documents, "documents must not be null");
        Validate.notNull(extractor, "extractor must not be null");

        Collection<Instance> instances = CollectionHelper.newHashSet();
        for (LocationDocument trainDocument : documents) {
            List<LocationAnnotation> annotations = extractor.getAnnotations(trainDocument.getText());
            Location mainLocation = trainDocument.getMainLocation();
            if (annotations.isEmpty() || mainLocation == null || mainLocation.getCoordinate() == null) {
                continue;
            }

            Set<ClassifiableLocation> classifiableLocations = extractFeatures(annotations);

            // 1) determine closest location to actual scope
            ClassifiableLocation positiveCandidate = null;
            double minDistance = Double.MAX_VALUE;
            for (ClassifiableLocation classifiableLocation : classifiableLocations) {
                GeoCoordinate coordinate = coalesce(classifiableLocation.getLocation().getCoordinate(),
                        GeoCoordinate.NULL);
                double currentDistance = mainLocation.getCoordinate().distance(coordinate);
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    positiveCandidate = classifiableLocation;
                }
            }

            // 2) check, if we are in range
            if (minDistance > POSITIVE_DISTANCE_THRESHOLD) {
                positiveCandidate = null;
                LOGGER.warn("Could not determine positive candidate, distance to closest is {}", minDistance);
                // XXX maybe it would make more sense, to drop all training examples in this case?
            } else {
                LOGGER.trace("Distance between actual and training candidate is {}", minDistance);
            }

            // 3) create positive and negative training examples
            for (ClassifiableLocation location : classifiableLocations) {
                boolean positive = location == positiveCandidate;
                instances.add(new InstanceBuilder().add(location.getFeatureVector()).create(positive));
            }
        }
        return instances;
    }

    /**
     * <p>
     * Run a backward feature elimination.
     * </p>
     * 
     * @param trainingCsv The CSV file with the training data, not <code>null</code>.
     * @param validationCsv The CSV file with the validation data, not <code>null</code>.
     * @param learner The learner, not <code>null</code>.
     * @param predictor The predictor, not <code>null</code>.
     */
    public static <M extends Model> void runFeatureElimination(File trainingCsv, File validationCsv,
            Learner<M> learner, Classifier<M> predictor) {
        List<Instance> trainSet = new CsvDatasetReader(trainingCsv).readAll();
        List<Instance> validationSet = new CsvDatasetReader(validationCsv).readAll();

        FMeasureScorer scorer = new FMeasureScorer("true");
        BackwardFeatureElimination<M> featureElimination = new BackwardFeatureElimination<M>(learner, predictor, scorer);
        FeatureRanking featureRanking = featureElimination.rankFeatures(trainSet, validationSet, NoProgress.INSTANCE);
        CollectionHelper.print(featureRanking.getAll());
    }

    public static void main(String[] args) throws IOException {
        QuickDtModel model = FileHelper
                .deserialize("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/Models/location_disambiguation_all_train_1377442726898.model");
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        // note that we are using a zero confidence threshold here; experiments showed, that it's better to go for high
        // recall here, and let the classifier scope detection's classifier decide about each candidate (this is at
        // least the case in the Wikipedia dataset; on the TUD-Loc-2013, it actually harms performance, but we have much
        // less data here for making a definite statement).
        FeatureBasedDisambiguation disambiguation = new FeatureBasedDisambiguation(model, 0,
                new ConfigurableFeatureExtractor());
        LocationExtractor extractor = new PalladianLocationExtractor(database, disambiguation);

        // Wikipedia scope dataset //////////////////////////////////////////////////////////////////////////////
        // File trainingDirectory = new File("/Users/pk/Desktop/WikipediaScopeDataset-2014/split-1");
        // File trainingCsv = new File("/Users/pk/Desktop/scopeFeaturesWikipediaTraining.csv");
        // File validationDirectory = new File("/Users/pk/Desktop/WikipediaScopeDataset-2014/split-2");
        // File validationCsv = new File("/Users/pk/Desktop/scopeFeaturesWikipediaValidation.csv");
        // Iterable<LocationDocument> trainingSet = new WikipediaLocationScopeIterator(trainingDirectory);
        // Iterable<LocationDocument> validationSet = new WikipediaLocationScopeIterator(validationDirectory);

        // TUD-Loc-2013 dataset //////////////////////////////////////////////////////////////////////////////
        File trainingDirectory = new File("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/1-training");
        // File trainingCsv = new File("/Users/pk/Desktop/scopeFeaturesTudLocTraining.csv");
        // File validationDirectory = new File("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/2-validation");
        // File validationCsv = new File("/Users/pk/Desktop/scopeFeaturesTudLocValidation.csv");
        Iterable<LocationDocument> trainingSet = new TudLoc2013DatasetIterable(trainingDirectory);
        // Iterable<LocationDocument> validationSet = new TudLoc2013DatasetIterable(validationDirectory);

        // train(trainingSet, extractor, null, trainingCsv, null, null);
        // train(validationSet, extractor, null, validationCsv, null, null);
        // System.exit(0);

        // runFeatureElimination(trainingCsv, validationCsv, QuickDtLearner.randomForest(), new QuickDtClassifier());

        // feature set was determined using backward feature elimination, using train + validation set on Wikipedia set

        // QuickDt model
        File modelOutput = new File("scopeDetection_tud-loc_quickDt.model");
        QuickDtModel scopeModel = train(trainingSet, extractor);
        FileHelper.serialize(scopeModel, modelOutput.getPath());
    }

}
