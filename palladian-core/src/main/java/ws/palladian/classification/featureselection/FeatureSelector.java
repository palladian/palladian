package ws.palladian.classification.featureselection;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.classification.dt.QuickDtClassifier;
import ws.palladian.classification.dt.QuickDtLearner;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Factories;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Predicates;
import ws.palladian.helper.math.ConfusionMatrix;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;

import static ws.palladian.helper.functional.Predicates.*;

/**
 * <p>
 * Simple, greedy backward feature elimination. Results are scored using accuracy. Does not consider sparse data. Split
 * for train/test data is 50:50. Important: The scores in the {@link FeatureRanking} returned by this class are no
 * scores, but simple ranking values. This is because the features depend on each other.
 * </p>
 *
 * @param <M> Type of the model.
 * @author Philipp Katz
 */
public final class FeatureSelector extends AbstractFeatureRanker {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureSelector.class);

    private final FeatureSelectorConfig config;

    /** Use {@link FeatureSelectorConfig.Builder#scoreAccuracy()} instead. */
    @Deprecated
    public static final Function<ConfusionMatrix, Double> ACCURACY_SCORER = new Function<ConfusionMatrix, Double>() {
        @Override
        public Double apply(ConfusionMatrix input) {
            return input.getAccuracy();
        }
    };

    /**
     * <p>
     * A scorer using F1 measure for the specified class name.
     * </p>
     *
     * @author Philipp Katz
     * @deprecated Use {@link FeatureSelectorConfig.Builder#scoreF1(String)} instead.
     */
    @Deprecated
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
        public Double apply(ConfusionMatrix input) {
            double value = input.getF(1., className);
            return Double.isNaN(value) ? 0 : value;
        }

        @Override
        public String toString() {
            return "FMeasureScorer [class=" + className + "]";
        }

    }

    private final class TestRun implements Callable<TestRunResult> {

        private final Dataset trainData;
        private final Dataset testData;
        private final Predicate<? super String> features;
        private final Predicate<? super String> evaluatedFeature;
        private final ProgressReporter progress;

        public TestRun(Dataset trainData, Dataset testData, Predicate<? super String> features, Predicate<? super String> evaluatedFeature, ProgressReporter progress) {
            this.trainData = trainData;
            this.testData = testData;
            this.features = features;
            this.evaluatedFeature = evaluatedFeature;
            this.progress = progress;
        }

        @Override
        public TestRunResult call() throws Exception {
            LOGGER.trace("Starting evaluation for {}", features);

            Dataset eliminatedTrainData = trainData.filterFeatures(features);
            Dataset eliminatedTestData = testData.filterFeatures(features);

            Double score = config.evaluator().score(eliminatedTrainData, eliminatedTestData);

            LOGGER.debug("Finished evaluation for {}, score {}", features, score);
            progress.increment();
            return new TestRunResult(score, evaluatedFeature);
        }

    }

    private static final class TestRunResult {
        private final Double score;
        private final Predicate<? super String> evaluatedFeature;

        public TestRunResult(Double score, Predicate<? super String> evaluatedFeature) {
            this.score = score;
            this.evaluatedFeature = evaluatedFeature;
        }
    }

    /**
     * Configuration constructor; used by
     * {@link FeatureSelectorConfig.Builder}.
     */
    FeatureSelector(FeatureSelectorConfig config) {
        this.config = config;
    }

    /**
     * <p>
     * Create a new {@link FeatureSelector} with the given learner and classifier. The scoring can be
     * parameterized through the {@link Function} argument; it must return a ranking value which is used to for deciding
     * which feature to eliminate.
     * </p>
     *
     * @param learner    The learner, not <code>null</code>.
     * @param classifier The classifier, not <code>null</code>.
     * @param scorer     The function for determining the score, not <code>null</code>.
     * @deprecated Use the {@link FeatureSelectorConfig.Builder} instead.
     */
    @Deprecated
    public <M extends Model> FeatureSelector(Learner<M> learner, Classifier<M> classifier, Function<ConfusionMatrix, Double> scorer) {
        this(FeatureSelectorConfig.with(learner, classifier).scorer(scorer).createConfig());
    }

    /**
     * <p>
     * Create a new {@link FeatureSelector} with the given learner and classifier. Not threading is used.
     * </p>
     *
     * @param learner    The learner, not <code>null</code>.
     * @param classifier The classifier, not <code>null</code>.
     * @deprecated Use the {@link FeatureSelectorConfig.Builder} instead.
     */
    @Deprecated
    public <M extends Model> FeatureSelector(Learner<M> learner, Classifier<M> classifier) {
        this(FeatureSelectorConfig.with(learner, classifier).scoreAccuracy().createConfig());
    }

    /**
     * <p>
     * Create a new {@link FeatureSelector} with the given learner and classifier. The scoring can be
     * parameterized through the {@link Function} argument; it must return a ranking value which is used to for deciding
     * which feature to eliminate. The {@link Factory}s are necessary, because each thread needs its own {@link Learner}
     * and {@link Classifier}.
     * </p>
     *
     * @param learnerFactory    Factory for the learner, not <code>null</code>.
     * @param classifierFactory Factory for the classifier, not <code>null</code>.
     * @param scorer            The function for determining the score, not <code>null</code>.
     * @param numThreads        Use the specified number of threads to parallelize training/testing. Must be greater/equal one.
     * @deprecated Use the {@link FeatureSelectorConfig.Builder} instead.
     */
    @Deprecated
    public <M extends Model> FeatureSelector(Factory<? extends Learner<M>> learnerFactory, Factory<? extends Classifier<M>> classifierFactory,
            Function<ConfusionMatrix, Double> scorer, int numThreads) {
        this(FeatureSelectorConfig.with(learnerFactory, classifierFactory).scorer(scorer).numThreads(numThreads).createConfig());
    }

    /** @deprecated Use {@link #rankFeatures(Dataset, Dataset, ProgressReporter)} instead. */
    @Deprecated
    public FeatureRanking rankFeatures(Iterable<? extends Instance> trainSet, Iterable<? extends Instance> validationSet, ProgressReporter progress) {
        return rankFeatures(new DefaultDataset(trainSet), new DefaultDataset(validationSet), progress);
    }

    /**
     * <p>
     * Perform the backward feature elimination for the two specified training and validation set.
     * </p>
     *
     * @param trainSet      The training set, not <code>null</code>.
     * @param validationSet The validation/testing set, not <code>null</code>.
     * @param progress      A progress instance.
     * @return A {@link FeatureRanking} containing the features in the order in which they were eliminated.
     */
    @Override
    public FeatureRanking rankFeatures(Dataset trainSet, Dataset validationSet, ProgressReporter progress) {
        Map<String, Integer> ranks = new HashMap<>();

        final Set<Predicate<? super String>> allFeatureFilters = constructFeatureFilters(trainSet);
        final List<Predicate<? super String>> selectedFeatures = new ArrayList<>();
        final int iterations = allFeatureFilters.size() * (allFeatureFilters.size() + 1) / 2;
        progress.startTask("Feature selection", iterations);
        int featureIndex = config.isBackward() ? 0 : allFeatureFilters.size();

        LOGGER.info("# of features or feature sets: {}", allFeatureFilters.size());
        LOGGER.info("# of iterations: {}", iterations);

        try {
            if (config.isBackward()) {
                // run with all features
                TestRun initialRun = new TestRun(trainSet, validationSet, Predicates.ALL, Predicates.NONE, progress);
                TestRunResult startScore = initialRun.call();
                LOGGER.info("Score with all features {}", startScore.score);
            }

            ExecutorService executor = Executors.newFixedThreadPool(config.numThreads());

            // stepwise elimination
            for (; ; ) {
                Set<Predicate<? super String>> featuresToCheck = new HashSet<>(allFeatureFilters);
                featuresToCheck.removeAll(selectedFeatures);
                if (featuresToCheck.isEmpty()) {
                    break;
                }
                List<TestRun> runs = new ArrayList<>();

                for (Predicate<? super String> currentFeature : featuresToCheck) {
                    List<Predicate<? super String>> currentRunFeatures = new ArrayList<>(selectedFeatures);
                    currentRunFeatures.add(currentFeature);
                    Predicate<String> featureFilter = or(currentRunFeatures);
                    if (config.isBackward()) {
                        featureFilter = not(featureFilter);
                    }
                    runs.add(new TestRun(trainSet, validationSet, featureFilter, currentFeature, progress));
                }

                List<Future<TestRunResult>> runFutures = executor.invokeAll(runs);
                Predicate<? super String> selectedFeature = null;
                double highestScore = 0;
                for (Future<TestRunResult> future : runFutures) {
                    TestRunResult testRunResult = future.get();
                    if (testRunResult.score >= highestScore || selectedFeature == null) {
                        highestScore = testRunResult.score;
                        selectedFeature = testRunResult.evaluatedFeature;
                    }
                }

                LOGGER.info("Selected {}, score {}", selectedFeature, highestScore);
                selectedFeatures.add(selectedFeature);
                ranks.put(selectedFeature.toString(), featureIndex += config.isBackward() ? 1 : -1);
            }

            executor.shutdown();

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return new FeatureRanking(ranks);
    }

    /**
     * Check which features are matched by any of the filters, then construct
     * individual (singleton) filters for all remaining features. The result is
     * a collection of filters, where the union of all filter results represents
     * the entire feature set.
     *
     * @param data The dataset.
     * @return A set of filters for every feature within the dataset.
     */
    private Set<Predicate<? super String>> constructFeatureFilters(Dataset dataset) {
        // XXX check, whether the filters are disjunct?
        Set<Predicate<? super String>> filters = new HashSet<>(config.featureGroups());
        Set<String> allFeatures = dataset.getFeatureInformation().getFeatureNames();
        Iterable<String> unmatchedFeatures = CollectionHelper.filter(allFeatures, not(or(config.featureGroups())));
        for (String unmatchedFeature : unmatchedFeatures) {
            filters.add(equal(unmatchedFeature));
        }
        return filters;
    }

    public static void main(String[] args) {
        Dataset trainSet = CsvDatasetReaderConfig.filePath(new File("/path/to/training.csv")).create();
        Dataset validationSet = CsvDatasetReaderConfig.filePath(new File("/path/to/validation.csv")).create();

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

        FeatureSelector elimination = new FeatureSelector(learnerFactory, predictorFactory, scorer, 1);
        FeatureRanking featureRanking = elimination.rankFeatures(trainSet, validationSet, new ProgressMonitor());
        CollectionHelper.print(featureRanking.getAll());
    }

}
