package ws.palladian.classification.evaluation;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.math.ThresholdAnalyzer;

public class ThresholdAnalysisEvaluator extends AbstractClassificationEvaluator<ThresholdAnalyzer> {
	private final String correctClass;

	public ThresholdAnalysisEvaluator(String correctClass) {
		this.correctClass = correctClass;
	}

	@Override
	public <M extends Model> ThresholdAnalyzer evaluate(Classifier<M> classifier, M model, Dataset data) {
		Validate.isTrue(model.getCategories().size() == 2, "binary model required");
		ThresholdAnalyzer thresholdAnalyzer = new ThresholdAnalyzer();
		for (Instance testInstance : data) {
			CategoryEntries result = classifier.classify(testInstance.getVector(), model);
			boolean relevant = testInstance.getCategory().equals(correctClass);
			double confidence = result.getProbability(correctClass);
			thresholdAnalyzer.add(relevant, confidence);
		}
		return thresholdAnalyzer;
	}

}
