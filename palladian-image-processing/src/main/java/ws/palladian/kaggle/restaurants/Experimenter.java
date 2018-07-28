package ws.palladian.kaggle.restaurants;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Classifier;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DatasetTransformer;
import ws.palladian.core.dataset.DatasetWithFeatureAsCategory;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.kaggle.restaurants.dataset.Label;
import ws.palladian.kaggle.restaurants.utils.ClassifierCombination;
import ws.palladian.kaggle.restaurants.utils.ClassifierCombination.EvaluationResult;
import ws.palladian.kaggle.restaurants.utils.IdentityDatasetTransformer;

/**
 * Batch evaluation of binary classification problems.
 * 
 * @author pk
 */
public class Experimenter {
	
	private static final class Experiment {
		final ClassifierCombination<?> classifierCombination;
		final Collection<? extends Filter<? super String>> featureSets;
		Experiment(ClassifierCombination<?> classifierCombination, Collection<? extends Filter<? super String>> featureSets) {
			this.classifierCombination = Objects.requireNonNull(classifierCombination);
			this.featureSets = Objects.requireNonNull(featureSets);
		}
	}

	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(Experimenter.class);

	private final Dataset training;

	private final Dataset testing;

	private final File resultsDirectory;

	private final List<String> classLabels = new ArrayList<>();
	
	private final List<Experiment> experiments = new ArrayList<>();

	// XXX should go into Experiment
	private final List<DatasetTransformer> transformers = new ArrayList<>();
	
	private final String trueClass;

	/**
	 * @param training
	 *            The training data.
	 * @param testing
	 *            The testing data.
	 * @param resultsDirectory
	 *            The directory where result CSV will be stored.
	 */
	public Experimenter(Dataset training, Dataset testing, File resultsDirectory) {
		this(training, testing, resultsDirectory, "true");
	}
	
	/**
	 * @param training
	 *            The training data.
	 * @param testing
	 *            The testing data.
	 * @param resultsDirectory
	 *            The directory where result CSV will be stored.
	 * @param trueClass
	 *            Value of the true class.
	 */
	public Experimenter(Dataset training, Dataset testing, File resultsDirectory,String trueClass) {
		this.training = Objects.requireNonNull(training);
		this.testing = Objects.requireNonNull(testing);
		this.resultsDirectory = Objects.requireNonNull(resultsDirectory);
		// this.transformers.add(IdentityDatasetTransformer.INSTANCE);
		this.trueClass = trueClass;
	}

	/**
	 * @param classLabels
	 *            The classes on which to evaluate.
	 * @return This instance.
	 */
	public Experimenter withClassLabel(Label label) {
		classLabels.add(label.toString());
		return this;
	}
	
	public Experimenter withClassLabels(Label...labels) {
		classLabels.addAll(Arrays.stream(labels).map(l -> l.toString()).collect(toList()));
		return this;
	}
	
	/**
	 * Allows to perform additional experiments with a transformed feature set.
	 * 
	 * @param transformer
	 *            The transformer for the features.
	 * @return This instance.
	 */
	// TODO transformers should be applied before feature filters
	public Experimenter withTransformer(DatasetTransformer transformer) {
		transformers.add(transformer);
		return this;
	}

	/**
	 * @param learner
	 *            The learner.
	 * @param classifier
	 *            The classifier.
	 * @param featureSets
	 *            Filters for the features to use.
	 * @return This instance.
	 */
	public <M extends Model> Experimenter withClassifier(Learner<M> learner, Classifier<M> classifier, Collection<? extends Filter<? super String>> featureSets) {
		experiments.add(new Experiment(new ClassifierCombination<>(learner, classifier), featureSets));
		return this;
	}
	
	public <M extends Model> Experimenter withClassifier(Learner<M> learner, Classifier<M> classifier, Filter<? super String> featureSet) {
		return withClassifier(learner, classifier, Collections.singleton(featureSet));
	}

