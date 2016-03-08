package ws.palladian.helper.math;

import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;

public abstract class AbstractClassificationEvaluator<R> implements ClassificationEvaluator<R> {
	@Override
	public <M extends Model> R evaluate(Learner<M> learner, Classifier<M> classifier,
			Iterable<? extends Instance> trainData, Iterable<? extends Instance> testData) {
		M model = learner.train(trainData);
		return evaluate(classifier, model, testData);
	}
}
