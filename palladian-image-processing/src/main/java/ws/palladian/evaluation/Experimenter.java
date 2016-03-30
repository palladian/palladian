package ws.palladian.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.functional.Functions;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.utils.ClassifierCombination;
import ws.palladian.utils.ClassifierCombination.EvaluationResult;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ws.palladian.classification.utils.ClassificationUtils.filterFeaturesIterable;
import static ws.palladian.classification.utils.ClassificationUtils.useFeatureAsCategory;

/**
 * Run experiments and output results and models.
 * @author Philipp Katz
 * @author David Urbansky
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

	private final Iterable<Instance> training;

	private final Iterable<Instance> testing;

    /** Describe the experiments. */
    private String description;

	private final File resultsDirectory;

	private final List<String> classLabels = new ArrayList<>();

	private final List<Experiment> experiments = new ArrayList<>();

	// XXX should go into Experiment
	private final List<Function<Instance, Instance>> transformers = new ArrayList<>();

	/**
	 * @param training
	 *            The training data.
	 * @param testing
	 *            The testing data.
	 * @param resultsDirectory
	 *            The directory where result CSV will be stored.
	 */
	public Experimenter(Iterable<Instance> training, Iterable<Instance> testing, File resultsDirectory) {
		this.training = Objects.requireNonNull(training);
		this.testing = Objects.requireNonNull(testing);
		this.resultsDirectory = Objects.requireNonNull(resultsDirectory);
		this.transformers.add(Functions.identity());
	}

	/**
	 * Allows to perform additional experiments with a transformed feature set.
	 * 
	 * @param transformer
	 *            The transformer for the features.
	 * @return This instance.
	 */
	public Experimenter addTransformer(Function<Instance, Instance> transformer) {
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
	public <M extends Model> Experimenter addClassifier(Learner<M> learner, Classifier<M> classifier, Collection<? extends Filter<? super String>> featureSets) {
		experiments.add(new Experiment(new ClassifierCombination<>(learner, classifier), featureSets));
		return this;
	}

	/**
	 * @param learnerClassifier
	 *            The learner and classifier.
	 * @param featureSets
	 *            Filters for the features to use.
	 * @return This instance.
	 */
	public <LC extends Learner<M> & Classifier<M>, M extends Model> Experimenter addClassifier(LC learnerClassifier, Collection<? extends Filter<? super String>> featureSets) {
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
				Iterable<Instance> classTraining = useFeatureAsCategory(training, classLabel);
				Iterable<Instance> classTesting = useFeatureAsCategory(testing, classLabel);
				runExpriments(progress, classLabel, classTraining, classTesting, dryRun);
			}
		}
	}

	public int getNumCombinations() {
		int numCombinations = 0;
		int numClassLabels = classLabels.size() > 0 ? classLabels.size() : 1;
		for (Experiment experiment : experiments) {
			numCombinations += numClassLabels * experiment.featureSets.size() * transformers.size();
		}
		return numCombinations;
	}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private void runExpriments(ProgressReporter progress, String classLabel, Iterable<Instance> classTraining,
			Iterable<Instance> classTesting, boolean dryRun) {
		for (Experiment experiment : experiments) {
			if (dryRun) {
				System.out.println("\tclassifier: " + experiment.classifierCombination);
			}
			for (Filter<? super String> featureSet : experiment.featureSets) {
				Iterable<Instance> experimentTraining = filterFeaturesIterable(classTraining, featureSet);
				Iterable<Instance> experimentTesting = filterFeaturesIterable(classTesting, featureSet);
				Set<String> featureNames = ClassificationUtils
						.getFeatureNames(ClassificationUtils.unwrapInstances(experimentTraining));
				if (dryRun) {
					System.out.println("\t\tfeature set: " + featureSet + " (" + featureNames.size() + ")");
				}
				for (Function<Instance, Instance> transformer : transformers) {
					if (dryRun) {
						if (transformers.size() > 1) {
							System.out.println("\t\t\ttransformer: " + transformer);
						}
						continue;
					}
					Iterable<Instance> experimentTrainingTransformed = CollectionHelper.convert(experimentTraining, transformer);
					Iterable<Instance> experimentTestingTransformed = CollectionHelper.convert(experimentTesting, transformer);
					EvaluationResult<?> evaluationResult = experiment.classifierCombination.runEvaluation(experimentTrainingTransformed, experimentTestingTransformed);
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
					result.append("Description: ").append(getDescription()).append('\n');
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

					if (evaluationResult.getRocCurves() != null) {
						result.append("ROC AUC:     ").append(evaluationResult.getRocCurves().getAreaUnderCurve());
						result.append("\n\n");
					}
					
					result.append(confusionMatrix.toString());

					if (evaluationResult.getThresholdAnalyzer() != null) {
						result.append("\n\n").append("Threshold analysis:\n");
						result.append(evaluationResult.getThresholdAnalyzer().toString());
					}
					
					String timestamp = DateHelper.getCurrentDatetime();
					File resultFile = new File(resultsDirectory, "result-" + timestamp + ".txt");
					FileHelper.writeToFile(resultFile.getAbsolutePath(), result);
					
					// summary CSV
					File summaryCsv = new File(resultsDirectory, "_summary.csv");
					StringBuilder csvResult = new StringBuilder();
					if (!summaryCsv.exists()) {
						csvResult.append("classLabel;details;learner;classifier;featureSet;description;numFeatures;transformer;timeTraining;timeTesting;precision;recall;f1;accuracy;superiority");
                        if (evaluationResult.getModel().getCategories().size() == 2) {
                            csvResult.append(";matthewsCorrelationCoefficient;rocAuc");
                        }
                        csvResult.append("\n");
					}
					csvResult.append(classLabel).append(';');
					csvResult.append(resultFile.getName()).append(';');
					csvResult.append(experiment.classifierCombination.getLearner()).append(';');
					csvResult.append(experiment.classifierCombination.getClassifier()).append(';');
					csvResult.append(featureSet).append(';');
					csvResult.append(getDescription().replace(";",",")).append(';');
					csvResult.append(featureNames.size()).append(';');
					csvResult.append(transformer).append(';');
					csvResult.append(secondsTraining).append(';');
					csvResult.append(secondsTesting).append(';');
					csvResult.append(confusionMatrix.getPrecision("true")).append(';');
					csvResult.append(confusionMatrix.getRecall("true")).append(';');
					csvResult.append(confusionMatrix.getF(1, "true")).append(';');
					csvResult.append(confusionMatrix.getAccuracy()).append(';');
					csvResult.append(confusionMatrix.getSuperiority()).append(';');
					if (evaluationResult.getModel().getCategories().size() == 2) {
						csvResult.append(confusionMatrix.getMatthewsCorrelationCoefficient()).append(';');
						csvResult.append(evaluationResult.getRocCurves().getAreaUnderCurve()).append('\n');
					} else {
                        csvResult.append('\n');
                    }
					FileHelper.appendFile(summaryCsv.getAbsolutePath(), csvResult);

                    // write separate confusion matrix (to be opened with csv programs)
                    FileHelper.writeToFile(resultsDirectory.getAbsolutePath() + File.separator + "confusion-" + timestamp + ".tsv",confusionMatrix.toString(true));

					// write the model
					File serializedFile = new File(resultsDirectory, "model-" + timestamp + ".ser.gz");
					FileHelper.trySerialize(evaluationResult.getModel(), serializedFile.getAbsolutePath());
					
					// write the ROC curves
					if (evaluationResult.getRocCurves() != null) {
						try {
							evaluationResult.getRocCurves().saveCurves(new File(resultsDirectory, "roc-" + timestamp + ".png"));
						} catch (IOException e) {
							throw new IllegalStateException("Could not save ROC curves", e);
						}
					}

					progress.increment();
					
				}
			}
		}
	}

}
