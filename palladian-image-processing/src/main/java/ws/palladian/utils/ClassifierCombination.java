package ws.palladian.utils;

import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.ThresholdAnalyzer;

public final class ClassifierCombination<M extends Model> {
	public static final class EvaluationResult<M extends Model> {
		private final ConfusionMatrix confusionMatrix;
		private final M model;
		private final long trainingTime;
		private final long testingTime;
		private final ThresholdAnalyzer thresholdAnalyzer;
		private RocCurves rocCurves;
		EvaluationResult(ConfusionMatrix confusionMatrix, M model, long trainingTime, long testingTime, ThresholdAnalyzer thresholdAnalyzer, RocCurves rocCurves) {
			this.confusionMatrix = confusionMatrix;
			this.model = model;
			this.trainingTime = trainingTime;
			this.testingTime = testingTime;
			this.thresholdAnalyzer = thresholdAnalyzer;
			this.rocCurves = rocCurves;
		}
		public ConfusionMatrix getConfusionMatrix() {
			return confusionMatrix;
		}
		public M getModel() {
			return model;
		}
		/**
		 * @return Time taken for training in milliseconds.
		 */
		public long getTrainingTime() {
			return trainingTime;
		}
		/**
		 * @return Time taken for testing in milliseconds.
		 */
		public long getTestingTime() {
			return testingTime;
		}
		public ThresholdAnalyzer getThresholdAnalyzer() {
			return thresholdAnalyzer;
		}
		public RocCurves getRocCurves() {
			return rocCurves;
		}
	}
	private final Learner<M> learner;
	private final Classifier<M> classifier;

	public <LC extends Learner<M> & Classifier<M>> ClassifierCombination(LC learnerClassifier) {
		this.learner = learnerClassifier;
		this.classifier = learnerClassifier;
	}

	public ClassifierCombination(Learner<M> learner, Classifier<M> classifier) {
		this.learner = learner;
		this.classifier = classifier;
	}

	@Deprecated
	public ConfusionMatrix evaluate(Iterable<Instance> trainingInstances, Iterable<Instance> testingInstances) {
		return runEvaluation(trainingInstances, testingInstances).getConfusionMatrix();
	}
	
	public EvaluationResult<M> runEvaluation(Iterable<Instance> trainingInstances, Iterable<Instance> testingInstances) {
		long start = System.currentTimeMillis();
		M model = learner.train(trainingInstances);
		long trainingTime = System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		@SuppressWarnings("unchecked")
		ConfusionMatrix confusionMatrix = ClassifierEvaluation.evaluate(classifier, testingInstances, model);
		long testingTime = System.currentTimeMillis() - start;

		ThresholdAnalyzer thresholdAnalyzer = null;
		RocCurves rocCurves = null;
		if (model.getCategories().size() == 2) {
			thresholdAnalyzer = ClassifierEvaluation.thresholdAnalysis(classifier, model, testingInstances, "true");
			RocCurves.RocCurvesEvaluator evaluator = new RocCurves.RocCurvesEvaluator("true");
			rocCurves = evaluator.evaluate(classifier, model, testingInstances);
		}

		return new EvaluationResult<>(confusionMatrix, model, trainingTime, testingTime, thresholdAnalyzer, rocCurves);
	}

	public Learner<M> getLearner() {
		return learner;
	}

	public Classifier<M> getClassifier() {
		return classifier;
	}

	@Override
	public String toString() {
		return classifier.toString();
	}

}
