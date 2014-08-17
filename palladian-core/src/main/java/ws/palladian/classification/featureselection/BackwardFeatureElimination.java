package ws.palladian.classification.featureselection;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.dt.QuickDtClassifier;
import ws.palladian.classification.dt.QuickDtLearner;
import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Factories;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.math.ConfusionMatrix;

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
public final class BackwardFeatureElimination<M extends Model> extends AbstractFeatureRanker {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BackwardFeatureElimination.class);

    private final Factory<? extends Learner<M>> learnerFactory;

    private final Factory<? extends Classifier<M>> classifierFactory;

    private final Function<ConfusionMatrix, Double> scorer;

    private final int numThreads;

    public static final Function<ConfusionMatrix, Double> ACCURACY_SCORER = new Function<ConfusionMatrix, Double>() {
        @Override
        public Double compute(ConfusionMatrix input) {
            return input.getAccuracy();
        }
    };

    /**
     * <p>
     * A scorer using F1 measure for the specified class name.
     * </p>
     * 
     * @author pk
     */
    public static final class FMeasureScorer implements Function<ConfusionMatrix, Double> {

        private final String className;

        /**
         * @param className The name of the class for which to calculate F1 measure, not <code>null</code> or empty.
         */
        public FMeasureScorer(String className) {
            Validate.notEmpty(className, "className must not be empty");
            this.className = className;
        }

        @Override
        public Double compute(ConfusionMatrix input) {
            double value = input.getF(1., className);
            return Double.isNaN(value) ? 0 : value;
        }

        @Override
        public String toString() {
            return "FMeasureScorer [class=" + className + "]";
        }

    }

    private final class TestRun implements Callable<TestRunResult> {

        private final Iterable<? extends Instance> trainData;
        private final Iterable<? extends Instance> testData;
        private final List<String> featuresToEliminate;
        private final ProgressReporter progress;

        public TestRun(Iterable<? extends Instance> trainData, Iterable<? extends Instance> testData,
                List<String> featuresToEliminate, ProgressReporter progress) {
            this.trainData = trainData;
            this.testData = testData;
            this.featuresToEliminate = featuresToEliminate;
            this.progress = progress;
        }

        @Override
        public TestRunResult call() throws Exception {
            String eliminatedFeature = CollectionHelper.getLast(featuresToEliminate);
            LOGGER.debug("Starting elimination for {}", eliminatedFeature);

            Filter<String> filter = Filters.not(Filters.equal(featuresToEliminate));
            List<Instance> eliminatedTrainData = ClassificationUtils.filterFeatures(trainData, filter);
            List<Instance> eliminatedTestData = ClassificationUtils.filterFeatures(testData, filter);

            // create a new learner and classifier
            Learner<M> learner = learnerFactory.create();
            Classifier<M> classifier = classifierFactory.create();

            M model = learner.train(eliminatedTrainData);
            @SuppressWarnings("unchecked")
            ConfusionMatrix confusionMatrix = ClassifierEvaluation.evaluate(classifier, eliminatedTestData, model);
            Double score = scorer.compute(confusionMatrix);

            LOGGER.debug("Finished elimination for {}", eliminatedFeature);
            progress.increment();
            return new TestRunResult(score, eliminatedFeature);
        }

    }

    private static final class TestRunResult {
        private final Double score;
        private final String eliminatedFeature;

        public TestRunResult(Double score, String eliminatedFeature) {
            this.score = score;
            this.eliminatedFeature = eliminatedFeature;
        }
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
    public BackwardFeatureElimination(Learner<M> learner, Classifier<M> classifier, Function<ConfusionMatrix, Double> scorer) {
        this(Factories.constant(learner), Factories.constant(classifier), scorer, 1);
    }

    /**
     * <p>
     * Create a new {@link BackwardFeatureElimination} with the given learner and classifier. Not threading is used.
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
     * which feature to eliminate. The {@link Factory}s are necessary, because each thread needs its own {@link Learner}
     * and {@link Classifier}.
     * </p>
     * 
     * @param learnerFactory Factory for the learner, not <code>null</code>.
     * @param classifierFactory Factory for the classifier, not <code>null</code>.
     * @param scorer The function for determining the score, not <code>null</code>.
     * @param numThreads Use the specified number of threads to parallelize training/testing. Must be greater/equal one.
     */
    public BackwardFeatureElimination(Factory<? extends Learner<M>> learnerFactory,
            Factory<? extends Classifier<M>> classifierFactory, Function<ConfusionMatrix, Double> scorer, int numThreads) {
        Validate.notNull(learnerFactory, "learnerFactory must not be null");
        Validate.notNull(classifierFactory, "classifierFactory must not be null");
        Validate.notNull(scorer, "scorer must not be null");
        Validate.isTrue(numThreads > 0, "numThreads must be greater zero");
        this.learnerFactory = learnerFactory;
        this.classifierFactory = classifierFactory;
        this.scorer = scorer;
        this.numThreads = numThreads;
    }

    @Override
    public FeatureRanking rankFeatures(Collection<? extends Instance> dataset, ProgressReporter progress) {
        List<Instance> instances = new ArrayList<Instance>(dataset);
        Collections.shuffle(instances);
        List<Instance> trainData = instances.subList(0, instances.size() / 2);
        List<Instance> testData = instances.subList(instances.size() / 2, instances.size());
        return rankFeatures(trainData, testData, progress);
    }

    /**
     * <p>
     * Perform the backward feature elimination for the two specified training and validation set.
     * </p>
     * 
     * @param trainSet The training set, not <code>null</code>.
     * @param validationSet The validation/testing set, not <code>null</code>.
     * @param progress A progress instance.
     * @return A {@link FeatureRanking} containing the features in the order in which they were eliminated.
     */
    public FeatureRanking rankFeatures(Iterable<? extends Instance> trainSet,
            Iterable<? extends Instance> validationSet, ProgressReporter progress) {
        Map<String, Integer> ranks = CollectionHelper.newHashMap();

        Iterable<FeatureVector> trainingVectors = ClassificationUtils.unwrapInstances(trainSet);
        final Set<String> allFeatures = ClassificationUtils.getFeatureNames(trainingVectors);
        final List<String> eliminatedFeatures = CollectionHelper.newArrayList();
        final int iterations = allFeatures.size() * (allFeatures.size() + 1) / 2;
        progress.startTask("Backwards feature elimination", iterations);
        int featureIndex = 0;

        LOGGER.info("# of features in dataset: {}", allFeatures.size());
        LOGGER.info("# of iterations: {}", iterations);

        try {
            // run with all features
            TestRun initialRun = new TestRun(trainSet, validationSet, Arrays.asList("<none>"), progress);
            TestRunResult startScore = initialRun.call();
            LOGGER.info("Score with all features {}", startScore.score);

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            // stepwise elimination
            for (;;) {
                Set<String> featuresToCheck = new HashSet<String>(allFeatures);
                featuresToCheck.removeAll(eliminatedFeatures);
                if (featuresToCheck.isEmpty()) {
                    break;
                }
                List<TestRun> runs = CollectionHelper.newArrayList();

                for (String currentFeature : featuresToCheck) {
                    List<String> featuresToEliminate = new ArrayList<String>(eliminatedFeatures);
                    featuresToEliminate.add(currentFeature);
                    runs.add(new TestRun(trainSet, validationSet, featuresToEliminate, progress));
                }

                List<Future<TestRunResult>> runFutures = executor.invokeAll(runs);
                String selectedFeature = null;
                double highestScore = 0;
                for (Future<TestRunResult> future : runFutures) {
                    TestRunResult testRunResult = future.get();
                    if (testRunResult.score >= highestScore || selectedFeature == null) {
                        highestScore = testRunResult.score;
                        selectedFeature = testRunResult.eliminatedFeature;
                    }
                }

                LOGGER.info("Selected {} for elimination, score {}", selectedFeature, highestScore);
                eliminatedFeatures.add(selectedFeature);
                ranks.put(selectedFeature, featureIndex++);
            }

            executor.shutdown();

        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return new FeatureRanking(ranks);
    }

    public static void main(String[] args) {
        List<Instance> trainSet = new CsvDatasetReader(new File("/path/to/training.csv")).readAll();
        List<Instance> validationSet = new CsvDatasetReader(new File("/path/to/validation.csv")).readAll();

        // the classifier/predictor to use; when using threading, they have to be created through the factory, as we
        // require them for each thread
        Factory<QuickDtLearner> learnerFactory = new Factory<QuickDtLearner>() {
            @Override
            public QuickDtLearner create() {
                return QuickDtLearner.randomForest(10);
            }
        };
        // we can share this, because it has no state
        Factory<QuickDtClassifier> predictorFactory = Factories.constant(new QuickDtClassifier());

        // scoring function used for deciding which feature to eliminate; we use the F1 measure here, but in general all
        // measures as provided by the ConfusionMatrix can be used (e.g. accuracy, precision, ...).
        Function<ConfusionMatrix, Double> scorer = new FMeasureScorer("true");

        BackwardFeatureElimination<QuickDtModel> elimination = new BackwardFeatureElimination<QuickDtModel>(
                learnerFactory, predictorFactory, scorer, 1);
        FeatureRanking featureRanking = elimination.rankFeatures(trainSet, validationSet, new ProgressMonitor());
        CollectionHelper.print(featureRanking.getAll());
    }

}
