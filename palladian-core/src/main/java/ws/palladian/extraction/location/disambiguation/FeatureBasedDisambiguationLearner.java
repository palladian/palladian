package ws.palladian.extraction.location.disambiguation;

import org.apache.commons.lang3.Validate;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.classification.dt.QuickDtLearner;
import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.core.ClassifyingTagger;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.location.*;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.evaluation.TudLoc2013DatasetIterable;
import ws.palladian.extraction.location.persistence.lucene.LuceneLocationSource;
import ws.palladian.helper.collection.CompositeIterator;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

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

    private final int numTrees;

    private final LocationFeatureExtractor featureExtraction;

    private final LocationSource locationSource;

    private final ClassifyingTagger tagger;

    public FeatureBasedDisambiguationLearner(LocationSource locationSource, ClassifyingTagger tagger, int numTrees, LocationFeatureExtractor featureExtractor) {
        Validate.notNull(locationSource, "locationSource must not be null");
        this.locationSource = locationSource;
        this.tagger = tagger;
        this.numTrees = numTrees;
        this.featureExtraction = featureExtractor;
    }

    public QuickDtModel learn(File datasetDirectory) {
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
    public QuickDtModel learn(File... datasetDirectories) {
        Validate.notNull(datasetDirectories, "datasetDirectories must not be null");
        List<Iterator<LocationDocument>> datasetIterators = new ArrayList<>();
        for (File datasetDirectory : datasetDirectories) {
            datasetIterators.add(new TudLoc2013DatasetIterable(datasetDirectory).iterator());
        }
        return learn(new CompositeIterator<>(datasetIterators));
    }

    public QuickDtModel learn(Iterator<LocationDocument> trainDocuments) {
        Set<Instance> trainingData = createTrainingData(trainDocuments);
        QuickDtLearner learner = QuickDtLearner.randomForest(numTrees);
        return learner.train(trainingData);
    }

    public Set<Instance> createTrainingData(Iterator<LocationDocument> trainDocuments) {
        Set<Instance> trainingData = new HashSet<>();
        while (trainDocuments.hasNext()) {
            LocationDocument trainDocument = trainDocuments.next();
            String text = trainDocument.getText();
            List<LocationAnnotation> trainAnnotations = trainDocument.getAnnotations();
            List<ClassifiedAnnotation> classifiedEntities = tagger.getAnnotations(text);
            MultiMap<? extends ClassifiedAnnotation, Location> locations = PalladianLocationExtractor.fetchLocations(locationSource, classifiedEntities);

            Set<ClassifiableLocation> classifiableLocations = featureExtraction.extract(text, locations);
            Set<Instance> trainInstances = createTrainData(classifiableLocations, trainAnnotations);
            trainingData.addAll(trainInstances);
        }
        return trainingData;
    }

    private Set<Instance> createTrainData(Set<ClassifiableLocation> classifiableLocations, List<LocationAnnotation> positiveLocations) {
        Set<Instance> result = new HashSet<>();
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
                boolean samePlace = trainCoordinate != null && actualLocation.getCoordinate().distance(trainCoordinate) < MAX_DISTANCE;
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
        double positivePercentage = MathHelper.round((float) numPositive / classifiableLocations.size() * 100, 2);
        LOGGER.debug("{} positive instances in {} ({}%)", numPositive, classifiableLocations.size(), positivePercentage);
        return result;
    }

    public static void main(String[] args) throws IOException {
    	LocationSource locationSource = new LuceneLocationSource(FSDirectory.open(Paths.get("/Users/pk/Desktop/Location_Lab_Revisited/Palladian_Location_Database_2022-04-19_13-45-08")));
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource, DefaultCandidateExtractor.INSTANCE, 100,
                new ConfigurableFeatureExtractor());
        File datasetTud = new File("/Users/pk/Desktop/Location_Lab_Revisited/tud-loc-2013/1-training");
        File datasetLgl = new File("/Users/pk/Desktop/Location_Lab_Revisited/LGL-converted/1-train");
        File datasetClust = new File("/Users/pk/Desktop/Location_Lab_Revisited/CLUST-converted/1-train");
        QuickDtModel model;
        model = learner.learn(datasetTud);
        FileHelper.serialize(model, "locationDisambiguationModel-tudLoc2013-100trees.ser.gz");
        model = learner.learn(datasetLgl);
        FileHelper.serialize(model, "locationDisambiguationModel-lgl-100trees.ser.gz");
        model = learner.learn(datasetClust);
        FileHelper.serialize(model, "locationDisambiguationModel-clust-100trees.ser.gz");
        model = learner.learn(datasetTud, datasetLgl, datasetClust);
        FileHelper.serialize(model, "locationDisambiguationModel-all-100trees.ser.gz");
    }

}
