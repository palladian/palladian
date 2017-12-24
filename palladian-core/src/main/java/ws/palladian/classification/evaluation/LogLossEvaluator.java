package ws.palladian.classification.evaluation;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;

public class LogLossEvaluator extends AbstractClassificationEvaluator<Double> {

	private static final double EPSILON = 10e-15;

	private static final String TRUE_CLASS = "1";

	@Override
	public <M extends Model> Double evaluate(Classifier<M> classifier, M model, Dataset data) {

		int n = 0;
		double sum = 0;

		for (Instance instance : data) {
			CategoryEntries result = classifier.classify(instance.getVector(), model);
			boolean actual = instance.getCategory().equals(TRUE_CLASS);
			double predicted = result.getProbability(TRUE_CLASS);
			sum += logLoss(actual, predicted);
			n++;
		}

		return sum / n;
	}

	static double logLoss(boolean actual, double predicted) {
		double y = actual ? 1 : 0;
		double p_binned = Math.max(Math.min(predicted, 1 - EPSILON), EPSILON);
		return -y * Math.log(p_binned);
	}

}
