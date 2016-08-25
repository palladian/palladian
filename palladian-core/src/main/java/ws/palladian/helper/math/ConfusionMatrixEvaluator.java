package ws.palladian.helper.math;

import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.core.Classifier;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;

public class ConfusionMatrixEvaluator extends AbstractClassificationEvaluator<ConfusionMatrix> {

	@Override
	public <M extends Model> ConfusionMatrix evaluate(Classifier<M> classifier, M model, Dataset data) {
		return ClassifierEvaluation.evaluate(classifier, data, model);
	}
	
}