	/**
	 * @param learnerClassifier
	 *            The learner and classifier.
	 * @param featureSets
	 *            Filters for the features to use.
	 * @return This instance.
	 */
	public <LC extends Learner<M> & Classifier<M>, M extends Model> Experimenter withClassifier(LC learnerClassifier, Collection<? extends Filter<? super String>> featureSets) {
		experiments.add(new Experiment(new ClassifierCombination<>(learnerClassifier), featureSets));
		return this;
	}

	/**
	 * Run the evaluation.
	 */
	public void run() {
		run(false);
	}
	
	/**
	 * Do not run the evaluation, but just print out all classifier/feature
	 * set/class label combination which would be evaluated.
	 */
	public void dryRun() {
		run(true);
	}
	
	private void run(boolean dryRun) {
		if (!classLabels.isEmpty()) {
			LOGGER.info("# class labels: {}", classLabels.size());
		}
		LOGGER.info("# total combinations: {}", getNumCombinations());
		ProgressReporter progress = new ProgressMonitor();
		progress.startTask("Experiments", getNumCombinations());
		if (classLabels.isEmpty()) {
			runExpriments(progress, null, training, testing, dryRun);
		} else {
			for (String classLabel : classLabels) {
				if (dryRun) {
					System.out.println("class label: " + classLabel);
				}
				Dataset classTraining = new DatasetWithFeatureAsCategory(training, classLabel);
				Dataset classTesting = new DatasetWithFeatureAsCategory(testing, classLabel);
				runExpriments(progress, classLabel, classTraining, classTesting, dryRun);
			}
		}
	}

	public int getNumCombinations() {
		int numCombinations = 0;
		int numClassLabels = classLabels.size() > 0 ? classLabels.size() : 1;
		int numTransformers = transformers.size() > 0 ? transformers.size() : 1;
		for (Experiment experiment : experiments) {
			numCombinations += numClassLabels * experiment.featureSets.size() * numTransformers;
		}
		return numCombinations;
	}

