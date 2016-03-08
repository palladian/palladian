package ws.palladian.helper.math;

import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;

public interface ClassificationEvaluator<R> {
	<M extends Model> R evaluate(Learner<M> learner, Classifier<M> classifier, Iterable<? extends Instance> trainData, Iterable<? extends Instance> testData);
	<M extends Model> R evaluate(Classifier<M> classifier, M model, Iterable<? extends Instance> data);
}
