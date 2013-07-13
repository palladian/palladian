package ws.palladian.classification.featureselection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.Classifier;
import ws.palladian.classification.Learner;
import ws.palladian.classification.Model;
import ws.palladian.classification.dt.BaggedDecisionTreeClassifier;
import ws.palladian.classification.dt.BaggedDecisionTreeModel;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.EqualsFilter;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.collection.Function;
import ws.palladian.helper.collection.InverseFilter;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.Feature;

/**
 * <p>
 * Simple, greedy backward feature elimination. Results are scored using accuracy. Does not consider sparse data. Split
 * for train/test data is 50:50. Important: The scores in the {@link FeatureRanking} returned by this class are no
 * scores, but simple ranking values. This is because the features depend on each other.
 * </p>
 * 
 * @author Philipp Katz
 * @param <M> Type of the model.
 */
public final class BackwardFeatureElimination<M extends Model> implements FeatureRanker {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BackwardFeatureElimination.class);

    private final Learner<M> learner;

    private final Classifier<M> classifier;

    private final Function<ConfusionMatrix, Double> scorer;

    public static final Function<ConfusionMatrix, Double> ACCURACY_SCORER = new Function<ConfusionMatrix, Double>() {
        @Override
        public Double compute(ConfusionMatrix input) {
            return input.getAccuracy();
        }
    };

    /**
     * <p>
     * Create a new {@link BackwardFeatureElimination} with the given learner and classifier.
     * </p>
     * 
     * @param learner The learner, not <code>null</code>.
     * @param classifier The classifier, not <code>null</code>.
     */
    public BackwardFeatureElimination(Learner<M> learner, Classifier<M> classifier) {
        this(learner, classifier, ACCURACY_SCORER);
    }

    /**
     * <p>
     * Create a new {@link BackwardFeatureElimination} with the given learner and classifier. The scoring can be
     * parameterized through the {@link Function} argument; it must return a ranking value which is used to for deciding
     * which feature to eliminate.
     * </p>
     * 
     * @param learner The learner, not <code>null</code>.
     * @param classifier The classifier, not <code>null</code>.
     * @param scorer The function for determining the score, not <code>null</code>.
     */
    public BackwardFeatureElimination(Learner<M> learner, Classifier<M> classifier,
            Function<ConfusionMatrix, Double> scorer) {
        Validate.notNull(learner, "learner must not be null");
        Validate.notNull(classifier, "classifier must not be null");
        Validate.notNull(scorer, "scorer must not be null");
        this.learner = learner;
        this.classifier = classifier;
        this.scorer = scorer;
    }

    @Override
    public FeatureRanking rankFeatures(Collection<? extends Trainable> dataset) {
        List<Trainable> instances = new ArrayList<Trainable>(dataset);
        Collections.shuffle(instances);
        List<Trainable> trainData = instances.subList(0, instances.size() / 2);
        List<Trainable> testData = instances.subList(instances.size() / 2, instances.size());
        return rankFeatures(trainData, testData);
    }

    /**
     * <p>
     * Perform the backward feature elimination for the two specified training and validation set.
     * </p>
     * 
     * @param trainSet The training set, not <code>null</code>.
     * @param validationSet The validation/testing set, not <code>null</code>.
     * @return A {@link FeatureRanking} containing the features in the order in which they were eliminated.
     */
    public FeatureRanking rankFeatures(Collection<? extends Trainable> trainSet,
            Collection<? extends Trainable> validationSet) {
        final FeatureRanking result = new FeatureRanking();

        final Set<String> allFeatures = getFeatureNames(trainSet);
        final Set<String> eliminatedFeatures = CollectionHelper.newTreeSet();
        final int iterations = allFeatures.size() * (allFeatures.size() + 1) / 2;
        final StopWatch stopWatch = new StopWatch();
        int count = 0;
        int featureIndex = 0;

        // run with all features
        double startScore = testRun(trainSet, validationSet);
        LOGGER.info("Score with all features {}", startScore);

        // stepwise elimination
        for (;;) {
            Set<String> featuresToCheck = new HashSet<String>(allFeatures);
            featuresToCheck.removeAll(eliminatedFeatures);
            if (featuresToCheck.isEmpty()) {
                break;
            }
            String selectedFeature = null;
            double highestScore = 0;

            for (String currentFeature : featuresToCheck) {
                ProgressHelper.printProgress(count++, iterations, 0, stopWatch);
                Set<String> featuresToEliminate = new HashSet<String>(eliminatedFeatures);
                featuresToEliminate.add(currentFeature);
                Filter<String> filter = InverseFilter.create(EqualsFilter.create(featuresToEliminate));
                List<Trainable> eliminatedTrainData = ClassificationUtils.filterFeatures(trainSet, filter);
                List<Trainable> eliminatedTestData = ClassificationUtils.filterFeatures(validationSet, filter);
                double score = testRun(eliminatedTrainData, eliminatedTestData);
                // LOGGER.debug("Eliminating {} gives {}", currentFeature, score);
                if (score >= highestScore || selectedFeature == null) {
                    highestScore = score;
                    selectedFeature = currentFeature;
                }
            }
            LOGGER.info("Selected {} for elimination, score {}", selectedFeature, highestScore);
            eliminatedFeatures.add(selectedFeature);
            result.add(selectedFeature, featureIndex++);
        }
        return result;
    }

    private Set<String> getFeatureNames(Collection<? extends Trainable> dataset) {
        Set<String> featureNames = CollectionHelper.newTreeSet();
        Trainable instance = CollectionHelper.getFirst(dataset);
        for (Feature<?> feature : instance.getFeatureVector()) {
            featureNames.add(feature.getName());
        }
        return featureNames;
    }

    private double testRun(Collection<? extends Trainable> trainData, Collection<? extends Trainable> testData) {
        M model = learner.train(trainData);
        ConfusionMatrix confusionMatrix = ClassifierEvaluation.evaluate(classifier, model, testData);
        return scorer.compute(confusionMatrix);
    }

    public static void main(String[] args) {
        List<Trainable> trainSet = ClassificationUtils.readCsv("data/temp/ld_features_training.csv", true);
        List<Trainable> validationSet = ClassificationUtils.readCsv("data/temp/ld_features_validation.csv", true);

        // the classifier to use
        BaggedDecisionTreeClassifier classifier = new BaggedDecisionTreeClassifier();

        // scoring function used for deciding which feature to eliminate; we use the F1 measure here, but in general all
        // measures as provided by the ConfusionMatrix can be used (e.g. accuracy, precision, ...).
        Function<ConfusionMatrix, Double> scorer = new Function<ConfusionMatrix, Double>() {
            @Override
            public Double compute(ConfusionMatrix input) {
                return input.getF(1.0, "true");
            }
        };

        BackwardFeatureElimination<BaggedDecisionTreeModel> elimination = new BackwardFeatureElimination<BaggedDecisionTreeModel>(
                classifier, classifier, scorer);
        FeatureRanking featureRanking = elimination.rankFeatures(trainSet, validationSet);
        CollectionHelper.print(featureRanking.getAll());
    }

}
