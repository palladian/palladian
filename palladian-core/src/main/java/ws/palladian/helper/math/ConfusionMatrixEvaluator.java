package ws.palladian.helper.math;

import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Model;

public class ConfusionMatrixEvaluator implements ClassificationEvaluator<ConfusionMatrix> {

	@Override
	public <M extends Model> ConfusionMatrix evaluate(Classifier<M> classifier, M model,
			Iterable<? extends Instance> data) {
		return ClassifierEvaluation.evaluate(classifier, data, model);
	}
	
}
