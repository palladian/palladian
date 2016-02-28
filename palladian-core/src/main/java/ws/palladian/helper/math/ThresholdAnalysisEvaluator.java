package ws.palladian.helper.math;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Model;

public class ThresholdAnalysisEvaluator implements ClassificationEvaluator<ThresholdAnalyzer> {
	private final String correctClass;

	public ThresholdAnalysisEvaluator(String correctClass) {
		this.correctClass = correctClass;
	}

	@Override
	public <M extends Model> ThresholdAnalyzer evaluate(Classifier<M> classifier, M model,
			Iterable<? extends Instance> data) {
		ThresholdAnalyzer thresholdAnalyzer = new ThresholdAnalyzer();
		for (Instance testInstance : data) {
			CategoryEntries result = classifier.classify(testInstance.getVector(), model);
			boolean relevant = testInstance.getCategory().equals(correctClass);
			double confidence = result.getProbability(correctClass);
			thresholdAnalyzer.add(relevant , confidence);
		}
		return thresholdAnalyzer;
	}

}
