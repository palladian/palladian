package ws.palladian.extraction.location.disambiguation;

import static ws.palladian.extraction.location.PalladianLocationExtractor.LONG_ANNOTATION_SPLIT;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.dt.QuickDtLearner;
import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.core.Annotation;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.location.AnnotationFilter;
import ws.palladian.extraction.location.ContextClassifier;
import ws.palladian.extraction.location.ContextClassifier.ClassificationMode;
import ws.palladian.extraction.location.ContextClassifier.ClassifiedAnnotation;
import ws.palladian.extraction.location.EntityPreprocessingTagger;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.evaluation.TudLoc2013DatasetIterable;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CompositeIterator;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

/**
 * <p>
 * This class is responsible for training models which can be used by the {@link FeatureBasedDisambiguation}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class FeatureBasedDisambiguationLearner {


    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureBasedDisambiguationLearner.class);

    /** Maximum distance between train and candidate location to be considered positive. */
    private static final int MAX_DISTANCE = 50;
    
    private final QuickDtLearner learner;

    private final LocationFeatureExtractor featureExtraction;

    private final EntityPreprocessingTagger tagger = new EntityPreprocessingTagger(LONG_ANNOTATION_SPLIT);

    private final AnnotationFilter filter = new AnnotationFilter();

    private final ContextClassifier contextClassifier = new ContextClassifier(ClassificationMode.PROPAGATION);

    private final LocationSource locationSource;

    public FeatureBasedDisambiguationLearner(LocationSource locationSource, int numTrees, LocationFeatureExtractor featureExtractor) {
        Validate.notNull(locationSource, "locationSource must not be null");
        this.learner = QuickDtLearner.randomForest(numTrees);
        this.locationSource = locationSource;
        this.featureExtraction = featureExtractor;
    }
    
    public FeatureBasedDisambiguationLearner(LocationSource locationSource) {
        this(locationSource, 10, new DefaultLocationFeatureExtractor(
                FeatureBasedDisambiguation.CONTEXT_SIZE));
    }

    public QuickDtModel learn(File datasetDirectory) throws IOException {
        return learn(new TudLoc2013DatasetIterable(datasetDirectory).iterator());
    }

    /**
     * <p>
     * Learn from multiple data sets.
     * </p>
     * 
     * @param datasetDirectories The directories to the training data sets, not <code>null</code>.
     * @return The model.
     * @throws IOException 
     */
    public QuickDtModel learn(File... datasetDirectories) throws IOException {
        Validate.notNull(datasetDirectories, "datasetDirectories must not be null");
        List<Iterator<LocationDocument>> datasetIterators = CollectionHelper.newArrayList();
        for (File datasetDirectory : datasetDirectories) {
            datasetIterators.add(new TudLoc2013DatasetIterable(datasetDirectory).iterator());
        }
        return learn(new CompositeIterator<LocationDocument>(datasetIterators));
    }

    public QuickDtModel learn(Iterator<LocationDocument> trainDocuments) throws IOException {
        Set<Instance> trainingData = createTrainingData(trainDocuments);
        String baseFileName = String.format("data/temp/location_disambiguation_%s", System.currentTimeMillis());
        ClassificationUtils.writeCsv(trainingData, new File(baseFileName + ".csv"));
        QuickDtModel model = learner.train(trainingData);
        String modelFileName = baseFileName + ".model";
        FileHelper.serialize(model, modelFileName);
        return model;
    }

    public Set<Instance> createTrainingData(Iterator<LocationDocument> trainDocuments) {
        Set<Instance> trainingData = CollectionHelper.newHashSet();
        while (trainDocuments.hasNext()) {
            LocationDocument trainDocument = trainDocuments.next();
            String text = trainDocument.getText();
            List<LocationAnnotation> trainAnnotations = trainDocument.getAnnotations();

            List<Annotation> taggedEntities = tagger.getAnnotations(text);
            taggedEntities = filter.filter(taggedEntities);
            List<ClassifiedAnnotation> classifiedEntities = contextClassifier.classify(taggedEntities, text);
            MultiMap<ClassifiedAnnotation, Location> locations = PalladianLocationExtractor.fetchLocations(
                    locationSource, classifiedEntities);

            Set<ClassifiableLocation> classifiableLocations = featureExtraction.extract(text, locations);
            Set<Instance> trainInstances = createTrainData(classifiableLocations, trainAnnotations);
            trainingData.addAll(trainInstances);
        }
        return trainingData;
    }

    private Set<Instance> createTrainData(Set<ClassifiableLocation> classifiableLocations,
            List<LocationAnnotation> positiveLocations) {
        Set<Instance> result = CollectionHelper.newHashSet();
        int numPositive = 0;
        for (ClassifiableLocation classifiableLocation : classifiableLocations) {
            boolean positiveClass = false;
            for (LocationAnnotation trainAnnotation : positiveLocations) {
                // we cannot determine the correct location, if the training data did not provide coordinates
                Location actualLocation = classifiableLocation.getLocation();
                if (actualLocation.getCoordinate() == null) {
                    continue;
                }
                Location trainLocation = trainAnnotation.getLocation();
                GeoCoordinate trainCoordinate = trainLocation.getCoordinate();
                // XXX offsets are not considered here; necessary?
                boolean samePlace = trainCoordinate != null
                        && actualLocation.getCoordinate().distance(trainCoordinate) < MAX_DISTANCE;
                boolean sameName = actualLocation.commonName(trainLocation);
                boolean sameType = actualLocation.getType().equals(trainLocation.getType());
                // consider locations as positive samples, if they have same name and have max. distance of 50 kms
                if (samePlace && sameName && sameType) {
                    numPositive++;
                    positiveClass = true;
                    break;
                }
            }
            result.add(new InstanceBuilder().add(classifiableLocation.getFeatureVector()).create(positiveClass));
        }
        double positivePercentage = MathHelper.round((float)numPositive / classifiableLocations.size() * 100, 2);
        LOGGER.info("{} positive instances in {} ({}%)", numPositive, classifiableLocations.size(), positivePercentage);
        return result;
    }

    public static void main(String[] args) throws IOException {
        LocationSource locationSource = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource);
        File datasetTud = new File("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2/1-training");
        File datasetLgl = new File("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/1-train");
        File datasetClust = new File("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/CLUST-converted/1-train");
        learner.learn(datasetTud);
        learner.learn(datasetLgl);
        learner.learn(datasetClust);
        learner.learn(datasetTud, datasetLgl, datasetClust);
        // dataset = new File("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2/2-validation");
        // learner.learn(dataset);
    }

}
