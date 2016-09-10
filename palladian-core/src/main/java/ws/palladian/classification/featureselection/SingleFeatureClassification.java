package ws.palladian.classification.featureselection;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.nb.NaiveBayesClassifier;
import ws.palladian.classification.nb.NaiveBayesLearner;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;
import ws.palladian.core.dataset.split.RandomSplit;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.math.ClassificationEvaluator;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.ConfusionMatrixEvaluator;

/**
 * <p>
 * Feature ranking apprach which classifies with each single feature in isolation and the checks for performance
 * measures.
 * </p>
 * 
 * @author Philipp Katz
 * @param <M> Type of the model.
 */
public final class SingleFeatureClassification extends AbstractFeatureRanker {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleFeatureClassification.class);
    
    private static final class EvaluatorAndMapper<R, M extends Model> {
    	private final Learner<M> learner;
    	private final Classifier<M> classifier;
    	private final ClassificationEvaluator<R> evaluator;
    	private final Function<R, Double> mapper;
		EvaluatorAndMapper(Learner<M> learner, Classifier<M> classifier, ClassificationEvaluator<R> evaluator, Function<R, Double> mapper) {
			this.learner = learner;
			this.classifier = classifier;
			this.evaluator = evaluator;
			this.mapper = mapper;
		}
		Double evaluate(Dataset trainingData, Dataset testingData) {
            M model = learner.train(trainingData);
			R evaluationResult = evaluator.evaluate(classifier, model, testingData);
			return mapper.compute(evaluationResult);
		}
    }

	private final EvaluatorAndMapper<?, ?> evaluatorAndMapper;

    /**
     * @param learner The learner, not <code>null</code>.
     * @param classifier The classifier, not <code>null</code>.
     * @param evaluator The evaluation logic.
     * @param mapper Mapping from the evaluation output to a ranking value. 
     */
    public <R, M extends Model> SingleFeatureClassification(Learner<M> learner, Classifier<M> classifier,
    		ClassificationEvaluator<R> evaluator, Function<R, Double> mapper) {
    	Validate.notNull(learner, "learner must not be null");
    	Validate.notNull(classifier, "classifier must not be null");
    	Validate.notNull(evaluator, "evaluator must not be null");
    	Validate.notNull(mapper, "mapper must not be null");
    	this.evaluatorAndMapper = new EvaluatorAndMapper<>(learner, classifier, evaluator, mapper);
    }
    
    /**
     * @param learner The learner, not <code>null</code>.
     * @param classifier The classifier, not <code>null</code>.
     * @param scorer The function for determining the score, not <code>null</code>.
     * @deprecated Use {@link #SingleFeatureClassification(Learner, Classifier, ClassificationEvaluator, Function)} instead.
     */
    @Deprecated
    public <M extends Model> SingleFeatureClassification(Learner<M> learner, Classifier<M> classifier,
            Function<ConfusionMatrix, Double> scorer) {
    	this(learner, classifier, new ConfusionMatrixEvaluator(), scorer);
    }
    
    @Override
    public FeatureRanking rankFeatures(Dataset dataset, ProgressReporter progress) {
    	RandomSplit split = new RandomSplit(dataset, 0.5);
        return rankFeatures(split.getTrain(), split.getTest());
    }
    
    /** @deprecated Use {@link #rankFeatures(Dataset, Dataset)} instead. */
    @Deprecated
    public FeatureRanking rankFeatures(Iterable<? extends Instance> trainSet, Iterable<? extends Instance> validationSet) {
    	return rankFeatures(new DefaultDataset(trainSet), new DefaultDataset(validationSet));
    }

    /**
     * @param trainSet The training set, not <code>null</code>.
     * @param validationSet The validation/testing set, not <code>null</code>.
     * @return A {@link FeatureRanking} containing the features in the order in which they were eliminated.
     */
    public FeatureRanking rankFeatures(Dataset trainSet, Dataset validationSet) {
        Map<String, Double> scores = new HashMap<>();

        final Set<String> allFeatures = trainSet.getFeatureInformation().getFeatureNames();
        final ProgressReporter progressMonitor = new ProgressMonitor();
        progressMonitor.startTask("Single feature classification", allFeatures.size());

        for (String feature : allFeatures) {
            Filter<String> filter = Filters.equal(feature);
            Dataset eliminatedTrainData = trainSet.filterFeatures(filter);
            Dataset eliminatedTestData = validationSet.filterFeatures(filter);

            Double score = evaluatorAndMapper.evaluate(eliminatedTrainData, eliminatedTestData);
            LOGGER.info("Finished testing with {}: {}", feature, score);
            progressMonitor.increment();
            scores.put(feature, score);
        }
        return new FeatureRanking(scores);
    }

    public static void main(String[] args) {
        Dataset trainSet = CsvDatasetReaderConfig.filePath(new File("/Users/pk/Dropbox/LocationExtraction/BFE/fd_merged_train.csv")).create();
        Dataset validationSet = CsvDatasetReaderConfig.filePath(new File("/Users/pk/Dropbox/LocationExtraction/BFE/fd_merged_validation.csv")).create();

        // the classifier/predictor to use; when using threading, they have to be created through the factory, as we
        // require them for each thread
        // BaggedDecisionTreeClassifier classifier = new BaggedDecisionTreeClassifier();
        NaiveBayesLearner learner = new NaiveBayesLearner();
        NaiveBayesClassifier classifier = new NaiveBayesClassifier();

        // scoring function used for deciding which feature to eliminate; we use the F1 measure here, but in general all
        // measures as provided by the ConfusionMatrix can be used (e.g. accuracy, precision, ...).
        Function<ConfusionMatrix, Double> scorer = new Function<ConfusionMatrix, Double>() {
            @Override
            public Double compute(ConfusionMatrix input) {
                double value = input.getF(1.0, "true");
                return Double.isNaN(value) ? 0 : value;
            }
        };

		SingleFeatureClassification elimination = new SingleFeatureClassification(learner, classifier, scorer);
        FeatureRanking featureRanking = elimination.rankFeatures(trainSet, validationSet);
        CollectionHelper.print(featureRanking.getAll());
    }

}
