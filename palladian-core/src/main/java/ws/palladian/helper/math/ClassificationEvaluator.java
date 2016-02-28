package ws.palladian.helper.math;

import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Model;

public interface ClassificationEvaluator<R> {
	<M extends Model> R evaluate(Classifier<M> classifier, M model, Iterable<? extends Instance> data);
}
