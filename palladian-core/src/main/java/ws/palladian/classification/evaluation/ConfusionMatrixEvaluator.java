package ws.palladian.classification.evaluation;

import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.core.Classifier;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.math.ConfusionMatrix;

public class ConfusionMatrixEvaluator extends AbstractClassificationEvaluator<ConfusionMatrix> {

	@Override
	public <M extends Model> ConfusionMatrix evaluate(Classifier<M> classifier, M model, Dataset data) {
		return ClassifierEvaluation.evaluate(classifier, data, model);
	}
	
}