	private void runExpriments(ProgressReporter progress, String classLabel, Dataset classTraining,
			Dataset classTesting, boolean dryRun) {
		// in case no transformer was specified, add the "identity transformer"
		List<DatasetTransformer> transformers = new ArrayList<>(this.transformers);
		if (this.transformers.isEmpty()) {
			transformers.add(IdentityDatasetTransformer.INSTANCE);
		}
		for (Experiment experiment : experiments) {
			if (dryRun) {
				System.out.println("\tclassifier: " + experiment.classifierCombination);
			}
			for (Filter<? super String> featureSet : experiment.featureSets) {
				Dataset experimentTraining = classTraining.filterFeatures(featureSet);
				Dataset experimentTesting = classTesting.filterFeatures(featureSet);
				Set<String> featureNames = experimentTraining.getFeatureInformation().getFeatureNames();
				if (dryRun) {
					System.out.println("\t\tfeature set: " + featureSet + " (" + featureNames.size() + ")");
				}
				for (DatasetTransformer transformer : transformers) {
					if (dryRun) {
						if (transformers.size() > 1) {
							System.out.println("\t\t\ttransformer: " + transformer);
						}
						continue;
					}
					Dataset experimentTrainingTransformed = experimentTraining.transform(transformer);
					Dataset experimentTestingTransformed = experimentTesting.transform(transformer);
					EvaluationResult<?> evaluationResult = experiment.classifierCombination.runEvaluation(experimentTrainingTransformed, experimentTestingTransformed, trueClass);
					ConfusionMatrix confusionMatrix = evaluationResult.getConfusionMatrix();
					StringBuilder result = new StringBuilder();
					if (classLabel != null) {
						result.append("Class:       ").append(classLabel).append('\n');
					}
					result.append("Learner:     ").append(experiment.classifierCombination.getLearner()).append('\n');
					result.append("Classifier:  ").append(experiment.classifierCombination.getClassifier()).append('\n');
					result.append("\n\n");
					result.append("Features:    ").append(featureNames.size()).append('\n');
					result.append("Filter:      ").append(featureSet).append('\n');
					result.append("Transformer: ").append(transformer).append('\n');
					result.append('\n');
					for (String featureName : featureNames) {
						result.append(featureName).append('\n');
					}
					result.append('\n');
					long secondsTraining = MILLISECONDS.toSeconds(evaluationResult.getTrainingTime());
					long secondsTesting = MILLISECONDS.toSeconds(evaluationResult.getTestingTime());
					result.append("Training:    ").append(secondsTraining).append(" seconds\n");
					result.append("Testing:     ").append(secondsTesting).append(" seconds\n");
					result.append("\n\n");
					
					result.append("ROC AUC:     ").append(evaluationResult.getRocCurves().getAreaUnderCurve());
					result.append("\n\n");
					
					result.append(confusionMatrix.toString());
					
					result.append("\n\n").append("Threshold analysis:\n");
					result.append(evaluationResult.getThresholdAnalyzer().toString());
					
					String timestamp = DateHelper.getCurrentDatetime();
					File resultFile = new File(resultsDirectory, "result-" + timestamp + ".txt");
					FileHelper.writeToFile(resultFile.getAbsolutePath(), result);
					
					// summary CSV
					File summaryCsv = new File(resultsDirectory, "_summary.csv");
					StringBuilder csvResult = new StringBuilder();
					if (!summaryCsv.exists()) {
						if (classLabel != null) {
							csvResult.append("classLabel;");
						}
						csvResult.append("details;learner;classifier;featureSet;numFeatures;transformer;timeTraining;timeTesting;precision;recall;f1;accuracy;superiority;matthewsCorrelationCoefficient;rocAuc\n");
					}
					if (classLabel != null) {
						csvResult.append(classLabel).append(';');
					}
					csvResult.append(resultFile.getName()).append(';');
					csvResult.append(experiment.classifierCombination.getLearner()).append(';');
					csvResult.append(experiment.classifierCombination.getClassifier()).append(';');
					csvResult.append(featureSet).append(';');
					csvResult.append(featureNames.size()).append(';');
					String transformerString = transformer.toString();
					int newlineIdx = transformer.toString().indexOf('\n');
					if (newlineIdx != -1) {
						transformerString = transformerString.substring(0, newlineIdx);
					}
					csvResult.append(transformerString).append(';');
					csvResult.append(secondsTraining).append(';');
					csvResult.append(secondsTesting).append(';');
					csvResult.append(confusionMatrix.getPrecision(trueClass)).append(';');
					csvResult.append(confusionMatrix.getRecall(trueClass)).append(';');
					csvResult.append(confusionMatrix.getF(1, trueClass)).append(';');
					csvResult.append(confusionMatrix.getAccuracy()).append(';');
					csvResult.append(confusionMatrix.getSuperiority()).append(';');
					csvResult.append(confusionMatrix.getMatthewsCorrelationCoefficient()).append(';');
					csvResult.append(evaluationResult.getRocCurves().getAreaUnderCurve()).append('\n');
					FileHelper.appendFile(summaryCsv.getAbsolutePath(), csvResult);
					
					// write the model
					File serializedFile = new File(resultsDirectory, "model-" + timestamp + ".ser.gz");
					FileHelper.trySerialize(evaluationResult.getModel(), serializedFile.getAbsolutePath());
					
					// write the ROC curves
					try {
						evaluationResult.getRocCurves().saveCurves(new File(resultsDirectory, "roc-" + timestamp + ".png"));
					} catch (IOException e) {
						throw new IllegalStateException("Could not save ROC curves", e);
					} catch (Throwable t) {
						LOGGER.warn("Could not save ROC curves", t);
					}
					
					progress.increment();
					
				}
			}
		}
	}

}
